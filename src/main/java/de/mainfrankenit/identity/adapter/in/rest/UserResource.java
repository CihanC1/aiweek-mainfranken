package de.mainfrankenit.identity.adapter.in.rest;
import de.mainfrankenit.events.domain.AttendanceMode;
import de.mainfrankenit.events.domain.EventType;
import de.mainfrankenit.identity.application.AuthService;
import de.mainfrankenit.identity.application.UserService;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.identity.domain.UserInterest;
import de.mainfrankenit.identity.domain.UserRole;
import de.mainfrankenit.recommendations.application.RecommendationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.*;
@Path("/api/users") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class UserResource {
    @Inject UserService users;
    @Inject AuthService auth;
    @Inject RecommendationService recommendations;
    public record CreateUser(String displayName){}
    public record Preferences(String preferredCity,Set<EventType> preferredEventTypes,AttendanceMode attendanceMode){}
    public record Interests(@NotEmpty Set<@NotBlank String> tags){}
    public record WhatsAppOptIn(@NotBlank @Pattern(regexp="^\\+[1-9][0-9]{7,14}$",message="must use E.164 format") String phoneNumber,@AssertTrue(message="explicit consent is required") boolean explicitConsent){}
    public record UserView(UUID id,String displayName,String phoneNumber,boolean whatsappOptIn,String preferredCity,Set<EventType> preferredEventTypes,AttendanceMode attendanceMode,List<String> interests){static UserView of(AppUser u){var tags=UserInterest.<UserInterest>find("user",u).list().stream().map(i->i.tag).toList();return new UserView(u.id,u.displayName,u.phoneNumber,u.whatsappOptIn,u.preferredCity,u.preferredEventTypes,u.preferredAttendanceMode,tags);}}
    @POST public Response create(CreateUser r,@Context UriInfo uri){var u=users.create(r==null?null:r.displayName);return Response.created(uri.getAbsolutePathBuilder().path(u.id.toString()).build()).entity(UserView.of(u)).build();}
    @GET @Path("/{id}") public UserView get(@PathParam("id")UUID id){return UserView.of(users.get(id));}
    @GET @Path("/{id}/recommendations") public List<RecommendationService.Recommendation> recommendations(@PathParam("id")UUID id,@CookieParam("mf_access")String access){var current=auth.current(access);if(!current.id.equals(id)&&current.role!=UserRole.ADMIN)throw new ForbiddenException("You can only view your own recommendations.");return recommendations.forUser(id);}
    @PATCH @Path("/{id}/preferences") public UserView preferences(@PathParam("id")UUID id,@Valid Preferences r){return UserView.of(users.preferences(id,r.preferredCity,r.preferredEventTypes,r.attendanceMode));}
    @POST @Path("/{id}/interests") public List<String> interests(@PathParam("id")UUID id,@Valid Interests r){return users.interests(id,r.tags);}
    @POST @Path("/{id}/whatsapp-opt-in") public UserView optIn(@PathParam("id")UUID id,@Valid WhatsAppOptIn r){return UserView.of(users.optIn(id,r.phoneNumber,r.explicitConsent));}
}