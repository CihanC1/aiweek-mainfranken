package de.mainfrankenit.community.domain;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
@Entity @Table(name="user_connections")
public class UserConnection extends BaseEntity {
    @ManyToOne(optional=false) @JoinColumn(name="user_a_id") public AppUser userA;
    @ManyToOne(optional=false) @JoinColumn(name="user_b_id") public AppUser userB;
}