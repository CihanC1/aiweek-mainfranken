package de.mainfrankenit.notifications.adapter.in.rest;
import de.mainfrankenit.assistant.application.ChatService;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.notifications.adapter.out.whatsapp.WhatsAppPort;
import jakarta.inject.Inject;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.*;
@Path("/api/whatsapp") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class WhatsAppResource {
    @Inject WhatsAppPort whatsapp; @Inject ChatService chat;
    public record SendTest(@NotBlank @Pattern(regexp="^\\+[1-9][0-9]{7,14}$")String phoneNumber,@NotBlank String message){}
    public record Webhook(@NotBlank String from,@NotBlank String message,UUID sessionId){}
    @POST @Path("/send-test") public WhatsAppPort.SendResult send(SendTest r){return whatsapp.send(r.phoneNumber,r.message);}
    @POST @Path("/webhook") public ChatService.Reply webhook(Webhook r){/* Provider signature verification belongs in the production provider adapter/filter. */return r.sessionId==null?chat.start(userByPhone(r.from)):chat.message(r.sessionId,r.message);}
    private UUID userByPhone(String phone){AppUser u=AppUser.find("phoneNumber",phone).firstResult();return u==null?null:u.id;}
}