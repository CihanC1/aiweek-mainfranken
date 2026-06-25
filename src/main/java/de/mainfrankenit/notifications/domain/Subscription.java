package de.mainfrankenit.notifications.domain;
import de.mainfrankenit.events.domain.Event;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
@Entity @Table(name="subscriptions")
public class Subscription extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) public AppUser user;
    @ManyToOne(fetch=FetchType.LAZY) public Event event;
    public String tag;
    @Column(nullable=false) public boolean active = true;
}