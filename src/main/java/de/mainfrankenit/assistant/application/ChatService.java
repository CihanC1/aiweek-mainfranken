package de.mainfrankenit.assistant.application;
import de.mainfrankenit.assistant.domain.ChatMessage;
import de.mainfrankenit.assistant.domain.ChatSession;
import de.mainfrankenit.events.domain.AttendanceMode;
import de.mainfrankenit.events.domain.EventType;
import de.mainfrankenit.identity.application.UserService;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.notifications.application.NotificationService;
import de.mainfrankenit.notifications.domain.NotificationType;
import de.mainfrankenit.recommendations.application.RecommendationService;
import de.mainfrankenit.shared.domain.ChatRole;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.time.format.DateTimeFormatter;
import java.util.*;
@ApplicationScoped
public class ChatService {
    private static final DateTimeFormatter EVENT_TIME=DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    @Inject UserService users;
    @Inject RecommendationService recommendations;
    @Inject NotificationService notifications;
    @ConfigProperty(name="app.public-url", defaultValue="http://localhost:3000") String publicUrl;
    public record Reply(UUID sessionId,String state,String message,boolean completed){}
    @Transactional public Reply start(UUID userId){var s=new ChatSession();s.user=userId==null?null:users.get(userId);s.persist();return assistant(s,"Hangi teknoloji konularıyla ilgileniyorsunuz? (Örn. Java, AI, SQL)");}
    @Transactional public Reply message(UUID id,String content){ChatSession s=ChatSession.findById(id);if(s==null)throw new NotFoundException("Chat session not found");save(s,ChatRole.USER,content);
        String response;
        switch(s.state){
            case "ASK_TOPIC"->{ensureUser(s);users.interests(s.user.id,new LinkedHashSet<>(Arrays.asList(content.split("\\s*,\\s*|\\s+ve\\s+"))));s.state="ASK_CITY";response="Hangi şehri tercih edersiniz?";}
            case "ASK_CITY"->{s.user.preferredCity=content.trim();s.state="ASK_TYPE";response="Meetup, workshop, konferans veya hackathon seçeneklerinden hangileri size uygun?";}
            case "ASK_TYPE"->{var types=new LinkedHashSet<EventType>();for(String token:content.split("[, ]+"))try{types.add(EventType.valueOf(token.trim().toUpperCase(Locale.ROOT).replace("KONFERANS","CONFERENCE")));}catch(Exception ignored){}s.user.preferredEventTypes.clear();s.user.preferredEventTypes.addAll(types);s.state="ASK_MODE";response="Online, yüz yüze veya hibrit etkinlikleri mi tercih edersiniz?";}
            case "ASK_MODE"->{String c=content.toLowerCase(Locale.ROOT);s.user.preferredAttendanceMode=c.contains("hib")?AttendanceMode.HYBRID:c.contains("online")?AttendanceMode.ONLINE:AttendanceMode.OFFLINE;s.state="ASK_WHATSAPP";response=recommendationText(s.user)+"\n\nWhatsApp üzerinden düzenli etkinlik bildirimleri almak ister misiniz?";}
            case "ASK_WHATSAPP"->{if(yes(content)){s.state="ASK_PHONE";response="Lütfen telefon numaranızı ülke koduyla yazın (örn. +491234567890).";}else{s.state="COMPLETED";s.completed=true;response="Tamamdır. Etkinlik önerilerinizi burada bıraktım:\n\n"+recommendationText(s.user);}}
            case "ASK_PHONE"->{if(!content.matches("^\\+[1-9][0-9]{7,14}$"))response="Numara E.164 biçiminde olmalı (örn. +491234567890).";else{s.user=users.optIn(s.user.id,content,true);var text=recommendationText(s.user);notifications.create(s.user,null,NotificationType.MATCHING_EVENT,"Size uygun etkinlikler",text);s.state="COMPLETED";s.completed=true;response="WhatsApp bildirimleri açık. Bu önerileri WhatsApp'a da gönderdim:\n\n"+text;}}
            default -> response="Bu sohbet tamamlandı. Önerilerinizi görüntüleyebilirsiniz.";
        }
        return assistant(s,response);
    }
    private void ensureUser(ChatSession s){if(s.user==null)s.user=users.create(null);}
    private boolean yes(String value){String v=value.toLowerCase(Locale.ROOT);return v.equals("evet")||v.equals("yes")||v.equals("ja")||v.equals("e");}
    private Reply assistant(ChatSession s,String text){save(s,ChatRole.ASSISTANT,text);return new Reply(s.id,s.state,text,s.completed);}
    private void save(ChatSession s,ChatRole role,String content){var m=new ChatMessage();m.session=s;m.role=role;m.content=content;m.persist();}
    private String recommendationText(AppUser user){var recs=recommendations.forUser(user.id).stream().limit(5).toList();if(recs.isEmpty())return "Şu an tercihlerinizle eşleşen güncel etkinlik bulamadım. Yeni etkinlikler içeri alındığında tekrar deneyebilirsiniz.";var lines=new ArrayList<String>();lines.add("Size uygun etkinlikler:");for(var r:recs){var e=r.event();lines.add("- "+e.title()+" ("+EVENT_TIME.format(e.startAt())+", "+e.city()+")");lines.add("  "+publicUrl.replaceAll("/+$","")+"/events/"+e.id());}return String.join("\n",lines);}
}