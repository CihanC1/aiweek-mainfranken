package de.mainfrankenit.events.domain;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="event_sources")
public class EventSource extends BaseEntity {
    @Column(nullable=false, unique=true) public String name;
    @Column(nullable=false, length=2048) public String url;
    @Column(nullable=false) public boolean active = true;
    @Column(nullable=false) public String parserKey;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public FetchStrategy fetchStrategy = FetchStrategy.HTTP;
    public Instant lastCheckedAt;
    public Instant lastChangedAt;
    public Instant lastSuccessAt;
    @Column(columnDefinition="text") public String lastError;
    @Column(length=64) public String contentHash;
    public String etag;
    public Instant remoteLastModifiedAt;
    @Column(nullable=false) public int checkIntervalMinutes = 30;
    @Column(nullable=false) public int consecutiveFailures = 0;
    public Instant nextCheckAt;
}
