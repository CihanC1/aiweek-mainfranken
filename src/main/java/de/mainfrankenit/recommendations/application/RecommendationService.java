package de.mainfrankenit.recommendations.application;
import de.mainfrankenit.events.application.EventView;
import de.mainfrankenit.events.domain.AttendanceMode;
import de.mainfrankenit.events.domain.Event;
import de.mainfrankenit.events.domain.EventStatus;
import de.mainfrankenit.events.domain.EventType;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.identity.domain.UserInterest;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.*;
import java.util.*;
@ApplicationScoped
public class RecommendationService {
    public record Preferences(Set<String> tags, String city, Set<EventType> eventTypes, AttendanceMode attendanceMode) {}
    public record Recommendation(EventView event, int score, String group, List<String> reasons) {}
    public List<Recommendation> forUser(UUID userId) {
        AppUser user = AppUser.findById(userId);
        if (user == null) throw new jakarta.ws.rs.NotFoundException("User not found");
        return preview(preferences(user));
    }
    public Preferences preferences(AppUser user) {
        Set<String> tags = UserInterest.<UserInterest>find("user",user).list().stream().map(i->i.tag).collect(java.util.stream.Collectors.toSet());
        return new Preferences(tags,user.preferredCity,user.preferredEventTypes,user.preferredAttendanceMode);
    }
    public List<Recommendation> preview(Preferences p) {
        var now=OffsetDateTime.now();
        return Event.<Event>find("status in (?1,?2) and startAt >= ?3 order by startAt",EventStatus.ACTIVE,EventStatus.UPDATED,now).list().stream()
                .map(e->score(e,p)).filter(r->r.score()>0).sorted(Comparator.comparingInt(Recommendation::score).reversed().thenComparing(r->r.event.startAt())).toList();
    }
    public Recommendation score(Event e, Preferences p) {
        int score=0; var reasons=new ArrayList<String>();
        var desired=p.tags()==null?Set.<String>of():p.tags().stream().map(s->s.toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
        var haystack=searchText(e);
        long matches=desired.stream().filter(tag -> e.tags.stream().map(s->s.toLowerCase(Locale.ROOT)).anyMatch(tag::equals) || haystack.contains(tag)).count();
        if(matches>0){score+=(int)Math.min(50,matches*20); reasons.add("matching topics");}
        if(p.city()!=null&&!p.city().isBlank()&&p.city().equalsIgnoreCase(e.city)){score+=20;reasons.add("preferred city");}
        if(p.attendanceMode()!=null&&(p.attendanceMode()==e.attendanceMode||e.attendanceMode==AttendanceMode.HYBRID)){score+=15;reasons.add("attendance mode");}
        if(p.eventTypes()!=null&&p.eventTypes().contains(e.eventType)){score+=15;reasons.add("event type");}
        long days=Duration.between(Instant.now(),e.startAt.toInstant()).toDays(); if(days>=0&&days<=14){score+=10;reasons.add("coming soon");}
        String group=score>=60?"BEST":score>=30?"MEDIUM":"RELATED";
        return new Recommendation(EventView.from(e),score,group,reasons);
    }
    private String searchText(Event e) {
        return String.join(" ", List.of(
                Objects.toString(e.title,""),
                Objects.toString(e.organizer,""),
                Objects.toString(e.description,""),
                Objects.toString(e.city,""),
                String.join(" ",e.tags))).toLowerCase(Locale.ROOT);
    }
}