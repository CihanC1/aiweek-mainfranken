package de.mainfrankenit.recommendations.application;
import de.mainfrankenit.events.application.EventView;
import de.mainfrankenit.events.application.TaxonomyService;
import de.mainfrankenit.events.domain.AttendanceMode;
import de.mainfrankenit.events.domain.Event;
import de.mainfrankenit.events.domain.EventStatus;
import de.mainfrankenit.events.domain.EventType;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.identity.domain.UserInterest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.*;
import java.util.*;
@ApplicationScoped
public class RecommendationService {
    @Inject TaxonomyService taxonomy;
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
        var scored = Event.<Event>find("status in (?1,?2) and startAt >= ?3 order by startAt",EventStatus.ACTIVE,EventStatus.UPDATED,now).list().stream()
                .map(e->score(e,p)).sorted(Comparator.comparingInt(Recommendation::score).reversed().thenComparing(r->r.event.startAt())).toList();
        var hasTopicPreference = p.tags()!=null && p.tags().stream().anyMatch(t -> t != null && !t.isBlank());
        var matching = scored.stream().filter(r->hasTopicPreference ? r.reasons().stream().anyMatch(reason -> reason.startsWith("passende Themen:")) : r.score()>0).toList();
        if(!matching.isEmpty())return matching;
        if(hasTopicPreference)return List.of();
        return scored.stream().limit(6).toList();
    }
    public Recommendation score(Event e, Preferences p) {
        int score=0; var reasons=new ArrayList<String>();
        var desired=desiredTerms(p);
        var haystack=searchText(e);
        var eventTerms=new LinkedHashSet<String>();
        eventTerms.addAll((e.tags==null?Set.<String>of():e.tags).stream().map(this::norm).toList());
        eventTerms.addAll((e.categories==null?Set.<String>of():e.categories).stream().map(this::norm).toList());
        int topicScore=0; var matchedTopics=new LinkedHashSet<String>();
        for(var tag:desired){
            int value=topicMatchScore(tag,eventTerms,haystack);
            if(value>0){topicScore+=value; matchedTopics.add(tag);}
        }
        if(topicScore>0){score+=Math.min(60,topicScore); reasons.add("passende Themen: "+String.join(", ",matchedTopics));}
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
                String.join(" ",e.tags),
                String.join(" ",e.categories))).toLowerCase(Locale.ROOT);
    }
    private int topicMatchScore(String desired, Set<String> eventTerms, String haystack) {
        if(eventTerms.contains(desired)) return 20;
        for(var term:eventTerms) if(prefixMatch(desired,term)) return 14;
        if(containsTerm(haystack,desired)) return 12;
        return 0;
    }
    private Set<String> desiredTerms(Preferences p) {
        var selected=p.tags()==null?Set.<String>of():p.tags();
        var result=new LinkedHashSet<String>();
        for(var raw:selected){
            if(raw==null)continue;
            var term=norm(raw);
            if(term.isBlank())continue;
            result.add(term);
            if(taxonomy==null)continue;
            for(var category:taxonomy.categories()){
                var categoryName=norm(category.name());
                if(categoryName.equals(term)){
                    result.addAll(category.tags().stream().map(this::norm).filter(s->!s.isBlank()).toList());
                }
                for(var tag:category.tags()){
                    if(norm(tag).equals(term))result.add(categoryName);
                }
            }
        }
        return result;
    }
    private boolean prefixMatch(String desired, String term) {
        return desired.length()>=2&&term.startsWith(desired) || term.length()>=3&&desired.startsWith(term);
    }
    private boolean containsTerm(String haystack, String term) {
        return haystack.matches(".*(?<![\\p{L}\\p{N}])"+java.util.regex.Pattern.quote(term)+"(?![\\p{L}\\p{N}]).*");
    }
    private String norm(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");
    }
}
