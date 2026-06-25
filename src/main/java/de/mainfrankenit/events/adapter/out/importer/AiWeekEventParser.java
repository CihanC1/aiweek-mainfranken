package de.mainfrankenit.events.adapter.out.importer;
import de.mainfrankenit.events.application.EventDraft;
import de.mainfrankenit.events.domain.AttendanceMode;
import de.mainfrankenit.events.domain.EventSource;
import de.mainfrankenit.events.domain.EventType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;
/** Parses AI Week's public timetable export. The website itself renders these records client-side. */
@ApplicationScoped
public class AiWeekEventParser implements EventParser {
    private static final String PUBLIC_PAGE = "https://www.ai-week.de/programm.php#/veranstaltung/";
    private static final Pattern DETAIL_ID = Pattern.compile("/veranstaltung/(\\d+)");
    @Inject ObjectMapper mapper;

    @Override public String key() { return "ai-week"; }

    @Override
    public ParseResult parse(FetchedPage page, EventSource source) {
        try {
            var result = new ArrayList<EventDraft>();
            JsonNode sessions = mapper.readTree(page.body()).path("sessions");
            if (!sessions.isArray()) throw new IllegalArgumentException("AI Week export does not contain a sessions array");
            String sourceEventId = sourceEventId(source.url);
            for (JsonNode session : sessions) {
                if (session.path("cancelled").asBoolean(false)) continue;
                String id = session.path("id").asText();
                if (sourceEventId != null && !sourceEventId.equals(id)) continue;
                String title = text(session, "title");
                String shortDescription = session.path("description").path("short").asText("").trim();
                String longDescription = session.path("description").path("long").asText("").trim();
                String description = description(shortDescription, longDescription);
                JsonNode location = session.path("location");
                String city = location.path("city").asText(session.path("onlineOnly").asBoolean() ? "Online" : "Mainfranken");
                String address = join(location.path("streetNo").asText(""), location.path("zipcode").asText(""), city);
                result.add(new EventDraft(
                        title,
                        session.path("host").path("name").asText("AI Week Mainfranken"),
                        description,
                        tags(session),
                        eventType(title, description),
                        OffsetDateTime.parse(text(session, "start")),
                        optionalTime(session.path("end").asText("")),
                        location.path("name").asText(session.path("onlineOnly").asBoolean() ? "Online" : null),
                        city,
                        address.isBlank() ? null : address,
                        session.path("onlineOnly").asBoolean() ? AttendanceMode.ONLINE : AttendanceMode.OFFLINE,
                        PUBLIC_PAGE + id,
                        source.name,
                        id));
            }
            return ParseResult.events(result);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse AI Week export", e);
        }
    }

    private Set<String> tags(JsonNode session) {
        var tags = new LinkedHashSet<String>();
        String channel = session.path("channel").path("name").asText("").trim();
        if (!channel.isBlank()) tags.add(channel.toLowerCase(Locale.GERMAN));
        String haystack = (session.path("title").asText("") + " " + session.path("description").toString()).toLowerCase(Locale.GERMAN);
        Map<String,String> keywords = new LinkedHashMap<>();
        keywords.put("ki", "ai"); keywords.put("künstliche intelligenz", "ai"); keywords.put("ai", "ai");
        keywords.put("software", "software development"); keywords.put("cloud", "cloud"); keywords.put("cyber", "cybersecurity");
        keywords.put("security", "cybersecurity"); keywords.put("datenbank", "database"); keywords.put("sql", "sql");
        keywords.put("startup", "startup"); keywords.put("start-up", "startup"); keywords.put("network", "networking");
        keywords.put("workshop", "workshop"); keywords.put("werkstatt", "workshop");
        keywords.forEach((needle, tag) -> { if (haystack.contains(needle)) tags.add(tag); });
        tags.add("ai-week");
        return tags;
    }

    private EventType eventType(String title, String description) {
        String value = (title + " " + description).toLowerCase(Locale.GERMAN);
        if (value.contains("workshop") || value.contains("werkstatt")) return EventType.WORKSHOP;
        if (value.contains("hackathon")) return EventType.HACKATHON;
        if (value.contains("konferenz") || value.contains("conference")) return EventType.CONFERENCE;
        return EventType.MEETUP;
    }

    private String description(String shortText, String longText) {
        if (longText.isBlank()) return shortText;
        if (shortText.isBlank() || longText.startsWith(shortText)) return longText;
        return shortText + "\n\n" + longText;
    }
    private OffsetDateTime optionalTime(String value) { return value.isBlank() ? null : OffsetDateTime.parse(value); }
    private String text(JsonNode node, String field) { String value=node.path(field).asText("").trim(); if(value.isBlank())throw new IllegalArgumentException(field+" is required");return value; }
    private String join(String... values) { return String.join(" ", Arrays.stream(values).filter(v -> v != null && !v.isBlank()).toList()); }
    private String sourceEventId(String url) {
        if (url == null) return null;
        var matcher = DETAIL_ID.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }
}
