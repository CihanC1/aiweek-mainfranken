package de.mainfrankenit.assistant.adapter.in.rest;
import de.mainfrankenit.assistant.application.ChatService;
import de.mainfrankenit.assistant.domain.ChatMessage;
import de.mainfrankenit.assistant.domain.ChatSession;
import de.mainfrankenit.shared.domain.ChatRole;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.*;
@Path("/api/chat/session") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class ChatResource {
    @Inject ChatService chat;
    public record Start(UUID userId){} public record Message(@NotBlank String message){}
    public record MessageView(ChatRole role,String content,Instant createdAt){}
    public record SessionView(UUID id,UUID userId,String state,boolean completed,List<MessageView> messages){}
    @POST public ChatService.Reply start(Start s){return chat.start(s==null?null:s.userId);}
    @POST @Path("/{id}/message") public ChatService.Reply message(@PathParam("id")UUID id,@Valid Message m){return chat.message(id,m.message);}
    @GET @Path("/{id}") public SessionView get(@PathParam("id")UUID id){ChatSession s=ChatSession.findById(id);if(s==null)throw new NotFoundException();var messages=ChatMessage.<ChatMessage>find("session=?1 order by createdAt",s).list().stream().map(m->new MessageView(m.role,m.content,m.createdAt)).toList();return new SessionView(s.id,s.user==null?null:s.user.id,s.state,s.completed,messages);}
}