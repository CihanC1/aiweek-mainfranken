package de.mainfrankenit.assistant.domain;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
@Entity @Table(name="chat_sessions")
public class ChatSession extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) public AppUser user;
    @Column(nullable=false) public String state = "ASK_TOPIC";
    @Column(nullable=false) public boolean completed;
}