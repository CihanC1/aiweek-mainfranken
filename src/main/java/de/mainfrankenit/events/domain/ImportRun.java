package de.mainfrankenit.events.domain;
import de.mainfrankenit.shared.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="import_runs")
public class ImportRun extends BaseEntity {
    @Column(nullable=false) public Instant startedAt;
    public Instant finishedAt;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public ImportRunStatus status = ImportRunStatus.RUNNING;
    @Column(nullable=false) public int sourceCount;
    @Column(nullable=false) public int discoveredCount;
    @Column(nullable=false) public int createdCount;
    @Column(nullable=false) public int updatedCount;
    @Column(nullable=false) public int unchangedCount;
    @Column(nullable=false) public int failedCount;
}
