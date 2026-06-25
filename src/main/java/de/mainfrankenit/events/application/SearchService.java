package de.mainfrankenit.events.application;
import de.mainfrankenit.community.domain.UserSearch;
import de.mainfrankenit.events.domain.Event;
import de.mainfrankenit.events.domain.EventStatus;
import de.mainfrankenit.identity.domain.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.*;
@ApplicationScoped
public class SearchService {
    @Transactional
    public List<EventView> events(String query, UUID userId) {
        var q = normalize(query);
        if (q.isBlank()) return List.of();
        AppUser user = userId == null ? null : AppUser.findById(userId);
        var history = new UserSearch(); history.user=user; history.query=query.trim(); history.normalizedQuery=q; history.persist();
        String like="%"+q+"%";
        return Event.<Event>find("select distinct e from Event e left join e.tags t where e.status <> ?1 and e.startAt >= ?2 and (lower(e.title) like ?3 or lower(coalesce(e.organizer,'')) like ?3 or lower(coalesce(e.description,'')) like ?3 or lower(e.city) like ?3 or lower(cast(e.eventType as string)) like ?3 or lower(t) like ?3) order by e.startAt", EventStatus.ARCHIVED, OffsetDateTime.now(), like)
                .list().stream().map(EventView::from).toList();
    }
    public List<UUID> usersWithSameQuery(String query, UUID exclude) {
        var normalized=normalize(query);
        return UserSearch.<UserSearch>find("normalizedQuery = ?1 and user is not null order by createdAt desc",normalized).list().stream()
                .map(s->s.user.id).filter(id->!id.equals(exclude)).distinct().toList();
    }
    public String normalize(String query) { return query == null ? "" : query.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," "); }
}