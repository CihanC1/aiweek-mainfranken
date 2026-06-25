package de.mainfrankenit.community.domain;
import de.mainfrankenit.events.domain.Event;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
@Entity @Table(name="event_groups")
public class EventGroup extends BaseEntity {
    @OneToOne(optional=false) @JoinColumn(name="event_id", unique=true) public Event event;
    @Column(nullable=false) public String name;
}