package de.mainfrankenit.events.adapter.in.rest;
import de.mainfrankenit.events.domain.EventSource;
import de.mainfrankenit.events.domain.FetchStrategy;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.*;
@Path("/api/event-sources") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class EventSourceResource {
    public record SourceRequest(@NotBlank String name,@NotBlank @Pattern(regexp="https?://.+") String url,Boolean active,@NotBlank String parserKey,FetchStrategy fetchStrategy,Integer checkIntervalMinutes){}
    @GET public List<EventSource> list(){return EventSource.listAll();}
    @POST @Transactional public Response create(@Valid SourceRequest r,@Context UriInfo uri){var s=new EventSource();s.name=r.name;s.url=r.url;s.active=r.active==null||r.active;s.parserKey=r.parserKey;if(r.fetchStrategy!=null)s.fetchStrategy=r.fetchStrategy;if(r.checkIntervalMinutes!=null)s.checkIntervalMinutes=Math.max(5,r.checkIntervalMinutes);s.persist();return Response.created(uri.getAbsolutePathBuilder().path(s.id.toString()).build()).entity(s).build();}
    @PATCH @Path("/{id}") @Transactional public EventSource patch(@PathParam("id")UUID id,SourceRequest r){EventSource s=EventSource.findById(id);if(s==null)throw new NotFoundException();if(r.name!=null)s.name=r.name;if(r.url!=null)s.url=r.url;if(r.active!=null)s.active=r.active;if(r.parserKey!=null)s.parserKey=r.parserKey;if(r.fetchStrategy!=null)s.fetchStrategy=r.fetchStrategy;if(r.checkIntervalMinutes!=null)s.checkIntervalMinutes=Math.max(5,r.checkIntervalMinutes);return s;}
}
