package de.mainfrankenit.community.adapter.in.rest;
import de.mainfrankenit.community.domain.DirectMessage;
import de.mainfrankenit.community.domain.MessageRequest;
import de.mainfrankenit.community.domain.MessageRequestStatus;
import de.mainfrankenit.identity.application.AuthService;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.identity.domain.UserInterest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.*;
@Path("/api/community") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class CommunityResource {
    @Inject AuthService auth;
    public record PublicProfile(UUID id,String displayName,String preferredCity,List<String> interests,String profileLink1,String profileLink2,String profileLink3,int matchScore){}
    public record NewMessage(@NotBlank String message){}
    public record DirectMessageView(UUID id,UUID senderId,UUID recipientId,String senderName,String content,Instant createdAt){}
    public record MessageRequestView(UUID id,PublicProfile requester,PublicProfile recipient,MessageRequestStatus status,Instant createdAt){}
    public record ThreadView(String status,String direction,UUID requestId,List<DirectMessageView> messages){}
    public record ConversationView(PublicProfile user,DirectMessageView lastMessage,Instant lastMessageAt,int unreadCount){}
    public record MessageInbox(List<MessageRequestView> incoming,List<MessageRequestView> outgoing){}

    @GET @Path("/users")
    public List<PublicProfile> search(@CookieParam("mf_access")String access,@QueryParam("q")String query){
        var current=auth.current(access);
        var q=normalize(query);
        return AppUser.<AppUser>listAll().stream()
                .filter(u->!u.id.equals(current.id))
                .map(u->profile(u,score(u,q)))
                .filter(p->q.isBlank()||p.matchScore()>0)
                .sorted(Comparator.comparingInt(PublicProfile::matchScore).reversed().thenComparing(PublicProfile::displayName,Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(30).toList();
    }

    @GET @Path("/users/{id}")
    public PublicProfile profile(@CookieParam("mf_access")String access,@PathParam("id")UUID id){
        auth.current(access);
        AppUser user=AppUser.findById(id);
        if(user==null)throw new NotFoundException("User not found");
        return profile(user,0);
    }

    @GET @Path("/users/{id}/messages") @Transactional
    public ThreadView messages(@CookieParam("mf_access")String access,@PathParam("id")UUID otherId){
        var current=auth.current(access);
        AppUser other=AppUser.findById(otherId);
        if(other==null)throw new NotFoundException("User not found");
        var request=requestBetween(current,other);
        if(request!=null&&request.status==MessageRequestStatus.ACCEPTED)DirectMessage.update("read=true where recipient=?1 and sender=?2 and read=false",current,other);
        var messages=request!=null&&request.status==MessageRequestStatus.ACCEPTED
                ? DirectMessage.<DirectMessage>find("(sender=?1 and recipient=?2) or (sender=?2 and recipient=?1) order by createdAt",current,other).list().stream().map(CommunityResource::messageView).toList()
                : List.<DirectMessageView>of();
        return new ThreadView(request==null?"NONE":request.status.name(),direction(current,request),request==null?null:request.id,messages);
    }

    @GET @Path("/conversations")
    public List<ConversationView> conversations(@CookieParam("mf_access")String access){
        var current=auth.current(access);
        return MessageRequest.<MessageRequest>find("(requester=?1 or recipient=?1) and status=?2",current,MessageRequestStatus.ACCEPTED).list().stream()
                .map(r->conversation(current,r))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ConversationView::lastMessageAt,Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    @GET @Path("/message-requests")
    public MessageInbox requests(@CookieParam("mf_access")String access){
        var current=auth.current(access);
        var incoming=MessageRequest.<MessageRequest>find("recipient=?1 and status=?2 order by createdAt desc",current,MessageRequestStatus.PENDING).list().stream().map(this::requestView).toList();
        var outgoing=MessageRequest.<MessageRequest>find("requester=?1 and status=?2 order by createdAt desc",current,MessageRequestStatus.PENDING).list().stream().map(this::requestView).toList();
        return new MessageInbox(incoming,outgoing);
    }

    @POST @Path("/users/{id}/message-request") @Transactional
    public ThreadView request(@CookieParam("mf_access")String access,@PathParam("id")UUID recipientId){
        var requester=auth.current(access);
        if(requester.id.equals(recipientId))throw new BadRequestException("You cannot message yourself.");
        AppUser recipient=AppUser.findById(recipientId);
        if(recipient==null)throw new NotFoundException("User not found");
        var existing=requestBetween(requester,recipient);
        if(existing!=null&&existing.status!=MessageRequestStatus.REJECTED)return messages(access,recipientId);
        if(existing==null){
            existing=new MessageRequest();
            existing.requester=requester;
            existing.recipient=recipient;
        } else {
            existing.requester=requester;
            existing.recipient=recipient;
        }
        existing.status=MessageRequestStatus.PENDING;
        existing.persist();
        return messages(access,recipientId);
    }

    @POST @Path("/message-requests/{id}/accept") @Transactional
    public MessageRequestView accept(@CookieParam("mf_access")String access,@PathParam("id")UUID id){
        var current=auth.current(access);
        MessageRequest request=MessageRequest.findById(id);
        if(request==null)throw new NotFoundException("Message request not found");
        if(!request.recipient.id.equals(current.id))throw new ForbiddenException("Only the recipient can accept this request.");
        request.status=MessageRequestStatus.ACCEPTED;
        return requestView(request);
    }

    @POST @Path("/message-requests/{id}/reject") @Transactional
    public MessageRequestView reject(@CookieParam("mf_access")String access,@PathParam("id")UUID id){
        var current=auth.current(access);
        MessageRequest request=MessageRequest.findById(id);
        if(request==null)throw new NotFoundException("Message request not found");
        if(!request.recipient.id.equals(current.id))throw new ForbiddenException("Only the recipient can reject this request.");
        request.status=MessageRequestStatus.REJECTED;
        return requestView(request);
    }

    @POST @Path("/users/{id}/messages") @Transactional
    public DirectMessageView send(@CookieParam("mf_access")String access,@PathParam("id")UUID recipientId,@Valid NewMessage body){
        var sender=auth.current(access);
        if(sender.id.equals(recipientId))throw new BadRequestException("You cannot message yourself.");
        AppUser recipient=AppUser.findById(recipientId);
        if(recipient==null)throw new NotFoundException("User not found");
        var request=requestBetween(sender,recipient);
        if(request==null||request.status!=MessageRequestStatus.ACCEPTED)throw new ForbiddenException("The message request must be accepted before sending messages.");
        var message=new DirectMessage();
        message.sender=sender; message.recipient=recipient; message.content=body.message.trim(); message.persist();
        return messageView(message);
    }

    private PublicProfile profile(AppUser user,int score){return new PublicProfile(user.id,user.displayName,user.preferredCity,interests(user),user.profileLink1,user.profileLink2,user.profileLink3,score);}
    private int score(AppUser user,String q){
        if(q.isBlank())return 1;
        int score=0;
        var name=normalize(user.displayName);
        if(!name.isBlank()&&name.contains(q))score+=30;
        for(String tag:interests(user)){
            var normalized=normalize(tag);
            if(normalized.equals(q))score+=70;
            else if(normalized.contains(q)||q.contains(normalized))score+=45;
        }
        return score;
    }
    private List<String> interests(AppUser user){return UserInterest.<UserInterest>find("user",user).list().stream().map(i->i.tag).sorted().toList();}
    private static DirectMessageView messageView(DirectMessage message){return new DirectMessageView(message.id,message.sender.id,message.recipient.id,message.sender.displayName,message.content,message.createdAt);}
    private MessageRequest requestBetween(AppUser a,AppUser b){return MessageRequest.find("(requester=?1 and recipient=?2) or (requester=?2 and recipient=?1)",a,b).firstResult();}
    private String direction(AppUser current,MessageRequest request){if(request==null)return "NONE";return request.requester.id.equals(current.id)?"OUTGOING":"INCOMING";}
    private MessageRequestView requestView(MessageRequest request){return new MessageRequestView(request.id,profile(request.requester,0),profile(request.recipient,0),request.status,request.createdAt);}
    private ConversationView conversation(AppUser current,MessageRequest request){
        var other=request.requester.id.equals(current.id)?request.recipient:request.requester;
        DirectMessage last=DirectMessage.find("(sender=?1 and recipient=?2) or (sender=?2 and recipient=?1) order by createdAt desc",current,other).firstResult();
        int unread=(int)DirectMessage.count("recipient=?1 and sender=?2 and read=false",current,other);
        return new ConversationView(profile(other,0),last==null?null:messageView(last),last==null?request.updatedAt:last.createdAt,unread);
    }
    private String normalize(String value){return value==null?"":value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");}
}
