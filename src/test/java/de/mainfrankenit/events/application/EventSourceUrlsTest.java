package de.mainfrankenit.events.application;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
class EventSourceUrlsTest {
    @Test void mapsAiWeekProgrammePagesToExport() {
        assertEquals(EventSourceUrls.AI_WEEK_EXPORT,
                EventSourceUrls.importUrl("https://www.ai-week.de/programm.php#/veranstaltung/83", "ai-week"));
    }

    @Test void leavesOtherSourcesUntouched() {
        assertEquals("https://example.org/events",
                EventSourceUrls.importUrl(" https://example.org/events ", "generic"));
    }
}