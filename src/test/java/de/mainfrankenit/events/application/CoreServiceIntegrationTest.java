package de.mainfrankenit.events.application;
import de.mainfrankenit.assistant.application.ChatService;
import de.mainfrankenit.assistant.domain.ChatMessage;
import de.mainfrankenit.community.domain.UserSearch;
import de.mainfrankenit.events.domain.AttendanceMode;
import de.mainfrankenit.events.domain.Event;
import de.mainfrankenit.events.domain.EventChange;
import de.mainfrankenit.events.domain.EventStatus;
import de.mainfrankenit.events.domain.EventSource;
import de.mainfrankenit.events.domain.EventType;
import de.mainfrankenit.identity.application.UserService;
import de.mainfrankenit.notifications.adapter.out.whatsapp.WhatsAppPort;
import de.mainfrankenit.notifications.application.NotificationService;
import de.mainfrankenit.notifications.domain.DeliveryChannel;
import de.mainfrankenit.notifications.domain.DeliveryStatus;
import de.mainfrankenit.notifications.domain.Notification;
import de.mainfrankenit.notifications.domain.NotificationDelivery;
import de.mainfrankenit.notifications.domain.NotificationType;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
@QuarkusTest
class CoreServiceIntegrationTest {
    @Inject EventImportService importer; @Inject SearchService search; @Inject UserService users;
    @Inject NotificationService notifications; @Inject ChatService chat; @Inject WhatsAppPort whatsapp;

    private EventDraft draft(String description,String city){return new EventDraft("Unique Integration Java Event","JUG",description,Set.of("java","cloud"),EventType.MEETUP,OffsetDateTime.parse("2031-01-01T18:00:00+01:00"),null,"Hub",city,null,AttendanceMode.OFFLINE,"https://integration.example/event/unique","Integration","unique-1");}

    @Test @TestTransaction
    void detectsDuplicatesAndCreatesFieldChanges(){assertEquals(1,importer.upsert(draft("first","Würzburg")));assertEquals(0,importer.upsert(draft("first","Würzburg")));assertEquals(2,importer.upsert(draft("changed","Bamberg")));Event event=Event.find("sourceUrl","https://integration.example/event/unique").firstResult();assertEquals(EventStatus.UPDATED,event.status);assertEquals(2,EventChange.count("event",event));}

    @Test @TestTransaction
    void keywordSearchRecordsHistoryAndMatchesTags(){importer.upsert(draft("first","Würzburg"));var user=users.create("Search User");var found=search.events("cloud",user.id);assertTrue(found.stream().anyMatch(e->e.title().contains("Unique Integration")));assertEquals(1,UserSearch.count("user",user));}

    @Test @TestTransaction
    void newEventsNotifyOptedInUsers(){var user=users.create("Notify User");users.optIn(user.id,"+491234567892",true);assertEquals(1,importer.upsert(draft("first","Würzburg")));Event event=Event.find("sourceUrl","https://integration.example/event/unique").firstResult();Notification notification=Notification.find("user=?1 and event=?2 and type=?3",user,event,NotificationType.NEW_EVENT).firstResult();assertNotNull(notification);assertTrue(notification.body.contains("Check it out"));NotificationDelivery delivery=NotificationDelivery.find("notification",notification).firstResult();assertNotNull(delivery);assertEquals(DeliveryChannel.WHATSAPP,delivery.channel);assertEquals(DeliveryStatus.SENT,delivery.status);}

    @Test @TestTransaction
    void cancellingMissingEventsNotifiesUsers(){var user=users.create("Cancel User");users.optIn(user.id,"+491234567893",true);assertEquals(1,importer.upsert(draft("first","Würzburg")));Event event=Event.find("sourceUrl","https://integration.example/event/unique").firstResult();var source=new EventSource();source.name=event.sourceName;source.url="https://integration.example/events";source.parserKey="generic";source.persist();assertEquals(1,importer.cancelMissingEvents(source,Set.of()));assertEquals(EventStatus.CANCELLED,event.status);Notification notification=Notification.find("user=?1 and event=?2 and type=?3",user,event,NotificationType.EVENT_CANCELLED).firstResult();assertNotNull(notification);assertTrue(notification.body.contains("no longer available"));}

    @Test @TestTransaction
    void guidedChatPersistsMessages(){var user=users.create("Chat User");var started=chat.start(user.id);assertEquals("ASK_TOPIC",started.state());var reply=chat.message(started.sessionId(),"java, ai");assertEquals("ASK_CITY",reply.state());assertEquals(3,ChatMessage.count("session.id",started.sessionId()));}

    @Test @TestTransaction
    void guidedChatReturnsRecommendationsAndSendsWhatsApp(){importer.upsert(draft("first","Würzburg"));var user=users.create("Chat Recommend User");var started=chat.start(user.id);chat.message(started.sessionId(),"java");chat.message(started.sessionId(),"Würzburg");chat.message(started.sessionId(),"meetup");var withRecommendations=chat.message(started.sessionId(),"offline");assertEquals("ASK_WHATSAPP",withRecommendations.state());assertTrue(withRecommendations.message().contains("Unique Integration Java Event"));chat.message(started.sessionId(),"evet");var completed=chat.message(started.sessionId(),"+491234567891");assertTrue(completed.completed());assertTrue(completed.message().contains("WhatsApp"));assertTrue(completed.message().contains("Unique Integration Java Event"));NotificationDelivery delivery=NotificationDelivery.find("notification.user=?1 and channel=?2",user,DeliveryChannel.WHATSAPP).firstResult();assertNotNull(delivery);assertEquals(DeliveryStatus.SENT,delivery.status);}

    @Test void mockWhatsAppValidatesPhone(){assertTrue(whatsapp.send("+491234567890","hello").success());assertFalse(whatsapp.send("123","hello").success());}
}