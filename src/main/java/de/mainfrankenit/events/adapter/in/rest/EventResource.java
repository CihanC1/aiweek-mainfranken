package de.mainfrankenit.events.adapter.in.rest;
import de.mainfrankenit.community.adapter.in.rest.EventGroupResource;
import de.mainfrankenit.events.application.EventImportService;
import de.mainfrankenit.events.application.EventView;
import de.mainfrankenit.events.application.SearchService;
import de.mainfrankenit.events.domain.Event;
import de.mainfrankenit.events.domain.EventChange;
import de.mainfrankenit.events.domain.EventStatus;
import de.mainfrankenit.events.domain.EventType;
import de.mainfrankenit.identity.application.AuthService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.time.OffsetDateTime;
import java.util.*;
@Path("/api/events") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class EventResource {
    @Inject EventImportService importer; @Inject SearchService search;
    @Inject AuthService auth;
    @GET public List<EventView> list(@QueryParam("city")String city,@QueryParam("type")EventType type,@QueryParam("includePast")@DefaultValue("false")boolean includePast){
        List<Event> events;
        var now = OffsetDateTime.now();
        var dateClause=includePast?"":" and startAt >= ?2";
        var order=includePast?" order by startAt desc":" order by startAt";
        if(city==null&&type==null) events=includePast?Event.list("status <> ?1"+order,EventStatus.ARCHIVED):Event.list("status <> ?1"+dateClause+order,EventStatus.ARCHIVED,now);
        else if(city!=null&&type==null) events=includePast?Event.list("status <> ?1 and lower(city)=?2"+order,EventStatus.ARCHIVED,city.toLowerCase(Locale.ROOT)):Event.list("status <> ?1"+dateClause+" and lower(city)=?3"+order,EventStatus.ARCHIVED,now,city.toLowerCase(Locale.ROOT));
        else if(city==null) events=includePast?Event.list("status <> ?1 and eventType=?2"+order,EventStatus.ARCHIVED,type):Event.list("status <> ?1"+dateClause+" and eventType=?3"+order,EventStatus.ARCHIVED,now,type);
        else events=includePast?Event.list("status <> ?1 and lower(city)=?2 and eventType=?3"+order,EventStatus.ARCHIVED,city.toLowerCase(Locale.ROOT),type):Event.list("status <> ?1"+dateClause+" and lower(city)=?3 and eventType=?4"+order,EventStatus.ARCHIVED,now,city.toLowerCase(Locale.ROOT),type);
        return events.stream().map(EventView::from).toList();
    }
    @GET @Path("/{id}") public EventView get(@PathParam("id")UUID id){Event e=Event.findById(id);if(e==null)throw new NotFoundException();return EventView.from(e);}
    @GET @Path("/search") public List<EventView> search(@QueryParam("query")@NotBlank String q){return search.events(q,null);}
    @POST @Path("/import/run") public EventImportService.ImportResult run(){return importer.run(true);}
    public record ChangeView(UUID id,String field,String oldValue,String newValue,java.time.Instant detectedAt){}
    @GET @Path("/{id}/changes") public List<ChangeView> changes(@PathParam("id")UUID id){return EventChange.<EventChange>find("event.id=?1 order by detectedAt desc",id).list().stream().map(c->new ChangeView(c.id,c.changedField,c.oldValue,c.newValue,c.detectedAt)).toList();}
    @POST @Path("/{id}/join-group") @Transactional public Response join(@PathParam("id")UUID eventId,@CookieParam("mf_access")String access){return Response.ok(EventGroupResource.join(eventId,auth.current(access).id)).build();}
}
