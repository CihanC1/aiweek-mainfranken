package de.mainfrankenit.community.domain;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
@Entity @Table(name="direct_messages")
public class DirectMessage extends BaseEntity {
    @ManyToOne(optional=false) @JoinColumn(name="sender_id") public AppUser sender;
    @ManyToOne(optional=false) @JoinColumn(name="recipient_id") public AppUser recipient;
    @Column(nullable=false, columnDefinition="text") public String content;
    @Column(nullable=false) public boolean read;
}
