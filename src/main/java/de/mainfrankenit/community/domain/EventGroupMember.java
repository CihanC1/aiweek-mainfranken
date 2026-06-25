package de.mainfrankenit.community.domain;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
@Entity @Table(name="event_group_members", uniqueConstraints=@UniqueConstraint(columnNames={"group_id","user_id"}))
public class EventGroupMember extends BaseEntity {
    @ManyToOne(optional=false) @JoinColumn(name="group_id") public EventGroup group;
    @ManyToOne(optional=false) @JoinColumn(name="user_id") public AppUser user;
}