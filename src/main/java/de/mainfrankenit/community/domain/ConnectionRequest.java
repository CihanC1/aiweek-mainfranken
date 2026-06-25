package de.mainfrankenit.community.domain;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
@Entity @Table(name="connection_requests")
public class ConnectionRequest extends BaseEntity {
    @ManyToOne(optional=false) public AppUser requester;
    @ManyToOne(optional=false) public AppUser recipient;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public ConnectionStatus status = ConnectionStatus.PENDING;
}