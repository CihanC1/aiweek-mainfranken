package de.mainfrankenit.identity.domain;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="auth_sessions", indexes=@Index(name="idx_auth_session_refresh",columnList="refresh_token_hash",unique=true))
public class AuthSession extends BaseEntity {
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="user_id") public AppUser user;
    @Column(name="access_token_hash",nullable=false,unique=true,length=64) public String accessTokenHash;
    @Column(name="refresh_token_hash",nullable=false,unique=true,length=64) public String refreshTokenHash;
    @Column(nullable=false) public Instant accessExpiresAt;
    @Column(nullable=false) public Instant refreshExpiresAt;
    @Column(nullable=false) public boolean revoked;
}