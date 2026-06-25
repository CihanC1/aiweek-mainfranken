package de.mainfrankenit.events.adapter.out.importer;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
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
            if (response.statusCode() / 100 != 2) throw new IllegalStateException("Source returned HTTP " + response.statusCode() + " for " + url);
            return new FetchedPage(response.uri().toString(), response.body(), response.headers().firstValue("ETag").orElse(null), response.headers().firstValue("Last-Modified").orElse(null));
        } catch (HttpTimeoutException e) {
            throw new IllegalStateException("Could not fetch event source: request timed out for " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Could not fetch event source: request interrupted for " + url, e);
        } catch (IOException e) {
            throw new IllegalStateException("Could not fetch event source: " + rootMessage(e) + " for " + url, e);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Could not fetch event source: invalid URL " + url, e);
        } catch (Exception e) {
            throw new IllegalStateException("Could not fetch event source: " + rootMessage(e) + " for " + url, e);
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) current = current.getCause();
        String message = current.getMessage();
        if (message == null || message.isBlank()) return current.getClass().getSimpleName();
        return current.getClass().getSimpleName() + ": " + message;
    }
}
