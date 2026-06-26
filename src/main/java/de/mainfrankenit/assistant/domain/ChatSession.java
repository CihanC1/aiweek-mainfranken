package de.mainfrankenit.assistant.domain;
import de.mainfrankenit.events.domain.AttendanceMode;
import de.mainfrankenit.events.domain.EventType;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
import java.util.*;
@Entity @Table(name="chat_sessions")
public class ChatSession extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) public AppUser user;
    @Column(nullable=false) public String state = "ASK_TOPIC";
    @Column(nullable=false) public boolean completed;
    @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="chat_session_tags", joinColumns=@JoinColumn(name="session_id")) @Column(name="tag")
    public Set<String> tags = new LinkedHashSet<>();
    public String preferredCity;
    @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="chat_session_event_types", joinColumns=@JoinColumn(name="session_id")) @Enumerated(EnumType.STRING) @Column(name="event_type")
    public Set<EventType> preferredEventTypes = new LinkedHashSet<>();
    @Enumerated(EnumType.STRING) public AttendanceMode preferredAttendanceMode;
}
