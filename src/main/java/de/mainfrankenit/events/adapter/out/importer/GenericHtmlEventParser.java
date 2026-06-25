package de.mainfrankenit.events.adapter.out.importer;
import de.mainfrankenit.events.application.EventDraft;
import de.mainfrankenit.events.domain.AttendanceMode;
import de.mainfrankenit.events.domain.EventSource;
import de.mainfrankenit.events.domain.EventType;
import jakarta.enterprise.context.ApplicationScoped;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.time.OffsetDateTime;
import java.util.*;
/** Parser for source pages exposing event cards with data-* fields; add site-specific parsers beside this adapter. */
@ApplicationScoped
public class GenericHtmlEventParser implements EventParser {
    public String key() { return "generic"; }
    public ParseResult parse(FetchedPage page, EventSource source) {
        var result = new ArrayList<EventDraft>();
        Document document = Jsoup.parse(page.body(), page.url());
        for (var el : document.select("[data-event]")) {
            var title = value(el.attr("data-title"), el.selectFirst(".event-title"));
            var description = value(el.attr("data-description"), el.selectFirst(".event-description"));
            var tags = new LinkedHashSet<>(Arrays.asList(el.attr("data-tags").split("\\s*,\\s*")));
            tags.remove("");
            var link = el.selectFirst("a[href]");
            result.add(new EventDraft(title, el.attr("data-organizer"), description, tags,
                    enumValue(EventType.class, el.attr("data-type"), EventType.OTHER),
                    OffsetDateTime.parse(el.attr("data-start")), parseTime(el.attr("data-end")),
                    el.attr("data-location"), el.attr("data-city"), el.attr("data-address"),
                    enumValue(AttendanceMode.class, el.attr("data-mode"), AttendanceMode.OFFLINE),
                    link == null ? source.url : link.absUrl("href"), source.name, el.attr("data-id")));
        }
                if (result.isEmpty()) {
                    var fallback = parseCommonEventPage(document, page.url(), source);
                    if (fallback != null) result.add(fallback);
                }
        return ParseResult.events(result);
    }

                private EventDraft parseCommonEventPage(Document document, String pageUrl, EventSource source) {
                String title = EventPageParserSupport.firstNonBlank(
                    EventPageParserSupport.text(document.selectFirst("h1")),
                    EventPageParserSupport.text(document.selectFirst("h2")),
                    EventPageParserSupport.meta(document, "meta[property=og:title]", "content"),
                    document.title());
                String description = EventPageParserSupport.firstNonBlank(
                    EventPageParserSupport.meta(document, "meta[name=description]", "content"),
                    EventPageParserSupport.text(document.selectFirst("main p")),
                    EventPageParserSupport.text(document.selectFirst("article p")));
                String bodyText = EventPageParserSupport.normalizeWhitespace(document.text());
                String dateText = EventPageParserSupport.labeledValue(bodyText,
                    new String[]{"Datum", "Date"},
                    new String[]{"Ort", "Location", "Venue", "Teilnahme", "Participation", "Anmeldung", "Registration"});
                var dateRange = EventPageParserSupport.parseDateRange(dateText);
                if (title == null || dateRange == null) return null;
                String placeText = EventPageParserSupport.labeledValue(bodyText,
                    new String[]{"Ort", "Location", "Venue"},
                    new String[]{"Teilnahme", "Participation", "Anmeldung", "Registration", "Fragen", "Questions"});
                var place = EventPageParserSupport.parsePlace(placeText);
                String pageContext = EventPageParserSupport.normalizeWhitespace(title + " " + Objects.toString(description, "") + " " + bodyText);
                return new EventDraft(
                    title,
                    source.name,
                    description,
                    EventPageParserSupport.inferTags(title, description),
                    EventPageParserSupport.eventType(title, description),
                    dateRange.start(),
                    dateRange.end(),
                    place.locationName(),
                    place.city() == null ? "Mainfranken" : place.city(),
                    place.address(),
                    EventPageParserSupport.attendanceMode(pageContext),
                    pageUrl,
                    source.name,
                    EventPageParserSupport.slug(pageUrl));
                }
    private String value(String attr, org.jsoup.nodes.Element fallback) { return attr.isBlank() && fallback != null ? fallback.text() : attr; }
    private OffsetDateTime parseTime(String value) { return value == null || value.isBlank() ? null : OffsetDateTime.parse(value); }
    private <T extends Enum<T>> T enumValue(Class<T> type, String value, T fallback) {
        try { return Enum.valueOf(type, value.toUpperCase(Locale.ROOT)); } catch (Exception ignored) { return fallback; }
    }
}
