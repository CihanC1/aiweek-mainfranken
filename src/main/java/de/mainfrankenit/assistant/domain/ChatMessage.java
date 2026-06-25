package de.mainfrankenit.assistant.domain;
import de.mainfrankenit.shared.domain.BaseEntity;
import de.mainfrankenit.shared.domain.ChatRole;
import jakarta.persistence.*;
@Entity @Table(name="chat_messages")
public class ChatMessage extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) public ChatSession session;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public ChatRole role;
    @Column(nullable=false, columnDefinition="text") public String content;
}