package de.mainfrankenit.identity.domain;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="password_reset_tokens")
public class PasswordResetToken extends BaseEntity {
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="user_id") public AppUser user;
    @Column(nullable=false,unique=true,length=64) public String tokenHash;
    @Column(nullable=false) public Instant expiresAt;
    @Column(nullable=false) public boolean used;
}