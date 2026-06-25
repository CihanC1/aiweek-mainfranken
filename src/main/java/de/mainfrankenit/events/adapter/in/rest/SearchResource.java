package de.mainfrankenit.events.adapter.in.rest;
import de.mainfrankenit.events.application.EventView;
import de.mainfrankenit.events.application.SearchService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.*;
@Path("/api/search") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class SearchResource {
    @Inject SearchService service;
    public record History(@NotBlank String query,UUID userId){}
    @GET @Path("/events") public List<EventView> events(@QueryParam("query")@NotBlank String q,@QueryParam("userId")UUID user){return service.events(q,user);}
    @GET @Path("/users-with-same-query") public List<UUID> users(@QueryParam("query")@NotBlank String q,@QueryParam("userId")UUID user){return service.usersWithSameQuery(q,user);}
    @POST @Path("/history") @Transactional public Map<String,Object> history(History h){service.events(h.query,h.userId);return Map.of("recorded",true,"normalizedQuery",service.normalize(h.query));}
}