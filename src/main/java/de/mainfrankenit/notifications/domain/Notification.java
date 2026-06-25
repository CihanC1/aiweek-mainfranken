package de.mainfrankenit.notifications.domain;
import de.mainfrankenit.events.domain.Event;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
@Entity @Table(name="notifications")
public class Notification extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) public AppUser user;
    @ManyToOne(fetch=FetchType.LAZY) public Event event;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public NotificationType type;
    @Column(nullable=false) public String title;
    @Column(nullable=false, columnDefinition="text") public String body;
    @Column(unique=true) public String dedupeKey;
}
