package de.mainfrankenit.events.domain;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
@Entity @Table(name="source_import_runs")
public class SourceImportRun extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) public ImportRun importRun;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) public EventSource source;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public ImportRunStatus status = ImportRunStatus.RUNNING;
    @Column(nullable=false) public int fetchedUrlCount;
    @Column(nullable=false) public int eventCount;
    @Column(nullable=false) public int createdCount;
    @Column(nullable=false) public int updatedCount;
    @Column(nullable=false) public int unchangedCount;
    @Column(nullable=false) public long durationMs;
    @Column(columnDefinition="text") public String errorMessage;
}
