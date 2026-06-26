package de.mainfrankenit.notifications.application;
import de.mainfrankenit.events.domain.Event;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.identity.domain.UserInterest;
import de.mainfrankenit.notifications.adapter.out.whatsapp.WhatsAppPort;
import de.mainfrankenit.notifications.domain.DeliveryChannel;
import de.mainfrankenit.notifications.domain.DeliveryStatus;
import de.mainfrankenit.notifications.domain.Notification;
import de.mainfrankenit.notifications.domain.NotificationDelivery;
import de.mainfrankenit.notifications.domain.NotificationType;
import de.mainfrankenit.recommendations.application.RecommendationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
@ApplicationScoped
public class NotificationService {
    @Inject WhatsAppPort whatsapp;
    @Inject RecommendationService recommendations;
    @ConfigProperty(name="app.public-url", defaultValue="http://localhost:3000") String publicUrl;

    @Transactional
    public Notification create(AppUser user, Event event, NotificationType type, String title, String body) {
        return create(user,event,type,title,body,null);
    }

    @Transactional
    public Notification create(AppUser user, Event event, NotificationType type, String title, String body, String dedupeKey) {
        if (dedupeKey != null) {
            Notification existing = Notification.find("dedupeKey", dedupeKey).firstResult();
            if (existing != null) return existing;
        }
        var n = new Notification();
        n.user=user; n.event=event; n.type=type; n.title=title; n.body=body; n.dedupeKey=dedupeKey; n.persist();
        var d = new NotificationDelivery();
        d.notification=n; d.channel=user.whatsappOptIn?DeliveryChannel.WHATSAPP:DeliveryChannel.WEB;
        if (d.channel==DeliveryChannel.WHATSAPP) sendWhatsApp(d,notificationBody(n));
        d.persist();
        return n;
    }

    @Transactional
    public NotificationDelivery deliverWhatsApp(Notification notification) {
        var d = new NotificationDelivery();
        d.notification=notification; d.channel=DeliveryChannel.WHATSAPP;
        sendWhatsApp(d,notificationBody(notification)); d.persist();
        return d;
    }

    @Transactional
    public void eventUpdated(Event event) { eventUpdated(event, Set.of("event")); }

    @Transactional
    public void eventUpdated(Event event, Set<String> changedFields) {
        String fields = String.join(",", changedFields.stream().sorted().toList());
        for (AppUser user : AppUser.<AppUser>find("enabled=true and whatsappOptIn=true").list()) {
            if (!matchesSavedPreferences(user,event)) continue;
            create(user,event,NotificationType.EVENT_UPDATED,"Event updated",
                    event.title+" changed: "+fields+"\n"+eventLink(event),
                    "EVENT_UPDATED:"+user.id+":"+event.id+":"+fields);
        }
    }

    @Transactional
    public void eventCreated(Event event) {
        for (AppUser user : AppUser.<AppUser>find("enabled=true and whatsappOptIn=true").list()) {
            notifyMatchingUser(user,event);
        }
    }

    private void notifyMatchingUser(AppUser user, Event event) {
        if (!matchesSavedPreferences(user,event)) return;
        create(user,event,NotificationType.MATCHING_EVENT,"Neues passendes Event",
                event.title+" passt zu deinen Interessen.\nWann: "+event.startAt+"\nOrt: "+event.city+"\n"+eventLink(event),
                "MATCHING_EVENT:"+user.id+":"+event.id);
    }

    @Transactional
    public void eventCancelled(Event event) {
        for (AppUser user : AppUser.<AppUser>find("enabled=true and whatsappOptIn=true").list()) {
            if (!matchesSavedPreferences(user,event)) continue;
            create(user,event,NotificationType.EVENT_CANCELLED,"Event cancelled",
                    event.title+" is no longer available.\n"+eventLink(event),
                    "EVENT_CANCELLED:"+user.id+":"+event.id);
        }
    }

    private boolean matchesSavedPreferences(AppUser user, Event event) {
        var interests = UserInterest.<UserInterest>find("user",user).list().stream()
                .map(i -> norm(i.tag)).filter(s -> !s.isBlank()).collect(java.util.stream.Collectors.toSet());
        if (interests.isEmpty()) return false;
        var recommendation = recommendations.score(event,recommendations.preferences(user));
        return recommendation.reasons().stream().anyMatch(reason -> reason.startsWith("passende Themen:"));
    }

    private String notificationBody(Notification notification) { return notification.title+"\n\n"+notification.body; }
    private void sendWhatsApp(NotificationDelivery d,String body) {
        if (!d.notification.user.whatsappOptIn) { d.status=DeliveryStatus.FAILED; d.errorMessage="User has not opted in"; return; }
        var r = whatsapp.send(d.notification.user.phoneNumber,body);
        d.status=r.success()?DeliveryStatus.SENT:DeliveryStatus.FAILED; d.providerReference=r.providerReference(); d.errorMessage=r.success()?null:(r.error()==null||r.error().isBlank()?"Unknown WhatsApp provider error":r.error());
        if (r.success()) d.deliveredAt=Instant.now();
    }
    private String eventLink(Event event) { return publicUrl.replaceAll("/+$","")+"/events/"+event.id; }
    private String norm(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," "); }
}
