package de.mainfrankenit.events.application;
import de.mainfrankenit.events.domain.EventSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
@ApplicationScoped
public class EventSourceFileLoader {
    private static final Logger LOG = Logger.getLogger(EventSourceFileLoader.class);

    @jakarta.inject.Inject EventImportScheduler importer;

    @ConfigProperty(name = "event.sources.file", defaultValue = "event-sources.csv")
    String sourceFile;

    @ConfigProperty(name = "event.import.run-at-start", defaultValue = "true")
    boolean importAtStart;

    @ConfigProperty(name = "event.import.enabled", defaultValue = "true")
    boolean importEnabled;

    @Transactional
    void load(@Observes StartupEvent ignored) {
        readLines().ifPresent(lines -> lines.stream()
                .map(String::trim)
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(this::parse)
                .forEach(this::upsert));
        if (importEnabled && importAtStart) {
            importer.importNow("Startup");
        }
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

    private void upsert(SourceLine line) {
        EventSource source = EventSource.find("name", line.name()).firstResult();
        if (source == null) source = new EventSource();
        source.name = line.name();
        source.url = line.url();
        source.parserKey = line.parserKey();
        source.active = line.active();
        source.persist();
    }

    private record SourceLine(String name, String url, String parserKey, boolean active) {}
}