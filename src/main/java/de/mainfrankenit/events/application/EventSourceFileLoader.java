package de.mainfrankenit.events.application;
import de.mainfrankenit.events.domain.EventSource;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
@ApplicationScoped
public class EventSourceFileLoader {
    private static final Logger LOG = Logger.getLogger(EventSourceFileLoader.class);

    @jakarta.inject.Inject EventImportScheduler importer;

    private String lastFingerprint;

    @ConfigProperty(name = "event.sources.file", defaultValue = "event-sources.csv")
    String sourceFile;

    @ConfigProperty(name = "event.import.run-at-start", defaultValue = "true")
    boolean importAtStart;

    @ConfigProperty(name = "event.import.enabled", defaultValue = "true")
    boolean importEnabled;

    void load(@Observes StartupEvent ignored) {
        var lines = readLines();
        lastFingerprint = fingerprint(lines.orElse(List.of()));
        lines.ifPresent(this::syncSourcesCommitted);
        if (importEnabled && importAtStart) {
            importer.importNow("Startup");
        }
    }

    @Scheduled(every = "{event.sources.reload-interval}", delayed = "5s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void reloadWhenFileChanges() {
        if (LaunchMode.current() == LaunchMode.TEST) return;
        var lines = readLines();
        var fingerprint = fingerprint(lines.orElse(List.of()));
        if (Objects.equals(lastFingerprint, fingerprint)) return;
        lastFingerprint = fingerprint;
        if (lines.isEmpty()) return;
        syncSourcesCommitted(lines.get());
        if (importEnabled) importer.importNow("Source file change", true);
    }

    private Optional<List<String>> readLines() {
        var path = Path.of(sourceFile);
        try {
            if (Files.exists(path)) return Optional.of(Files.readAllLines(path, StandardCharsets.UTF_8));
            try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(sourceFile)) {
                if (stream == null) return Optional.empty();
                try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    return Optional.of(reader.lines().toList());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read event source file: " + sourceFile, e);
        }
    }

    private SourceLine parse(String line) {
        var parts = line.split(";", -1);
        if (parts.length < 3) throw new IllegalArgumentException("Invalid event source line: " + line);
        return new SourceLine(parts[0].trim(), parts[1].trim(), parts[2].trim(),
                parts.length < 4 || Boolean.parseBoolean(parts[3].trim()));
    }

    private void syncSources(List<String> lines) {
        var parsed = lines.stream()
                .map(String::trim)
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(this::parse)
                .toList();
        var names = new LinkedHashSet<String>();
        for (SourceLine line : parsed) {
            names.add(line.name());
            upsert(line);
        }
        for (EventSource source : EventSource.<EventSource>listAll()) {
            if (!names.contains(source.name) && source.active) source.active = false;
        }
        LOG.infof("Loaded %d event sources from %s", parsed.size(), sourceFile);
    }

    private void syncSourcesCommitted(List<String> lines) {
        QuarkusTransaction.requiringNew().run(() -> syncSources(lines));
    }

    private void upsert(SourceLine line) {
        EventSource source = EventSource.find("name", line.name()).firstResult();
        if (source == null) source = new EventSource();
        source.name = line.name();
        source.url = line.url();
        source.parserKey = line.parserKey();
        source.active = line.active();
        source.persist();
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

    private record SourceLine(String name, String url, String parserKey, boolean active) {}
}