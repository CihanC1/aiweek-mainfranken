package de.mainfrankenit.events.adapter.out.importer;

import de.mainfrankenit.events.domain.AttendanceMode;
import de.mainfrankenit.events.domain.EventType;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class EventPageParserSupport {
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Berlin");
    private static final Pattern DATE_RANGE = Pattern.compile("(\\d{1,2})\\.\\s*(?:[\\-–]\\s*(\\d{1,2})\\.)?\\s*([A-Za-zÄÖÜäöü]+)\\s*(\\d{4})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern POSTCODE_CITY = Pattern.compile("(?:\\b\\d{5}\\s+)?([\\p{L}][\\p{L} .-]+)$", Pattern.UNICODE_CASE);
    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("january", 1), Map.entry("jan", 1), Map.entry("januar", 1),
            Map.entry("february", 2), Map.entry("feb", 2), Map.entry("februar", 2),
            Map.entry("march", 3), Map.entry("mar", 3), Map.entry("maerz", 3), Map.entry("märz", 3),
            Map.entry("april", 4), Map.entry("apr", 4),
            Map.entry("may", 5), Map.entry("mai", 5),
            Map.entry("june", 6), Map.entry("jun", 6), Map.entry("juni", 6),
            Map.entry("july", 7), Map.entry("jul", 7), Map.entry("juli", 7),
            Map.entry("august", 8), Map.entry("aug", 8),
            Map.entry("september", 9), Map.entry("sep", 9),
            Map.entry("october", 10), Map.entry("oct", 10), Map.entry("oktober", 10), Map.entry("okt", 10),
            Map.entry("november", 11), Map.entry("nov", 11),
            Map.entry("december", 12), Map.entry("dec", 12), Map.entry("dezember", 12), Map.entry("dez", 12)
    );

    static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    static String text(Element element) {
        return element == null ? null : firstNonBlank(element.text());
    }

    static String meta(Document document, String cssQuery, String attribute) {
        var element = document.selectFirst(cssQuery);
        return element == null ? null : firstNonBlank(element.attr(attribute));
    }

    static String labeledValue(String text, String[] labels, String[] terminators) {
        String labelPattern = String.join("|", labels);
        String terminatorPattern = String.join("|", terminators);
        Pattern pattern = Pattern.compile("(?is)(?:" + labelPattern + ")\\s*:?\\s*(.+?)(?=(?:" + terminatorPattern + ")\\s*:?|$)");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) return null;
        return normalizeWhitespace(matcher.group(1));
    }

    static DateRange parseDateRange(String value) {
        if (value == null || value.isBlank()) return null;
        Matcher matcher = DATE_RANGE.matcher(normalizeWhitespace(value));
        if (!matcher.find()) return null;
        int startDay = Integer.parseInt(matcher.group(1));
        int endDay = matcher.group(2) == null ? startDay : Integer.parseInt(matcher.group(2));
        Integer month = MONTHS.get(normalizeMonth(matcher.group(3)));
        if (month == null) return null;
        int year = Integer.parseInt(matcher.group(4));
        LocalDate start = LocalDate.of(year, month, startDay);
        LocalDate end = LocalDate.of(year, month, endDay);
        return new DateRange(start.atStartOfDay(DEFAULT_ZONE).toOffsetDateTime(), end.plusDays(1).atStartOfDay(DEFAULT_ZONE).minusMinutes(1).toOffsetDateTime());
    }

    static Place parsePlace(String raw) {
        if (raw == null || raw.isBlank()) return new Place(null, null, null);
        String normalized = normalizeWhitespace(raw);
        String[] parts = normalized.split("\\s*,\\s*");
        String locationName = parts.length > 0 ? parts[0].trim() : null;
        String city = null;
        Matcher matcher = POSTCODE_CITY.matcher(normalized);
        if (matcher.find()) city = firstNonBlank(matcher.group(1));
        if (city == null && parts.length > 1) city = parts[parts.length - 1].trim();
        String address = parts.length <= 1 ? null : String.join(", ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        return new Place(locationName, city, address);
    }

    static EventType eventType(String title, String description) {
        String value = normalizeWhitespace((Objects.toString(title, "") + " " + Objects.toString(description, "")).toLowerCase(Locale.GERMAN));
        if (value.contains("hackathon")) return EventType.HACKATHON;
        if (value.contains("workshop")) return EventType.WORKSHOP;
        if (value.contains("conference") || value.contains("konferenz") || value.contains("summit")) return EventType.CONFERENCE;
        if (value.contains("networking") || value.contains("meetup") || value.contains("barcamp")) return EventType.NETWORKING;
        if (value.contains("webinar")) return EventType.WEBINAR;
        return EventType.OTHER;
    }

    static AttendanceMode attendanceMode(String text) {
        String value = normalizeWhitespace(Objects.toString(text, "").toLowerCase(Locale.GERMAN));
        if (value.contains("hybrid")) return AttendanceMode.HYBRID;
        if (value.contains("online") || value.contains("remote")) return AttendanceMode.ONLINE;
        return AttendanceMode.OFFLINE;
    }

    static Set<String> inferTags(String title, String description) {
        String haystack = normalizeWhitespace((Objects.toString(title, "") + " " + Objects.toString(description, "")).toLowerCase(Locale.GERMAN));
        var tags = new LinkedHashSet<String>();
        if (haystack.contains("ai") || haystack.contains("ki") || haystack.contains("künstliche intelligenz")) tags.add("ai");
        if (haystack.contains("hackathon")) tags.add("hackathon");
        if (haystack.contains("workshop")) tags.add("workshop");
        if (haystack.contains("barcamp")) tags.add("barcamp");
        if (haystack.contains("network") || haystack.contains("community")) tags.add("networking");
        return tags;
    }

    static String slug(String url) {
        try {
            String path = URI.create(url).getPath();
            if (path == null || path.isBlank()) return url;
            String[] parts = path.split("/");
            for (int index = parts.length - 1; index >= 0; index--) {
                if (!parts[index].isBlank()) return parts[index];
            }
            return path;
        } catch (Exception ignored) {
            return url;
        }
    }

    static String normalizeWhitespace(String value) {
        return value == null ? null : value.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }

    private static String normalizeMonth(String month) {
        return normalizeWhitespace(month).toLowerCase(Locale.GERMAN);
    }

    record DateRange(OffsetDateTime start, OffsetDateTime end) {}
    record Place(String locationName, String city, String address) {}

    private EventPageParserSupport() {}
}