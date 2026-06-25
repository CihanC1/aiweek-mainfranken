package de.mainfrankenit.events.application;
import de.mainfrankenit.events.domain.EventType;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.*;
@ApplicationScoped
public class EventNormalizer {
    private static final int SHORT_TEXT_LIMIT = 255;
    private static final int URL_LIMIT = 2048;

    public EventDraft normalize(EventDraft d) {
        if (d == null || blank(d.title()) || d.startAt() == null || blank(d.sourceUrl()))
            throw new IllegalArgumentException("title, startAt and sourceUrl are required");
        var tags = d.tags() == null ? Set.<String>of() : d.tags().stream().filter(Objects::nonNull)
                .map(this::text).filter(s -> !s.isBlank()).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new EventDraft(clean(d.title(), SHORT_TEXT_LIMIT), clean(d.organizer(), SHORT_TEXT_LIMIT), clean(d.description()), tags,
                Objects.requireNonNullElse(d.eventType(), de.mainfrankenit.events.domain.EventType.OTHER), d.startAt(), d.endAt(),
                clean(d.locationName(), SHORT_TEXT_LIMIT), clean(d.city(), SHORT_TEXT_LIMIT), clean(d.address(), SHORT_TEXT_LIMIT),
                Objects.requireNonNull(d.attendanceMode(), "attendanceMode"), clean(d.sourceUrl(), URL_LIMIT),
                clean(d.sourceName(), SHORT_TEXT_LIMIT), clean(d.externalEventId(), SHORT_TEXT_LIMIT));
    }

    public String fingerprint(EventDraft d) {
        var key = String.join("|", text(d.sourceName()), text(d.externalEventId()), text(d.title()),
                d.startAt().toInstant().toString(), text(d.city()), text(d.locationName()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public String text(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFKC)
                .replaceAll("\\s+", " ");
    }
    private String clean(String value) { return value == null ? null : value.trim().replaceAll("\\s+", " "); }
    private String clean(String value, int maxLength) {
        String cleaned = clean(value);
        if (cleaned == null || cleaned.length() <= maxLength) return cleaned;
        return cleaned.substring(0, maxLength);
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}