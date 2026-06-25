package de.mainfrankenit.events.application;
import de.mainfrankenit.events.domain.AttendanceMode;
import de.mainfrankenit.events.domain.EventType;
import org.junit.jupiter.api.*;
import java.time.OffsetDateTime;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
class EventNormalizerTest {
    EventNormalizer n=new EventNormalizer();
    @Test void normalizesWhitespaceTagsAndStableFingerprint(){var d=new EventDraft("  Java   Meetup "," JUG "," Desc ",Set.of(" JAVA ","Cloud"),EventType.MEETUP,OffsetDateTime.parse("2030-01-01T18:00:00+01:00"),null," Hub "," Würzburg ",null,AttendanceMode.OFFLINE,"https://example.org/1","Demo","42");var normalized=n.normalize(d);assertEquals("Java Meetup",normalized.title());assertEquals(Set.of("java","cloud"),normalized.tags());assertEquals(n.fingerprint(normalized),n.fingerprint(normalized));}
    @Test void rejectsIncompleteDraft(){assertThrows(IllegalArgumentException.class,()->n.normalize(new EventDraft(null,null,null,null,null,null,null,null,null,null,null,null,null,null)));}
}