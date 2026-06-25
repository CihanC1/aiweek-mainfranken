package de.mainfrankenit.events.adapter.out.importer;

import de.mainfrankenit.events.application.EventDraft;
import de.mainfrankenit.events.domain.EventSource;
import jakarta.enterprise.context.ApplicationScoped;
import org.jsoup.Jsoup;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

@ApplicationScoped
public class HackationEventParser implements EventParser {
        private static final Pattern HEADER_EVENT_FACTS = Pattern.compile(
                        "((?:\\d{1,2})\\.\\s*(?:[\\-–]\\s*\\d{1,2}\\.)?\\s*[A-Za-zÄÖÜäöü]+\\s*\\d{4})(?:,\\s*\\d{1,2}:\\d{2}\\s*(?:[\\-–]\\s*\\d{1,2}:\\d{2}\\s*Uhr)?)?\\s*[·•]\\s*([^|]+)",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    @Override public String key() { return "hackation"; }

    @Override
    public ParseResult parse(FetchedPage page, EventSource source) {
        var document = Jsoup.parse(page.body(), page.url());
        String bodyText = EventPageParserSupport.normalizeWhitespace(document.text());
        String title = EventPageParserSupport.firstNonBlank(
                EventPageParserSupport.text(document.selectFirst("h1")),
                EventPageParserSupport.text(document.selectFirst("h2")),
                EventPageParserSupport.meta(document, "meta[property=og:title]", "content"));
        String description = EventPageParserSupport.firstNonBlank(
                EventPageParserSupport.meta(document, "meta[name=description]", "content"),
                EventPageParserSupport.labeledValue(bodyText,
                        new String[]{"Was ist der AI Vibe Hackathon", "What is the AI Vibe Hackathon"},
                        new String[]{"Fragen", "Questions", "Hackation", "Schedule"})) ;
        String dateText = EventPageParserSupport.labeledValue(bodyText,
                new String[]{"Datum", "Date"},
                new String[]{"Ort", "Location", "Venue", "Teilnahme", "Participation", "Anmeldung", "Registration"});
        String placeText = EventPageParserSupport.labeledValue(bodyText,
                new String[]{"Ort", "Location", "Venue"},
                new String[]{"Teilnahme", "Participation", "Anmeldung", "Registration", "Community", "Discord"});
        if (dateText == null) {
                        var headerFacts = document.select("p, li, div").stream()
                                        .map(element -> EventPageParserSupport.normalizeWhitespace(element.text()))
                                        .filter(text -> text != null && !text.isBlank())
                                        .map(HEADER_EVENT_FACTS::matcher)
                                        .filter(java.util.regex.Matcher::find)
                                        .findFirst();
                        if (headerFacts.isPresent()) {
                                dateText = headerFacts.get().group(1);
                                if (placeText == null) placeText = EventPageParserSupport.normalizeWhitespace(headerFacts.get().group(2));
            }
        }
                if (dateText == null) dateText = bodyText;
                if (placeText == null) {
                        var bodyHeaderFacts = HEADER_EVENT_FACTS.matcher(bodyText);
                        if (bodyHeaderFacts.find()) placeText = EventPageParserSupport.normalizeWhitespace(bodyHeaderFacts.group(2));
                }
        var dateRange = EventPageParserSupport.parseDateRange(dateText);
        if (title == null || dateRange == null) return ParseResult.empty();
        var place = EventPageParserSupport.parsePlace(placeText);
        var tags = new LinkedHashSet<>(EventPageParserSupport.inferTags(title, description));
        tags.add("hackation");
        if (bodyText.toLowerCase(java.util.Locale.GERMAN).contains("ai week")) tags.add("ai-week");
        EventDraft event = new EventDraft(
                title,
                source.name,
                description,
                tags,
                EventPageParserSupport.eventType(title, description),
                dateRange.start(),
                dateRange.end(),
                place.locationName(),
                place.city() == null ? "Mainfranken" : place.city(),
                place.address(),
                EventPageParserSupport.attendanceMode(bodyText),
                page.url(),
                source.name,
                EventPageParserSupport.slug(page.url()));
        return new ParseResult(List.of(event), List.of(), 95, List.of());
    }
}