package de.mainfrankenit.community.adapter.in.rest;
import de.mainfrankenit.community.domain.UserConnection;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.*;
@Path("/api/users/{id}/connections") @Produces(MediaType.APPLICATION_JSON)
public class UserConnectionResource {
    @GET public List<UUID> list(@PathParam("id")UUID id){return UserConnection.<UserConnection>find("userA.id=?1 or userB.id=?1",id).list().stream().map(c->c.userA.id.equals(id)?c.userB.id:c.userA.id).toList();}
}