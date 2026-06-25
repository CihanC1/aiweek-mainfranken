package de.mainfrankenit.events.adapter.out.importer;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mainfrankenit.events.domain.EventSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class JsonLdEventExtractorTest {
    @Test void extractsSchemaOrgEvents() {
        var extractor = new JsonLdEventExtractor(); extractor.mapper = new ObjectMapper();
        var source = new EventSource(); source.name="Schema Demo"; source.url="https://example.org/events";
        String html = """
          <html><head><script type="application/ld+json">
          {"@context":"https://schema.org","@type":"Event","name":"Cloud Night","startDate":"2030-01-01T18:00:00+01:00",
           "location":{"@type":"Place","name":"Hub","address":{"addressLocality":"Wurzburg","streetAddress":"Main 1"}},
           "organizer":{"@type":"Organization","name":"Cloud Group"},"keywords":["cloud","java"],"url":"https://example.org/cloud-night"}
          </script></head></html>
          """;
        var events = extractor.parse(new FetchedPage(source.url, html, null, null), source).events();
        assertEquals(1, events.size());
        assertEquals("Cloud Night", events.getFirst().title());
        assertEquals("Wurzburg", events.getFirst().city());
        assertTrue(events.getFirst().tags().contains("schema-org"));
    }
}
