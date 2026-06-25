package de.mainfrankenit.notifications.domain;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="notification_deliveries")
public class NotificationDelivery extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) public Notification notification;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public DeliveryChannel channel;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public DeliveryStatus status = DeliveryStatus.PENDING;
    public String providerReference;
    public String errorMessage;
    public Instant deliveredAt;
}