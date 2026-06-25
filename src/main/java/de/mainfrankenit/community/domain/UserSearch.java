package de.mainfrankenit.community.domain;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
@Entity @Table(name="user_searches")
public class UserSearch extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) public AppUser user;
    @Column(nullable=false) public String query;
    @Column(nullable=false) public String normalizedQuery;
}