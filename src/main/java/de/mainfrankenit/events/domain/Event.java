package de.mainfrankenit.events.domain;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
@Entity @Table(name="events", uniqueConstraints=@UniqueConstraint(name="uq_event_fingerprint", columnNames="fingerprint"))
public class Event extends BaseEntity {
    @Column(nullable=false) public String title;
    public String organizer;
    @Column(columnDefinition="text") public String description;
    @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="event_tags", joinColumns=@JoinColumn(name="event_id")) @Column(name="tag")
    public Set<String> tags = new LinkedHashSet<>();
    @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="event_categories", joinColumns=@JoinColumn(name="event_id")) @Column(name="category")
    public Set<String> categories = new LinkedHashSet<>();
    @Enumerated(EnumType.STRING) @Column(nullable=false) public EventType eventType = EventType.OTHER;
    @Column(nullable=false) public OffsetDateTime startAt;
    public OffsetDateTime endAt;
    public String locationName;
    @Column(nullable=false) public String city;
    public String address;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public AttendanceMode attendanceMode;
    @Column(nullable=false, length=2048) public String sourceUrl;
    @Column(nullable=false) public String sourceName;
    public String externalEventId;
    @Column(nullable=false, length=64) public String fingerprint;
    @Column(nullable=false) public Instant lastCheckedAt;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public EventStatus status = EventStatus.ACTIVE;
}
