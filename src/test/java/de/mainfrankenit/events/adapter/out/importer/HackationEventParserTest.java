package de.mainfrankenit.events.adapter.out.importer;

import de.mainfrankenit.events.domain.EventSource;
import de.mainfrankenit.events.domain.EventType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HackationEventParserTest {
    @Test
    void parsesHackationEventPages() {
        var parser = new HackationEventParser();
        var source = new EventSource();
        source.name = "AI WEEK Hackathons";
        source.url = "https://hackation.de/de/events/hackathon-2026-q2/";
        String html = """
                <html><head><meta name='description' content='AI Vibe Hackathon in Wuerzburg'></head><body>
                <h1>AI Vibe Hackathon #4</h1>
                <h3>Wann &amp; Wo?</h3>
                <p>Datum: 25. - 26. Juni 2026</p>
                <p>Ort: ZDI Ideenlabor, Rottendorfer Str. 71, 97074 Wuerzburg</p>
                <p>Teilnahme: Kostenlos</p>
                <p>Anmeldung: <a href='https://pretix.hackation.de/Hackation/Hack2026.Q2/'>Jetzt anmelden</a></p>
                <h3>Was ist der AI Vibe Hackathon?</h3>
                <p>Der AI Vibe Hackathon bringt Menschen aus Unternehmen, Startups und Community zusammen.</p>
                </body></html>
                """;
        var event = parser.parse(new FetchedPage(source.url, html, null, null), source).events().getFirst();
        assertEquals("AI Vibe Hackathon #4", event.title());
        assertEquals(EventType.HACKATHON, event.eventType());
        assertEquals("Wuerzburg", event.city());
        assertTrue(event.tags().contains("hackation"));
        assertTrue(event.tags().contains("ai"));
    }

    @Test
    void parsesHackationRecapHeaderLayout() {
        var parser = new HackationEventParser();
        var source = new EventSource();
        source.name = "AI WEEK Hackathons Q1";
        source.url = "https://hackation.de/de/events/hackathon-2026-q1/";
        String html = """
                <html><head><meta name='description' content='Recap of the AI Vibe Hackathon'></head><body>
                <h1>AI VIBE HACKATHON #3 - Maerz 2026</h1>
                <p>05. - 06. Maerz 2026, 09:00 - 17:00 Uhr · ZDI Ideenlabor, Wuerzburg</p>
                <h2>AI Vibe Hackathon</h2>
                <h3>Rueckblick</h3>
                <p>Am 5. und 6. Maerz 2026 verwandelten rund 40 Teilnehmende im ZDI Ideenlabor Wuerzburg zwei Tage lang Ideen in funktionierende Prototypen.</p>
                </body></html>
                """;

        var event = parser.parse(new FetchedPage(source.url, html, null, null), source).events().getFirst();

        assertEquals("AI VIBE HACKATHON #3 - Maerz 2026", event.title());
        assertEquals(EventType.HACKATHON, event.eventType());
        assertEquals("Wuerzburg", event.city());
        assertTrue(event.tags().contains("hackation"));
        assertTrue(event.tags().contains("ai"));
    }
}