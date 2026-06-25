package de.mainfrankenit.identity.domain;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
@Entity @Table(name="user_interests", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","tag"}))
public class UserInterest extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="user_id") public AppUser user;
    @Column(nullable=false) public String tag;
}