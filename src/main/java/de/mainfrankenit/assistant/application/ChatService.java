package de.mainfrankenit.assistant.application;

import de.mainfrankenit.assistant.domain.ChatMessage;
import de.mainfrankenit.assistant.domain.ChatSession;
import de.mainfrankenit.events.application.TaxonomyService;
import de.mainfrankenit.events.domain.AttendanceMode;
import de.mainfrankenit.events.domain.EventType;
import de.mainfrankenit.identity.application.UserService;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.identity.domain.UserInterest;
import de.mainfrankenit.notifications.application.NotificationService;
import de.mainfrankenit.notifications.domain.DeliveryChannel;
import de.mainfrankenit.notifications.domain.DeliveryStatus;
import de.mainfrankenit.notifications.domain.NotificationDelivery;
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
    private static final DateTimeFormatter EVENT_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Inject UserService users;
    @Inject RecommendationService recommendations;
    @Inject NotificationService notifications;
    @Inject TaxonomyService taxonomy;
    @ConfigProperty(name = "app.public-url", defaultValue = "http://localhost:3000") String publicUrl;

    public record Reply(UUID sessionId, String state, String message, boolean completed) {}

    @Transactional
    public Reply start(UUID userId) {
        var s = new ChatSession();
        s.user = userId == null ? null : users.get(userId);
        s.persist();
        return assistant(s, "Für welche Technologiethemen interessierst du dich? (z. B. Java, AI, SQL)");
    }

    @Transactional
    public Reply message(UUID id, String content) {
        ChatSession s = ChatSession.findById(id);
        if (s == null) throw new NotFoundException("Chat session not found");
        save(s, ChatRole.USER, content);

        String response;
        switch (s.state) {
            case "ASK_TOPIC" -> {
                ensureUser(s);
                var topics = extractTopics(content);
                s.tags.clear();
                s.tags.addAll(topics);
                s.state = "ASK_CITY";
                response = (topics.isEmpty()
                        ? "Ich konnte noch kein klares Thema erkennen. Du kannst später im Profil Tags wie AI, SQL oder Python ergänzen."
                        : "Ich habe diese Themen erkannt: " + String.join(", ", topics) + ".")
                        + " Welche Stadt bevorzugst du?";
            }
            case "ASK_CITY" -> {
                var city = extractCity(content);
                s.preferredCity = city;
                s.state = "ASK_TYPE";
                response = (city == null ? "Alles klar, ich lasse die Stadt offen." : "Alles klar, ich filtere nach " + city + ".")
                        + " Welche Formate passen zu dir: Meetup, Workshop, Konferenz oder Hackathon?";
            }
            case "ASK_TYPE" -> {
                var types = extractTypes(content);
                s.preferredEventTypes.clear();
                s.preferredEventTypes.addAll(types);
                s.state = "ASK_MODE";
                response = (types.isEmpty() ? "Ich lasse den Event-Typ offen." : "Ich habe diese Event-Typen erkannt: " + typeLabels(types) + ".")
                        + " Bevorzugst du Online-, Vor-Ort- oder hybride Events?";
            }
            case "ASK_MODE" -> {
                s.preferredAttendanceMode = extractMode(content);
                s.state = "ASK_WHATSAPP";
                response = sessionRecommendationText(s)
                        + "\n\nMöchtest du regelmäßige Event-Benachrichtigungen per WhatsApp erhalten?";
            }
            case "ASK_WHATSAPP" -> {
                if (yes(content)) {
                    s.state = "ASK_PHONE";
                    response = "Bitte gib deine Telefonnummer mit Ländercode ein (z. B. +491234567890).";
                } else {
                    s.state = "COMPLETED";
                    s.completed = true;
                    response = "Alles klar. Hier sind deine Event-Empfehlungen:\n\n" + sessionRecommendationText(s);
                }
            }
            case "ASK_PHONE" -> {
                if (!content.matches("^\\+[1-9][0-9]{7,14}$")) {
                    response = "Die Nummer muss im E.164-Format sein (z. B. +491234567890).";
                } else {
                    s.user = users.optIn(s.user.id, content, true);
                    var text = profileRecommendationText(s.user);
                    var notification = notifications.create(s.user, null, NotificationType.MATCHING_EVENT, "Passende Events für dich", text);
                    NotificationDelivery delivery = NotificationDelivery.find("notification=?1 and channel=?2", notification, DeliveryChannel.WHATSAPP).firstResult();
                    s.state = "COMPLETED";
                    s.completed = true;
                    if (delivery != null && delivery.status == DeliveryStatus.SENT) {
                        response = "WhatsApp-Benachrichtigungen sind aktiviert. Diese Empfehlungen habe ich dir auch per WhatsApp geschickt:\n\n" + text;
                    } else {
                        var error = delivery == null || delivery.errorMessage == null || delivery.errorMessage.isBlank()
                                ? "Twilio konnte die Nachricht gerade nicht zustellen."
                                : delivery.errorMessage;
                        response = "WhatsApp-Benachrichtigungen sind aktiviert, aber die erste WhatsApp konnte nicht gesendet werden: " + error
                                + "\n\nDeine Empfehlungen:\n\n" + text;
                    }
                }
            }
            default -> response = "Dieser Chat ist abgeschlossen. Du kannst deine Empfehlungen im Profil ansehen.";
        }
        return assistant(s, response);
    }

    private void ensureUser(ChatSession s) {
        if (s.user == null) s.user = users.create(null);
    }

    private boolean yes(String value) {
        String v = value.toLowerCase(Locale.ROOT).trim();
        return v.equals("evet") || v.equals("yes") || v.equals("ja") || v.equals("j") || v.equals("e");
    }

    private Reply assistant(ChatSession s, String text) {
        save(s, ChatRole.ASSISTANT, text);
        return new Reply(s.id, s.state, text, s.completed);
    }

    private void save(ChatSession s, ChatRole role, String content) {
        var m = new ChatMessage();
        m.session = s;
        m.role = role;
        m.content = content;
        m.persist();
    }

    private String sessionRecommendationText(ChatSession s) {
        var prefs = new RecommendationService.Preferences(s.tags, s.preferredCity, s.preferredEventTypes, s.preferredAttendanceMode);
        return recommendationText(recommendations.preview(prefs).stream().limit(5).toList(), s.tags);
    }

    private String profileRecommendationText(AppUser user) {
        var interests = UserInterest.<UserInterest>find("user", user).list().stream().map(i -> i.tag).toList();
        return recommendationText(recommendations.forUser(user.id).stream().limit(5).toList(), interests);
    }

    private String recommendationText(List<RecommendationService.Recommendation> recs, Collection<String> interests) {
        if (recs.isEmpty()) {
            return interests.isEmpty()
                    ? "Ich habe aktuell keine Events gefunden, die genau zu deinen gespeicherten Profil-Präferenzen passen. Pflege im Profil Tags wie Java, AI oder SQL, damit künftige Benachrichtigungen besser passen."
                    : "Ich habe aktuell keine Events gefunden, die zu diesen Themen passen: " + String.join(", ", interests) + ". Du kannst einzelne Themen entfernen oder breitere Tags wie AI, Data Engineering oder Cloud probieren.";
        }
        var lines = new ArrayList<String>();
        lines.add("Passende Events für dich:");
        for (var r : recs) {
            var e = r.event();
            lines.add("- " + e.title() + " (" + EVENT_TIME.format(e.startAt()) + ", " + e.city() + ")");
            lines.add("  " + publicUrl.replaceAll("/+$", "") + "/events/" + e.id());
        }
        return String.join("\n", lines);
    }

    private Set<String> extractTopics(String content) {
        var result = new LinkedHashSet<>(taxonomy.extractTerms(content));
        for (var token : normalize(content).split("[^\\p{L}\\p{N}+#.]+")) {
            if (token.length() >= 2 && !STOPWORDS.contains(token)) result.add(token);
        }
        return result;
    }

    private String extractCity(String content) {
        var c = content.trim();
        if (c.isBlank() || normalize(c).matches(".*\\b(egal|keine|online|remote|anywhere|any|no preference)\\b.*")) return null;
        return c;
    }

    private Set<EventType> extractTypes(String content) {
        var c = normalize(content);
        var types = new LinkedHashSet<EventType>();
        if (c.contains("meetup")) types.add(EventType.MEETUP);
        if (c.contains("workshop")) types.add(EventType.WORKSHOP);
        if (c.contains("konferenz") || c.contains("conference") || c.contains("konferans")) types.add(EventType.CONFERENCE);
        if (c.contains("hackathon")) types.add(EventType.HACKATHON);
        return types;
    }

    private AttendanceMode extractMode(String content) {
        var c = normalize(content);
        if (c.contains("hybrid") || c.contains("hibrit")) return AttendanceMode.HYBRID;
        if (c.contains("online") || c.contains("remote")) return AttendanceMode.ONLINE;
        if (c.contains("offline") || c.contains("vor ort") || c.contains("präsenz") || c.contains("praesenz")) return AttendanceMode.OFFLINE;
        return null;
    }

    private String typeLabels(Set<EventType> types) {
        return String.join(", ", types.stream().map(t -> switch (t) {
            case MEETUP -> "Meetup";
            case WORKSHOP -> "Workshop";
            case CONFERENCE -> "Konferenz";
            case HACKATHON -> "Hackathon";
            default -> "Event";
        }).toList());
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static final Set<String> STOPWORDS = Set.of("ich", "i", "think", "would", "like", "learn", "lernen",
            "something", "about", "etwas", "ueber", "über", "und", "and", "oder", "or", "hmm", "vielleicht",
            "will", "want", "möchte", "moechte", "gerne", "interessiere", "interessiert", "mich", "zu", "zum",
            "zur", "the", "a", "an", "to", "mit", "for", "für", "fuer");
}
