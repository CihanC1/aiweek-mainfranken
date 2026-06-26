package de.mainfrankenit.community.adapter.in.rest;
import de.mainfrankenit.community.domain.EventGroup;
import de.mainfrankenit.community.domain.EventGroupMember;
import de.mainfrankenit.community.domain.EventGroupMessage;
import de.mainfrankenit.events.domain.Event;
import de.mainfrankenit.identity.application.AuthService;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.identity.domain.UserInterest;
import de.mainfrankenit.shared.domain.ChatRole;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.*;
@Path("/api/events/{eventId}/group") @Produces(MediaType.APPLICATION_JSON)
public class EventGroupResource {
    @Inject AuthService auth;
    public record GroupMemberView(UUID id,String displayName,String preferredCity,List<String> interests,Instant joinedAt){}
    public record GroupView(UUID id,UUID eventId,String name,List<UUID> memberIds,List<GroupMemberView> members,List<MessageView> messages){}
    public record MessageView(UUID id,UUID userId,String author,ChatRole role,String content,Instant createdAt){}
    public record NewMessage(@NotBlank String message){}
    @GET public GroupView get(@PathParam("eventId")UUID eventId){return view(group(eventId,true));}
    @POST @Path("/join") @Transactional public GroupView joinEndpoint(@PathParam("eventId")UUID eventId,@CookieParam("mf_access")String access){return join(eventId,auth.current(access).id);}
    @POST @Path("/leave") @Transactional public GroupView leave(@PathParam("eventId")UUID eventId,@CookieParam("mf_access")String access){var user=auth.current(access);var g=group(eventId,false);EventGroupMember.delete("group=?1 and user=?2",g,user);return view(g);}
    @POST @Path("/messages") @Transactional public GroupView message(@PathParam("eventId")UUID eventId,@CookieParam("mf_access")String access,@Valid NewMessage body){var user=auth.current(access);var g=group(eventId,false);if(EventGroupMember.count("group=?1 and user=?2",g,user)==0)throw new ForbiddenException("Join the event group before sending messages.");var m=new EventGroupMessage();m.group=g;m.user=user;m.role=ChatRole.USER;m.content=body.message.trim();m.persist();return view(g);}
    public static GroupView join(UUID eventId,UUID userId){var g=group(eventId,true);ensureSeed(g);AppUser u=AppUser.findById(userId);if(u==null)throw new NotFoundException("User not found");if(EventGroupMember.count("group=?1 and user=?2",g,u)==0){var m=new EventGroupMember();m.group=g;m.user=u;m.persist();}return view(g);}
    static EventGroup group(UUID eventId,boolean create){Event e=Event.findById(eventId);if(e==null)throw new NotFoundException("Event not found");EventGroup g=EventGroup.find("event",e).firstResult();if(g==null&&create){g=new EventGroup();g.event=e;g.name=e.title+" Community";g.persist();seed(g,e);}if(g==null)throw new NotFoundException("Event group not found");return g;}
    static GroupView view(EventGroup g){var members=EventGroupMember.<EventGroupMember>find("group=?1 order by createdAt",g).list().stream().map(EventGroupResource::memberView).toList();var ids=members.stream().map(GroupMemberView::id).toList();var messages=EventGroupMessage.<EventGroupMessage>find("group=?1 order by createdAt",g).list().stream().sorted(Comparator.comparing((EventGroupMessage m)->m.role!=ChatRole.SYSTEM).thenComparing(m->m.createdAt)).map(EventGroupResource::messageView).toList();return new GroupView(g.id,g.event.id,g.name,ids,members,messages);}
    private static GroupMemberView memberView(EventGroupMember m){var interests=UserInterest.<UserInterest>find("user",m.user).list().stream().map(i->i.tag).sorted().toList();return new GroupMemberView(m.user.id,displayName(m.user),m.user.preferredCity,interests,m.createdAt);}
    private static MessageView messageView(EventGroupMessage m){return new MessageView(m.id,m.user==null?null:m.user.id,m.user==null?"MainFrankenIT":m.user.displayName,m.role,m.content,m.createdAt);}
    private static String displayName(AppUser user){return user.displayName==null||user.displayName.isBlank()?"Community-Mitglied":user.displayName;}
    private static void ensureSeed(EventGroup g){if(EventGroupMessage.count("group=?1 and role=?2",g,ChatRole.SYSTEM)==0)seed(g,g.event);}
    private static void seed(EventGroup g,Event e){var m=new EventGroupMessage();m.group=g;m.role=ChatRole.SYSTEM;m.content="Event-Gruppe gestartet:\n"+e.title+"\n"+e.startAt+"\n"+e.city+"\n"+(e.locationName==null?"":e.locationName)+"\n"+e.sourceUrl;m.persist();}
}
