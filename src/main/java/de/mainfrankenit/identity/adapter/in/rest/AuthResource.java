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
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.*;
@Path("/api/auth") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Inject AuthService auth;
    @Inject UserService users;
    @Inject RecommendationService recommendations;
    public record Register(@NotBlank @Size(min=2,max=80) String displayName,@NotBlank @Email String email,@NotBlank @Size(min=8,max=128) String password){}
    public record Login(@NotBlank @Email String email,@NotBlank String password){}
    public record Forgot(@NotBlank @Email String email){}
    public record Reset(@NotBlank String token,@NotBlank @Size(min=8,max=128) String password){}
    public record UpdateProfile(@Size(min=2,max=80) String displayName,String preferredCity,Set<EventType> preferredEventTypes,AttendanceMode attendanceMode,Set<String> interests,String profileLink1,String profileLink2,String profileLink3){}
    public record UserView(UUID id,String displayName,String email,String phoneNumber,boolean whatsappOptIn,String preferredCity,Set<EventType> preferredEventTypes,AttendanceMode attendanceMode,UserRole role,List<String> interests,String profileLink1,String profileLink2,String profileLink3){
        static UserView of(AppUser u){return new UserView(u.id,u.displayName,u.email,u.phoneNumber,u.whatsappOptIn,u.preferredCity,u.preferredEventTypes,u.preferredAttendanceMode,u.role,UserInterest.<UserInterest>find("user",u).list().stream().map(i->i.tag).toList(),u.profileLink1,u.profileLink2,u.profileLink3);}
    }
    @POST @Path("/register") public Response register(@Valid Register r){return session(auth.register(r.displayName,r.email,r.password));}
    @POST @Path("/login") public Response login(@Valid Login r){return session(auth.login(r.email,r.password));}
    @POST @Path("/refresh") public Response refresh(@CookieParam("mf_refresh")String refresh){return session(auth.refresh(refresh));}
    @POST @Path("/logout") public Response logout(@CookieParam("mf_access")String access,@CookieParam("mf_refresh")String refresh){auth.logout(access,refresh);return Response.noContent().header("Set-Cookie",clear("mf_access","/")).header("Set-Cookie",clear("mf_refresh","/api/auth")).build();}
    @GET @Path("/me") public UserView me(@CookieParam("mf_access")String access){return UserView.of(auth.current(access));}
    @GET @Path("/me/recommendations") public List<RecommendationService.Recommendation> meRecommendations(@CookieParam("mf_access")String access){return recommendations.forUser(auth.current(access).id);}
    @PATCH @Path("/me") @Transactional public UserView update(@CookieParam("mf_access")String access,@Valid UpdateProfile r){var u=auth.current(access);if(r.displayName!=null)u.displayName=r.displayName.trim();if(r.preferredCity!=null){var city=r.preferredCity.trim();u.preferredCity=city.isBlank()?null:city;}if(r.preferredEventTypes!=null){u.preferredEventTypes.clear();u.preferredEventTypes.addAll(r.preferredEventTypes);}u.preferredAttendanceMode=r.attendanceMode;if(r.interests!=null)users.replaceInterests(u,r.interests);u.profileLink1=link(r.profileLink1);u.profileLink2=link(r.profileLink2);u.profileLink3=link(r.profileLink3);return UserView.of(u);}
    @POST @Path("/forgot-password") public Map<String,String> forgot(@Valid Forgot r){String token=auth.forgot(r.email);var result=new LinkedHashMap<String,String>();result.put("message","Wenn ein Konto existiert, wurde ein Link erstellt.");if(token!=null)result.put("resetToken",token);return result;}
    @POST @Path("/reset-password") public Map<String,String> reset(@Valid Reset r){auth.reset(r.token,r.password);return Map.of("message","Passwort wurde aktualisiert.");}
    private Response session(AuthService.Tokens t){return Response.ok(UserView.of(t.user())).header("Set-Cookie",cookie("mf_access",t.access(),"/",900)).header("Set-Cookie",cookie("mf_refresh",t.refresh(),"/api/auth",2592000)).build();}
    private String cookie(String name,String value,String path,int age){return name+"="+value+"; Path="+path+"; Max-Age="+age+"; HttpOnly; SameSite=Lax";}
    private String clear(String name,String path){return name+"=; Path="+path+"; Max-Age=0; HttpOnly; SameSite=Lax";}
    private String link(String value){if(value==null)return null;var v=value.trim();return v.isBlank()?null:v;}
}
