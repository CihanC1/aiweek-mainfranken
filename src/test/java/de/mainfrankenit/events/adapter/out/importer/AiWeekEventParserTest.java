package de.mainfrankenit.events.adapter.out.importer;
import de.mainfrankenit.events.domain.EventSource;
import de.mainfrankenit.events.domain.EventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AiWeekEventParserTest {
    @Test void mapsDescriptionsAndSkipsCancelledSessions() {
        var parser=new AiWeekEventParser(); parser.mapper=new ObjectMapper();
        var source=new EventSource();source.name="AI Week";source.url="https://www.ai-week.de/programm.php#/veranstaltung/83";
        String json="""
          {"sessions":[
            {"id":83,"start":"2026-06-24T10:00:00+02:00","end":"2026-06-24T11:00:00+02:00","cancelled":false,"onlineOnly":false,
             "title":"KI-Agenten Workshop","host":{"name":"Test GmbH"},"location":{"name":"Hub","streetNo":"Test 1","zipcode":"97070","city":"Würzburg"},
             "description":{"short":"Kurz erklärt.","long":"Die vollständige Beschreibung."},"channel":{"name":"Tech & Science"}},
            {"id":84,"start":"2026-06-24T12:00:00+02:00","cancelled":true,"onlineOnly":true,"title":"Abgesagt"}
          ]}
          """;
        var events=parser.parse(new FetchedPage(source.url,json,null,null),source).events();
        assertEquals(1,events.size());
        assertEquals(EventType.WORKSHOP,events.getFirst().eventType());
        assertTrue(events.getFirst().description().contains("vollständige Beschreibung"));
        assertEquals("Würzburg",events.getFirst().city());
        assertTrue(events.getFirst().tags().contains("ai"));
    }

    @Test void filtersDetailUrlsToThatSession() {
        var parser=new AiWeekEventParser(); parser.mapper=new ObjectMapper();
        var source=new EventSource();source.name="AI Week";source.url="https://www.ai-week.de/programm.php#/veranstaltung/84";
        String json="""
          {"sessions":[
            {"id":83,"start":"2026-06-24T10:00:00+02:00","title":"Other","host":{"name":"Test"},"description":{}},
            {"id":84,"start":"2026-06-24T12:00:00+02:00","title":"Target","host":{"name":"Test"},"description":{}}
          ]}
          """;
        var events=parser.parse(new FetchedPage(source.url,json,null,null),source).events();
        assertEquals(1,events.size());
        assertEquals("Target",events.getFirst().title());
        assertEquals("84",events.getFirst().externalEventId());
    }
}
