package de.mainfrankenit.community.domain;

import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;

@Entity @Table(name="message_requests")
public class MessageRequest extends BaseEntity {
    @ManyToOne(optional=false) @JoinColumn(name="requester_id") public AppUser requester;
    @ManyToOne(optional=false) @JoinColumn(name="recipient_id") public AppUser recipient;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public MessageRequestStatus status;
}
