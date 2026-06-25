package de.mainfrankenit.identity.domain;
import de.mainfrankenit.events.domain.AttendanceMode;
import de.mainfrankenit.events.domain.EventType;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
import java.util.*;
@Entity @Table(name="app_users")
public class AppUser extends BaseEntity {
    public String displayName;
    @Column(unique=true) public String email;
    public String passwordHash;
    @Column(unique=true) public String phoneNumber;
    @Column(nullable=false) public boolean whatsappOptIn;
    @Column(nullable=false) public boolean enabled = true;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public UserRole role = UserRole.USER;
    public String preferredCity;
    @Enumerated(EnumType.STRING) public AttendanceMode preferredAttendanceMode;
    @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="user_preferred_event_types", joinColumns=@JoinColumn(name="user_id")) @Enumerated(EnumType.STRING) @Column(name="event_type")
    public Set<EventType> preferredEventTypes = new LinkedHashSet<>();
    public String profileLink1;
    public String profileLink2;
    public String profileLink3;
}
