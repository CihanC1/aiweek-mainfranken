package de.mainfrankenit.events.adapter.out.importer;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
@ApplicationScoped
public class HttpPageFetcher {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
    public FetchedPage fetch(String url) {
        try {
            var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).header("User-Agent", "MainFrankenIT/1.0 (+event-indexer)").GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) throw new IllegalStateException("Source returned HTTP " + response.statusCode());
            return new FetchedPage(response.uri().toString(), response.body(), response.headers().firstValue("ETag").orElse(null), response.headers().firstValue("Last-Modified").orElse(null));
        } catch (Exception e) { throw new IllegalStateException("Could not fetch event source", e); }
    }
}
