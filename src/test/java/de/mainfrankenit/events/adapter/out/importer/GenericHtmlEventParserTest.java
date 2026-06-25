package de.mainfrankenit.events.adapter.out.importer;
import de.mainfrankenit.events.domain.EventSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GenericHtmlEventParserTest {
 @Test void parsesDeterministically(){var s=new EventSource();s.name="Demo";s.url="https://example.org";var html="<article data-event data-title='AI Workshop' data-start='2030-01-01T18:00:00+01:00' data-city='Bamberg' data-mode='hybrid' data-tags='ai, java'><a href='/42'>x</a></article>";var d=new GenericHtmlEventParser().parse(new FetchedPage("https://example.org",html,null,null),s).events().getFirst();assertEquals("AI Workshop",d.title());assertEquals("https://example.org/42",d.sourceUrl());assertTrue(d.tags().contains("ai"));}

 @Test void parsesCommonLabeledEventPages(){var s=new EventSource();s.name="Hack Demo";s.url="https://example.org/hack";var html="<html><head><meta name='description' content='A two-day AI hackathon in Wuerzburg'></head><body><h1>AI Vibe Hackathon #4</h1><section><h3>Wann &amp; Wo?</h3><p>Datum: 25. – 26. Juni 2026</p><p>Ort: ZDI Ideenlabor, Rottendorfer Str. 71, 97074 Wuerzburg</p><p>Anmeldung: Jetzt anmelden</p></section></body></html>";var d=new GenericHtmlEventParser().parse(new FetchedPage(s.url,html,null,null),s).events().getFirst();assertEquals("AI Vibe Hackathon #4",d.title());assertEquals("Wuerzburg",d.city());assertEquals(de.mainfrankenit.events.domain.EventType.HACKATHON,d.eventType());assertEquals("hack",d.externalEventId());}
}
