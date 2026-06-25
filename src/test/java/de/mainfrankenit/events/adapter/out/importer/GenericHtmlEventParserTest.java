package de.mainfrankenit.events.adapter.out.importer;
import de.mainfrankenit.events.domain.EventSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GenericHtmlEventParserTest {
 @Test void parsesDeterministically(){var s=new EventSource();s.name="Demo";s.url="https://example.org";var html="<article data-event data-title='AI Workshop' data-start='2030-01-01T18:00:00+01:00' data-city='Bamberg' data-mode='hybrid' data-tags='ai, java'><a href='/42'>x</a></article>";var d=new GenericHtmlEventParser().parse(new FetchedPage("https://example.org",html,null,null),s).events().getFirst();assertEquals("AI Workshop",d.title());assertEquals("https://example.org/42",d.sourceUrl());assertTrue(d.tags().contains("ai"));}
}
