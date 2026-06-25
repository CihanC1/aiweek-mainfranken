package de.mainfrankenit.events.adapter.out.importer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mainfrankenit.events.application.EventDraft;
import de.mainfrankenit.events.domain.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jsoup.Jsoup;
import java.time.OffsetDateTime;
import java.util.*;
@ApplicationScoped
public class JsonLdEventExtractor {
    @Inject ObjectMapper mapper;
    public ParseResult parse(FetchedPage page, EventSource source) {
        var events = new ArrayList<EventDraft>();
        var warnings = new ArrayList<String>();
        for (var script : Jsoup.parse(page.body(), page.url()).select("script[type=application/ld+json]")) {
            try { collect(mapper.readTree(script.data()), page, source, events); }
            catch (Exception e) { warnings.add("Invalid JSON-LD: " + e.getMessage()); }
        }
        return new ParseResult(events, List.of(), events.isEmpty()?0:95, warnings);
    }
    private void collect(JsonNode node, FetchedPage page, EventSource source, List<EventDraft> events) {
        if (node == null || node.isMissingNode() || node.isNull()) return;
        if (node.isArray()) { node.forEach(n -> collect(n,page,source,events)); return; }
        if (node.has("@graph")) collect(node.path("@graph"), page, source, events);
        if (!isEvent(node)) return;
        String title = text(node, "name");
        String start = text(node, "startDate");
        if (title == null || start == null) return;
        JsonNode location = node.path("location");
        String locationName = text(location, "name");
        JsonNode address = location.path("address");
        String city = firstText(address, "addressLocality", "addressRegion");
        if (city == null) city = "Mainfranken";
        String street = firstText(address, "streetAddress", "postalCode");
        String description = text(node, "description");
        String url = Optional.ofNullable(text(node, "url")).orElse(page.url());
        String mode = text(node, "eventAttendanceMode");
        events.add(new EventDraft(title, organizer(node), description, tags(node),
                EventType.OTHER, OffsetDateTime.parse(start), optionalTime(text(node, "endDate")),
                locationName, city, street, attendance(mode), url, source.name, text(node, "identifier")));
    }
    private boolean isEvent(JsonNode node) {
        String type = text(node, "@type");
        if (type != null && type.toLowerCase(Locale.ROOT).contains("event")) return true;
        JsonNode typeNode = node.path("@type");
        if (typeNode.isArray()) for (JsonNode t : typeNode) if (t.asText("").toLowerCase(Locale.ROOT).contains("event")) return true;
        return false;
    }
    private Set<String> tags(JsonNode node) {
        var tags = new LinkedHashSet<String>();
        JsonNode keywords = node.path("keywords");
        if (keywords.isArray()) keywords.forEach(k -> add(tags, k.asText()));
        else for (String token : keywords.asText("").split(",")) add(tags, token);
        add(tags, "schema-org");
        return tags;
    }
    private void add(Set<String> tags, String tag) { if (tag != null && !tag.isBlank()) tags.add(tag.trim().toLowerCase(Locale.ROOT)); }
    private String organizer(JsonNode node) { JsonNode org=node.path("organizer"); return text(org,"name"); }
    private AttendanceMode attendance(String mode) {
        if (mode == null) return AttendanceMode.OFFLINE;
        String value=mode.toLowerCase(Locale.ROOT);
        if (value.contains("online")) return AttendanceMode.ONLINE;
        if (value.contains("mixed")) return AttendanceMode.HYBRID;
        return AttendanceMode.OFFLINE;
    }
    private OffsetDateTime optionalTime(String value) { return value == null || value.isBlank() ? null : OffsetDateTime.parse(value); }
    private String firstText(JsonNode node, String... fields) { for(String f:fields){String v=text(node,f); if(v!=null)return v;} return null; }
    private String text(JsonNode node, String field) { String value=node.path(field).asText(null); return value == null || value.isBlank() ? null : value.trim(); }
}
