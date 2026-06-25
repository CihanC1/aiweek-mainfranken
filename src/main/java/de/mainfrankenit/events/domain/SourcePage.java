package de.mainfrankenit.events.domain;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="source_pages", uniqueConstraints=@UniqueConstraint(name="uq_source_page_url", columnNames={"source_id","url"}))
public class SourcePage extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) public EventSource source;
    @Column(nullable=false, length=2048) public String url;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public SourcePageType pageType = SourcePageType.UNKNOWN;
    @Column(nullable=false) public boolean active = true;
    public Instant lastCheckedAt;
    public Instant lastChangedAt;
    @Column(length=64) public String contentHash;
    @Column(columnDefinition="text") public String lastError;
}
