package de.mainfrankenit.events.application;

import de.mainfrankenit.events.domain.Event;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ApplicationScoped
public class TaxonomyService {
    @ConfigProperty(name="taxonomy.file", defaultValue="tags-catagories.csv")
    String taxonomyFile;

    public record TaxonomyEntry(String category, String tag, Pattern pattern) {}
    public record CategoryView(String name, List<String> tags) {}
    public record Classification(Set<String> tags, Set<String> categories) {}

    private volatile List<TaxonomyEntry> cached;

    public Classification classify(EventDraft event) {
        var haystack = searchable(event.title(), event.description());
        var tags = event.tags() == null ? new LinkedHashSet<String>() : event.tags().stream()
                .filter(Objects::nonNull).map(this::key).filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        var categories = new LinkedHashSet<String>();
        var checkedCategories = new HashSet<String>();
        for (var entry : entries()) {
            if (checkedCategories.add(entry.category()) && Pattern.compile(boundaryPattern(entry.category()), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(haystack).find()) {
                categories.add(key(entry.category()));
            }
            if (entry.pattern().matcher(haystack).find()) {
                tags.add(key(entry.tag()));
                categories.add(key(entry.category()));
            }
        }
        return new Classification(tags, categories);
    }

    public Set<String> extractTerms(String text) {
        var haystack = searchable(text);
        var result = new LinkedHashSet<String>();
        if (haystack.isBlank()) return result;
        var tokens = Arrays.stream(haystack.split("[^\\p{L}\\p{N}+#.]+"))
                .filter(s -> s.length() >= 2)
                .filter(s -> !PREFIX_STOPWORDS.contains(s))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (var entry : entries()) {
            var category = key(entry.category());
            var tag = key(entry.tag());
            if (entry.pattern().matcher(haystack).find() || matchesPrefix(tokens, tag)) result.add(tag);
            if (Pattern.compile(boundaryPattern(entry.category()), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(haystack).find() || matchesPrefix(tokens, category)) result.add(category);
        }
        return result;
    }

    private boolean matchesPrefix(Set<String> tokens, String term) {
        for (var token : tokens) {
            if (token.length() >= 3 && term.startsWith(token)) return true;
            if (term.length() >= 3 && token.startsWith(term)) return true;
        }
        return false;
    }

    public List<CategoryView> categories() {
        var grouped = new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
        for (var entry : entries()) grouped.computeIfAbsent(entry.category(), ignored -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)).add(entry.tag());
        return grouped.entrySet().stream().map(e -> new CategoryView(e.getKey(), List.copyOf(e.getValue()))).toList();
    }

    @Transactional
    public int reclassifyExistingEvents() {
        int updated = 0;
        for (Event event : Event.<Event>listAll()) {
            var draft = new EventDraft(event.title, event.organizer, event.description, event.tags, event.eventType, event.startAt, event.endAt,
                    event.locationName, event.city, event.address, event.attendanceMode, event.sourceUrl, event.sourceName, event.externalEventId);
            var classification = classify(draft);
            if (!same(event.tags, classification.tags()) || !same(event.categories, classification.categories())) {
                event.tags.clear();
                event.tags.addAll(classification.tags());
                event.categories.clear();
                event.categories.addAll(classification.categories());
                updated++;
            }
        }
        return updated;
    }

    public List<TaxonomyEntry> entries() {
        var value = cached;
        if (value == null) {
            synchronized (this) {
                value = cached;
                if (value == null) cached = value = load();
            }
        }
        return value;
    }

    private List<TaxonomyEntry> load() {
        try (var input = openTaxonomy(); var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            var entries = new ArrayList<TaxonomyEntry>();
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; }
                if (line.isBlank()) continue;
                var parts = line.split("\\t|,", 3);
                if (parts.length < 2) continue;
                var category = parts[0].trim();
                var tag = parts[1].trim();
                if (category.isBlank() || tag.isBlank()) continue;
                entries.add(new TaxonomyEntry(category, tag, Pattern.compile(boundaryPattern(tag), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)));
            }
            return List.copyOf(entries);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load taxonomy file: " + taxonomyFile, e);
        }
    }

    private InputStream openTaxonomy() throws IOException {
        var resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(taxonomyFile);
        if (resource != null) return resource;
        return new FileInputStream(taxonomyFile);
    }

    private String boundaryPattern(String value) {
        return "(?<![\\p{L}\\p{N}])" + Pattern.quote(searchable(value)) + "(?![\\p{L}\\p{N}])";
    }

    private String searchable(String... values) {
        return Arrays.stream(values).filter(Objects::nonNull).map(this::plain).collect(Collectors.joining(" "));
    }

    private String plain(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private String key(String value) {
        return plain(value);
    }

    private boolean same(Set<String> a, Set<String> b) {
        return Objects.equals(new LinkedHashSet<>(a == null ? Set.of() : a), new LinkedHashSet<>(b == null ? Set.of() : b));
    }
    private static final Set<String> PREFIX_STOPWORDS=Set.of("learn","lernen","learning","want","would","like","think","about","something","hmm","möchte","moechte","interessiere","interessiert");
}
