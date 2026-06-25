package de.mainfrankenit.events.application;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.runtime.LaunchMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import java.time.*;
@ApplicationScoped
public class EventImportScheduler {
    private static final Logger LOG = Logger.getLogger(EventImportScheduler.class);

    @Inject EventImportService importer;

    @ConfigProperty(name = "event.import.enabled", defaultValue = "true")
    boolean enabled;

    private Instant lastSuccessfulRun = Instant.EPOCH;
    private volatile String pendingForcedTrigger;

    @Scheduled(every = "{event.import.interval}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void importEvents() {
        importNow("Scheduled");
    }

    @Scheduled(every = "5s", delayed = "5s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void processPendingForcedImport() {
        String trigger = pendingForcedTrigger;
        if (trigger == null) return;
        pendingForcedTrigger = null;
        importNow(trigger, true);
    }

    public void requestForcedImport(String trigger) {
        pendingForcedTrigger = trigger == null || trigger.isBlank() ? "Source file change" : trigger;
    }

    public synchronized void importNow(String trigger) {
        importNow(trigger, false);
    }

    public synchronized void importNow(String trigger, boolean force) {
        if (LaunchMode.current() == LaunchMode.TEST) return;
        if (!enabled) return;
        if (!force && Duration.between(lastSuccessfulRun, Instant.now()).compareTo(Duration.ofMinutes(1)) < 0) return;
        var result = importer.run(force);
        if (result.sources() > 0) lastSuccessfulRun = Instant.now();
        LOG.infof("%s event import finished: sources=%d discovered=%d created=%d updated=%d unchanged=%d errors=%s",
                trigger, result.sources(), result.discovered(), result.created(), result.updated(), result.unchanged(), result.errors());
    }
}