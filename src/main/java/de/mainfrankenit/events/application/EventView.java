package de.mainfrankenit.events.application;
import de.mainfrankenit.events.domain.AttendanceMode;
import de.mainfrankenit.events.domain.Event;
import de.mainfrankenit.events.domain.EventStatus;
import de.mainfrankenit.events.domain.EventType;
import java.time.*;
import java.util.*;
public record EventView(UUID id, String title, String organizer, String description, Set<String> tags, Set<String> categories,
                        EventType eventType, OffsetDateTime startAt, OffsetDateTime endAt, String locationName,
                        String city, String address, AttendanceMode attendanceMode, String sourceUrl,
                        String sourceName, EventStatus status, Instant lastCheckedAt) {
    public static EventView from(Event e) {
        var categories = clean(e.categories);
        var tags = clean(e.tags);
        tags.removeAll(categories);
        return new EventView(e.id,e.title,e.organizer,e.description,tags,categories,e.eventType,e.startAt,e.endAt,e.locationName,e.city,e.address,e.attendanceMode,e.sourceUrl,e.sourceName,e.status,e.lastCheckedAt);
    }
    private static LinkedHashSet<String> clean(Set<String> values) {
        var result = new LinkedHashSet<String>();
        if (values == null) return result;
        for (var value : values) if (value != null && !value.isBlank()) result.add(value.trim());
        return result;
    }
}
