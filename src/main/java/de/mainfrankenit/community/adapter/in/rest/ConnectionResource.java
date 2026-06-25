package de.mainfrankenit.community.adapter.in.rest;
import de.mainfrankenit.community.domain.ConnectionRequest;
import de.mainfrankenit.community.domain.ConnectionStatus;
import de.mainfrankenit.community.domain.UserConnection;
import de.mainfrankenit.identity.domain.AppUser;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.*;
@Path("/api/connections") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class ConnectionResource {
    public record Request(UUID requesterId,UUID recipientId){}
    public record ConnectionView(UUID id,UUID requesterId,UUID recipientId,ConnectionStatus status){}
    @POST @Path("/request") @Transactional public ConnectionView request(Request r){if(r.requesterId.equals(r.recipientId))throw new IllegalArgumentException("Cannot connect to yourself");AppUser a=AppUser.findById(r.requesterId),b=AppUser.findById(r.recipientId);if(a==null||b==null)throw new NotFoundException("User not found");var c=new ConnectionRequest();c.requester=a;c.recipient=b;c.persist();return view(c);}
    @POST @Path("/{id}/accept") @Transactional public ConnectionView accept(@PathParam("id")UUID id){var c=find(id);c.status=ConnectionStatus.ACCEPTED;var u=new UserConnection();u.userA=c.requester;u.userB=c.recipient;u.persist();return view(c);}
    @POST @Path("/{id}/reject") @Transactional public ConnectionView reject(@PathParam("id")UUID id){var c=find(id);c.status=ConnectionStatus.REJECTED;return view(c);}
    private ConnectionRequest find(UUID id){ConnectionRequest c=ConnectionRequest.findById(id);if(c==null)throw new NotFoundException();return c;}
    private ConnectionView view(ConnectionRequest c){return new ConnectionView(c.id,c.requester.id,c.recipient.id,c.status);}
}