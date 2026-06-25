package de.mainfrankenit.events.application;
import de.mainfrankenit.events.domain.AttendanceMode;
import de.mainfrankenit.events.domain.Event;
import de.mainfrankenit.events.domain.EventStatus;
import de.mainfrankenit.events.domain.EventType;
import java.time.*;
import java.util.*;
public record EventView(UUID id, String title, String organizer, String description, Set<String> tags,
                        EventType eventType, OffsetDateTime startAt, OffsetDateTime endAt, String locationName,
                        String city, String address, AttendanceMode attendanceMode, String sourceUrl,
                        String sourceName, EventStatus status, Instant lastCheckedAt) {
    public static EventView from(Event e) { return new EventView(e.id,e.title,e.organizer,e.description,e.tags,e.eventType,e.startAt,e.endAt,e.locationName,e.city,e.address,e.attendanceMode,e.sourceUrl,e.sourceName,e.status,e.lastCheckedAt); }
}