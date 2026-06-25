package de.mainfrankenit.recommendations.application;
import de.mainfrankenit.events.domain.AttendanceMode;
import de.mainfrankenit.events.domain.Event;
import de.mainfrankenit.events.domain.EventType;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
class RecommendationServiceTest {
    @Test void deterministicScoringRewardsAllMatches(){var e=new Event();e.title="Java";e.tags=Set.of("java","cloud");e.city="Würzburg";e.eventType=EventType.MEETUP;e.attendanceMode=AttendanceMode.OFFLINE;e.startAt=OffsetDateTime.now().plusDays(3);var r=new RecommendationService().score(e,new RecommendationService.Preferences(Set.of("java"),"Würzburg",Set.of(EventType.MEETUP),AttendanceMode.OFFLINE));assertEquals(80,r.score());assertEquals("BEST",r.group());}
}