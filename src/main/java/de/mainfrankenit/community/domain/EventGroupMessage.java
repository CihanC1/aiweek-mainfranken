package de.mainfrankenit.community.domain;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.shared.domain.BaseEntity;
import de.mainfrankenit.shared.domain.ChatRole;
import jakarta.persistence.*;
@Entity @Table(name="event_group_messages")
public class EventGroupMessage extends BaseEntity {
    @ManyToOne(optional=false) @JoinColumn(name="group_id") public EventGroup group;
    @ManyToOne @JoinColumn(name="user_id") public AppUser user;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public ChatRole role;
    @Column(nullable=false,columnDefinition="text") public String content;
}