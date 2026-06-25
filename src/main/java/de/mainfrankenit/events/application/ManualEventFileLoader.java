package de.mainfrankenit.events.application;

import de.mainfrankenit.events.domain.AttendanceMode;
import de.mainfrankenit.events.domain.EventType;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class ManualEventFileLoader {
    private static final Logger LOG = Logger.getLogger(ManualEventFileLoader.class);

    @Inject EventImportService importer;

    @ConfigProperty(name = "event.manual.file", defaultValue = "manual-events.csv")
    String manualFile;

    @ConfigProperty(name = "event.manual.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "event.manual.notify", defaultValue = "false")
    boolean notify;

    private String lastFingerprint;

    void load(@Observes StartupEvent ignored) {
        if (!enabled) return;
        var lines = readLines();
        lastFingerprint = fingerprint(lines.orElse(List.of()));
        lines.ifPresent(this::importCommitted);
    }

    @Scheduled(every = "{event.sources.reload-interval}", delayed = "5s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void reloadWhenFileChanges() {
        if (!enabled || LaunchMode.current() == LaunchMode.TEST) return;
        var lines = readLines();
        var fingerprint = fingerprint(lines.orElse(List.of()));
        if (Objects.equals(lastFingerprint, fingerprint)) return;
        lastFingerprint = fingerprint;
        lines.ifPresent(this::importCommitted);
    }

    private Optional<List<String>> readLines() {
        var path = Path.of(manualFile);
        try {
            if (Files.exists(path)) return Optional.of(Files.readAllLines(path, StandardCharsets.UTF_8));
            try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(manualFile)) {
                if (stream == null) return Optional.empty();
                try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    return Optional.of(reader.lines().toList());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read manual event file: " + manualFile, e);
        }
    }

    private void importCommitted(List<String> lines) {
        QuarkusTransaction.requiringNew().run(() -> importRows(lines));
    }

    private void importRows(List<String> lines) {
        int imported = 0, created = 0, updated = 0, unchanged = 0, skipped = 0;
        for (String rawLine : lines) {
            var line = rawLine.trim();
            if (line.isBlank() || line.startsWith("#")) continue;
            try {
                var row = parse(line);
                if (!row.active()) { skipped++; continue; }
                var outcome = importer.upsert(row.toDraft(), notify);
                imported++;
                if (outcome == 1) created++;
                else if (outcome == 2) updated++;
                else unchanged++;
            } catch (RuntimeException e) {
                skipped++;
                LOG.warnf("Skipping manual event row from %s: %s", manualFile, e.getMessage());
            }
        }
        LOG.infof("Loaded manual events from %s: imported=%d created=%d updated=%d unchanged=%d skipped=%d",
                manualFile, imported, created, updated, unchanged, skipped);
    }

    private ManualEventLine parse(String line) {
        var parts = line.split(";", -1);
        if (parts.length < 14) throw new IllegalArgumentException("Expected at least 14 columns");
        return new ManualEventLine(
                value(parts, 0),
                required(parts, 1, "title"),
                OffsetDateTime.parse(required(parts, 2, "startAt")),
                parseOptionalTime(value(parts, 3)),
                required(parts, 4, "city"),
                value(parts, 5),
                value(parts, 6),
                parseEnum(EventType.class, value(parts, 7), EventType.OTHER),
                parseEnum(AttendanceMode.class, value(parts, 8), AttendanceMode.OFFLINE),
                value(parts, 9),
                value(parts, 10).isBlank() ? "Manual Events" : value(parts, 10),
                required(parts, 11, "sourceUrl"),
                tags(value(parts, 12)),
                value(parts, 13),
                parts.length < 15 || Boolean.parseBoolean(value(parts, 14))
        );
    }

    private OffsetDateTime parseOptionalTime(String value) {
        return value.isBlank() ? null : OffsetDateTime.parse(value);
    }

    private <T extends Enum<T>> T parseEnum(Class<T> type, String value, T fallback) {
        if (value.isBlank()) return fallback;
        return Enum.valueOf(type, value.trim().toUpperCase());
    }

    private Set<String> tags(String raw) {
        var result = new LinkedHashSet<String>();
        Arrays.stream(raw.split(",", -1)).map(String::trim).filter(s -> !s.isBlank()).forEach(result::add);
        return result;
    }

    private String required(String[] parts, int index, String name) {
        var value = value(parts, index);
        if (value.isBlank()) throw new IllegalArgumentException("Missing required column: " + name);
        return value;
    }

    private String value(String[] parts, int index) {
        return index >= parts.length ? "" : parts[index].trim();
    }

    private String fingerprint(List<String> lines) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            for (String line : lines) digest.update(line.trim().getBytes(StandardCharsets.UTF_8));
            var hex = new StringBuilder();
            for (byte value : digest.digest()) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256 support", e);
        }
    }

    private record ManualEventLine(String externalEventId, String title, OffsetDateTime startAt, OffsetDateTime endAt,
                                   String city, String locationName, String address, EventType eventType,
                                   AttendanceMode attendanceMode, String organizer, String sourceName,
                                   String sourceUrl, Set<String> tags, String description, boolean active) {
        EventDraft toDraft() {
            return new EventDraft(title, organizer, description, tags, eventType, startAt, endAt,
                    locationName, city, address, attendanceMode, sourceUrl, sourceName, externalEventId);
        }
    }
}
