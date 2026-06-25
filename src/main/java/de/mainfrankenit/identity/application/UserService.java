package de.mainfrankenit.identity.application;
import de.mainfrankenit.events.domain.AttendanceMode;
import de.mainfrankenit.events.domain.EventType;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.identity.domain.UserInterest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.util.*;
@ApplicationScoped
public class UserService {
    @Transactional public AppUser create(String displayName){var u=new AppUser();u.displayName=displayName;u.persist();return u;}
    public AppUser get(UUID id){AppUser u=AppUser.findById(id);if(u==null)throw new NotFoundException("User not found");return u;}
    @Transactional public AppUser preferences(UUID id,String city,Set<EventType> types,AttendanceMode mode){var u=get(id);u.preferredCity=city;u.preferredEventTypes.clear();if(types!=null)u.preferredEventTypes.addAll(types);u.preferredAttendanceMode=mode;return u;}
    @Transactional public List<String> interests(UUID id,Set<String> tags){var u=get(id);if(tags!=null)for(String raw:tags){String tag=raw.trim().toLowerCase(Locale.ROOT);if(!tag.isBlank()&&UserInterest.count("user=?1 and tag=?2",u,tag)==0){var i=new UserInterest();i.user=u;i.tag=tag;i.persist();}}return UserInterest.<UserInterest>find("user",u).list().stream().map(i->i.tag).toList();}
    @Transactional public List<String> replaceInterests(AppUser user,Set<String> tags){UserInterest.delete("user",user);return interests(user.id,tags);}
    @Transactional public AppUser optIn(UUID id,String phone,boolean explicitConsent){if(!explicitConsent)throw new IllegalArgumentException("Explicit consent is required");var u=get(id);AppUser existing=AppUser.find("phoneNumber",phone).firstResult();if(existing!=null&&!existing.id.equals(u.id)){mergePreferences(u,existing);existing.whatsappOptIn=true;return existing;}u.phoneNumber=phone;u.whatsappOptIn=true;return u;}
    private void mergePreferences(AppUser source,AppUser target){if(source.preferredCity!=null&&!source.preferredCity.isBlank())target.preferredCity=source.preferredCity;if(source.preferredAttendanceMode!=null)target.preferredAttendanceMode=source.preferredAttendanceMode;if(!source.preferredEventTypes.isEmpty()){target.preferredEventTypes.clear();target.preferredEventTypes.addAll(source.preferredEventTypes);}for(UserInterest interest:UserInterest.<UserInterest>find("user",source).list())if(UserInterest.count("user=?1 and tag=?2",target,interest.tag)==0){var copy=new UserInterest();copy.user=target;copy.tag=interest.tag;copy.persist();}}
}