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
        return Event.<Event>find("status <> ?1 and startAt >= ?2 order by startAt", EventStatus.ARCHIVED, OffsetDateTime.now())
                .list().stream().filter(e -> matches(e,q)).map(EventView::from).toList();
    }
    public List<UUID> usersWithSameQuery(String query, UUID exclude) {
        var normalized=normalize(query);
        return UserSearch.<UserSearch>find("normalizedQuery = ?1 and user is not null order by createdAt desc",normalized).list().stream()
                .map(s->s.user.id).filter(id->!id.equals(exclude)).distinct().toList();
    }
    public String normalize(String query) { return query == null ? "" : query.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," "); }
    private boolean matches(Event e, String q) {
        var haystack = String.join(" ", List.of(Objects.toString(e.title,""),Objects.toString(e.organizer,""),Objects.toString(e.description,""),Objects.toString(e.city,""),Objects.toString(e.eventType,""))).toLowerCase(Locale.ROOT);
        if (haystack.contains(q)) return true;
        var terms = new LinkedHashSet<String>();
        if (e.tags != null) terms.addAll(e.tags.stream().map(this::normalize).toList());
        if (e.categories != null) terms.addAll(e.categories.stream().map(this::normalize).toList());
        for (var term : terms) if (term.contains(q) || term.startsWith(q) || q.startsWith(term)) return true;
        return false;
    }
}
