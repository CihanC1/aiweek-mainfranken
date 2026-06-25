package de.mainfrankenit.events.domain;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="event_changes")
public class EventChange extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) public Event event;
    @Column(nullable=false) public String changedField;
    @Column(columnDefinition="text") public String oldValue;
    @Column(columnDefinition="text") public String newValue;
    @Column(nullable=false) public Instant detectedAt;
}