package de.mainfrankenit.events.application;
import de.mainfrankenit.events.domain.AttendanceMode;
import de.mainfrankenit.events.domain.EventType;
import java.time.OffsetDateTime;
import java.util.Set;
public record EventDraft(String title, String organizer, String description, Set<String> tags,
                         EventType eventType, OffsetDateTime startAt, OffsetDateTime endAt,
                         String locationName, String city, String address, AttendanceMode attendanceMode,
                         String sourceUrl, String sourceName, String externalEventId) {}