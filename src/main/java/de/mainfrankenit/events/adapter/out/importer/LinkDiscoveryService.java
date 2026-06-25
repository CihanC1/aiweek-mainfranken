package de.mainfrankenit.events.adapter.out.importer;
import de.mainfrankenit.events.domain.SourcePageType;
import jakarta.enterprise.context.ApplicationScoped;
import org.jsoup.Jsoup;
import java.net.URI;
import java.util.*;
@ApplicationScoped
public class LinkDiscoveryService {
    private static final List<String> KEYWORDS = List.of("event","events","veranstaltung","veranstaltungen","termin","termine","calendar","kalender","programm","meetup","workshop","conference","konferenz");
    public record DiscoveredLink(String url, SourcePageType type) {}
    public List<DiscoveredLink> discover(FetchedPage page, int limit) {
        var result = new LinkedHashMap<String, SourcePageType>();
        URI base = URI.create(page.url());
        for (var link : Jsoup.parse(page.body(), page.url()).select("a[href]")) {
            String href = link.absUrl("href");
            if (href.isBlank() || !sameHost(base, href)) continue;
            String haystack = (href + " " + link.text()).toLowerCase(Locale.ROOT);
            if (KEYWORDS.stream().noneMatch(haystack::contains)) continue;
            result.putIfAbsent(stripFragment(href), classify(haystack));
            if (result.size() >= limit) break;
        }
        return result.entrySet().stream().map(e -> new DiscoveredLink(e.getKey(), e.getValue())).toList();
    }
    private boolean sameHost(URI base, String href) {
        try { return Objects.equals(base.getHost(), URI.create(href).getHost()); } catch (Exception e) { return false; }
    }
    private String stripFragment(String url) {
        try { var u=URI.create(url); return new URI(u.getScheme(),u.getAuthority(),u.getPath(),u.getQuery(),null).toString(); } catch (Exception e) { return url; }
    }
    private SourcePageType classify(String value) {
        if (value.contains("veranstaltung/") || value.contains("/event/") || value.contains("/events/")) return SourcePageType.EVENT_DETAIL;
        if (value.contains("programm") || value.contains("calendar") || value.contains("kalender")) return SourcePageType.EVENT_LIST;
        return SourcePageType.UNKNOWN;
    }
}
