package de.mainfrankenit.events.application;
import de.mainfrankenit.events.adapter.out.importer.EventParser;
import de.mainfrankenit.events.adapter.out.importer.ContentHasher;
import de.mainfrankenit.events.adapter.out.importer.FetchedPage;
import de.mainfrankenit.events.adapter.out.importer.HttpPageFetcher;
import de.mainfrankenit.events.adapter.out.importer.JsonLdEventExtractor;
import de.mainfrankenit.events.adapter.out.importer.LinkDiscoveryService;
import de.mainfrankenit.events.domain.Event;
import de.mainfrankenit.events.domain.EventChange;
import de.mainfrankenit.events.domain.EventSource;
import de.mainfrankenit.events.domain.EventStatus;
import de.mainfrankenit.events.domain.ImportRun;
import de.mainfrankenit.events.domain.ImportRunStatus;
import de.mainfrankenit.events.domain.SourceImportRun;
import de.mainfrankenit.events.domain.SourcePage;
import de.mainfrankenit.events.domain.SourcePageType;
import de.mainfrankenit.notifications.application.NotificationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
@ApplicationScoped
public class EventImportService {
    private static final Logger LOG = Logger.getLogger(EventImportService.class);
    private static final int MAX_PAGES_PER_SOURCE = 10;
    private static final Set<String> MEANINGFUL_NOTIFICATION_FIELDS = Set.of("title","eventType","startAt","endAt","locationName","city","address","attendanceMode");
    @Inject EventNormalizer normalizer;
    @Inject HttpPageFetcher fetcher;
    @Inject ContentHasher hasher;
    @Inject LinkDiscoveryService links;
    @Inject JsonLdEventExtractor jsonLd;
    @Inject jakarta.enterprise.inject.Instance<EventParser> parsers;
    @Inject NotificationService notifications;

    public record ImportResult(int sources, int discovered, int created, int updated, int unchanged, List<String> errors) {}

    @Transactional
    public ImportResult run() { return run(false); }

    @Transactional
    public ImportResult run(boolean force) {
        int sourceCount=0, discovered=0, created=0, updated=0, unchanged=0;
        var errors = new ArrayList<String>();
        var run = new ImportRun(); run.startedAt=Instant.now(); run.persist();
        for (EventSource source : EventSource.<EventSource>list("active", true)) {
            if (!force && source.nextCheckAt != null && source.nextCheckAt.isAfter(Instant.now())) continue;
            sourceCount++;
            var sourceRun = new SourceImportRun(); sourceRun.importRun=run; sourceRun.source=source; sourceRun.persist();
            var started = Instant.now();
            try {
                var parser = parsers.stream().filter(p -> p.key().equals(source.parserKey)).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Unknown parser: " + source.parserKey));
                int sourceEvents=0, sourceCreated=0, sourceUpdated=0, sourceUnchanged=0, fetched=0;
                for (var pageUrl : pageUrls(source)) {
                    var page = fetcher.fetch(EventSourceUrls.importUrl(pageUrl, source.parserKey));
                    fetched++;
                    boolean changed = markPageChecked(source, pageUrl, page);
                    if (!changed) continue;
                    var parsed = parser.parse(page, source);
                    if (!"ai-week".equals(source.parserKey)) parsed = jsonLd.parse(page, source).merge(parsed);
                    rememberLinks(source, links.discover(page, MAX_PAGES_PER_SOURCE));
                    for (var draft : parsed.events()) {
                        discovered++; sourceEvents++;
                        var outcome = upsert(draft);
                        if (outcome == 1) { created++; sourceCreated++; }
                        else if (outcome == 2) { updated++; sourceUpdated++; }
                        else { unchanged++; sourceUnchanged++; }
                    }
                    if (!parsed.warnings().isEmpty()) LOG.debugf("Parser warnings for %s: %s", page.url(), parsed.warnings());
                }
                source.consecutiveFailures=0; source.lastError=null; source.lastSuccessAt=Instant.now(); scheduleNext(source);
                sourceRun.fetchedUrlCount=fetched; sourceRun.eventCount=sourceEvents; sourceRun.createdCount=sourceCreated; sourceRun.updatedCount=sourceUpdated; sourceRun.unchangedCount=sourceUnchanged; sourceRun.status=ImportRunStatus.SUCCESS;
            } catch (RuntimeException e) { errors.add(source.name + ": " + e.getMessage()); }
            finally {
                source.lastCheckedAt=Instant.now();
                if (!errors.isEmpty() && errors.getLast().startsWith(source.name + ":")) {
                    source.consecutiveFailures++; source.lastError=errors.getLast(); scheduleNext(source);
                    sourceRun.status=ImportRunStatus.FAILED; sourceRun.errorMessage=errors.getLast();
                }
                sourceRun.durationMs=Duration.between(started, Instant.now()).toMillis();
            }
        }
        run.sourceCount=sourceCount; run.discoveredCount=discovered; run.createdCount=created; run.updatedCount=updated; run.unchangedCount=unchanged; run.failedCount=errors.size();
        run.status=errors.isEmpty()?ImportRunStatus.SUCCESS:(sourceCount==errors.size()?ImportRunStatus.FAILED:ImportRunStatus.PARTIAL);
        run.finishedAt=Instant.now();
        return new ImportResult(sourceCount, discovered, created, updated, unchanged, errors);
    }

    /** 1 created, 2 updated, 0 unchanged. Public for deterministic import/test adapters. */
    @Transactional
    public int upsert(EventDraft raw) {
        var d = normalizer.normalize(raw);
        var fingerprint = normalizer.fingerprint(d);
        Event event = Event.find("fingerprint = ?1 or sourceUrl = ?2", fingerprint, d.sourceUrl()).firstResult();
        if (event == null) { event = new Event(); apply(event, d, fingerprint); event.persist(); notifications.eventCreated(event); return 1; }
        var changes = changes(event, d);
        event.lastCheckedAt = Instant.now();
        if (changes.isEmpty()) return 0;
        for (var entry : changes.entrySet()) {
            var values = entry.getValue(); var change = new EventChange(); change.event=event; change.changedField=entry.getKey();
            change.oldValue=values[0]; change.newValue=values[1]; change.detectedAt=Instant.now(); change.persist();
        }
        apply(event, d, fingerprint); event.status = EventStatus.UPDATED;
        if (changes.keySet().stream().anyMatch(MEANINGFUL_NOTIFICATION_FIELDS::contains)) notifications.eventUpdated(event, changes.keySet());
        return 2;
    }

    private void apply(Event e, EventDraft d, String fp) {
        e.title=d.title(); e.organizer=d.organizer(); e.description=d.description(); e.tags.clear(); e.tags.addAll(d.tags());
        e.eventType=d.eventType(); e.startAt=d.startAt(); e.endAt=d.endAt(); e.locationName=d.locationName(); e.city=d.city(); e.address=d.address();
        e.attendanceMode=d.attendanceMode(); e.sourceUrl=d.sourceUrl(); e.sourceName=d.sourceName(); e.externalEventId=d.externalEventId();
        e.fingerprint=fp; e.lastCheckedAt=Instant.now(); if (e.status == null) e.status=EventStatus.ACTIVE;
    }
    private Map<String,String[]> changes(Event e, EventDraft d) {
        var m = new LinkedHashMap<String,String[]>();
        compareText(m,"title",e.title,d.title()); compareText(m,"organizer",e.organizer,d.organizer()); compareText(m,"description",e.description,d.description());
        compareTags(m,"tags",e.tags,d.tags()); compare(m,"eventType",e.eventType,d.eventType()); compareTime(m,"startAt",e.startAt,d.startAt());
        compareTime(m,"endAt",e.endAt,d.endAt()); compareText(m,"locationName",e.locationName,d.locationName()); compareText(m,"city",e.city,d.city());
        compareText(m,"address",e.address,d.address()); compare(m,"attendanceMode",e.attendanceMode,d.attendanceMode());
        return m;
    }
    private void compare(Map<String,String[]> m, String field, Object oldV, Object newV) {
        if (!Objects.equals(oldV,newV)) m.put(field,new String[]{Objects.toString(oldV,null),Objects.toString(newV,null)});
    }
    private void compareText(Map<String,String[]> m, String field, String oldV, String newV) {
        if (!Objects.equals(normalizer.text(oldV),normalizer.text(newV))) m.put(field,new String[]{Objects.toString(oldV,null),Objects.toString(newV,null)});
    }
    private void compareTime(Map<String,String[]> m, String field, OffsetDateTime oldV, OffsetDateTime newV) {
        if (!Objects.equals(toInstant(oldV),toInstant(newV))) m.put(field,new String[]{Objects.toString(oldV,null),Objects.toString(newV,null)});
    }
    private void compareTags(Map<String,String[]> m, String field, Set<String> oldV, Set<String> newV) {
        var oldTags=normalizeTags(oldV); var newTags=normalizeTags(newV);
        if (!Objects.equals(oldTags,newTags)) m.put(field,new String[]{Objects.toString(oldV,null),Objects.toString(newV,null)});
    }
    private Instant toInstant(OffsetDateTime value) { return value == null ? null : value.toInstant(); }
    private Set<String> normalizeTags(Set<String> tags) {
        if (tags == null) return Set.of();
        var result = new LinkedHashSet<String>();
        for (String tag : tags) { var value=normalizer.text(tag); if(!value.isBlank()) result.add(value); }
        return result;
    }

    private List<String> pageUrls(EventSource source) {
        var urls = new LinkedHashSet<String>();
        urls.add(source.url);
        SourcePage.<SourcePage>find("source=?1 and active=true order by updatedAt desc", source).page(0, MAX_PAGES_PER_SOURCE - 1).list().forEach(p -> urls.add(p.url));
        return urls.stream().limit(MAX_PAGES_PER_SOURCE).toList();
    }

    private boolean markPageChecked(EventSource source, String requestedUrl, FetchedPage page) {
        var hash = hasher.hash(page.body());
        if (Objects.equals(source.url, requestedUrl)) {
            boolean changed = !Objects.equals(source.contentHash, hash);
            source.contentHash=hash; source.etag=page.etag(); source.remoteLastModifiedAt=parseHttpDate(page.lastModified());
            if (changed) source.lastChangedAt=Instant.now();
            return changed || source.lastSuccessAt == null;
        }
        SourcePage sourcePage = SourcePage.find("source=?1 and url=?2", source, requestedUrl).firstResult();
        if (sourcePage == null) { sourcePage = new SourcePage(); sourcePage.source=source; sourcePage.url=requestedUrl; sourcePage.pageType=SourcePageType.UNKNOWN; }
        boolean changed = !Objects.equals(sourcePage.contentHash, hash);
        sourcePage.contentHash=hash; sourcePage.lastCheckedAt=Instant.now(); sourcePage.lastError=null;
        if (changed) sourcePage.lastChangedAt=Instant.now();
        sourcePage.persist();
        return changed || sourcePage.lastChangedAt == null;
    }

    private void rememberLinks(EventSource source, List<LinkDiscoveryService.DiscoveredLink> discovered) {
        for (var link : discovered) {
            if (Objects.equals(source.url, link.url())) continue;
            if (SourcePage.count("source=?1 and url=?2", source, link.url()) > 0) continue;
            var page = new SourcePage(); page.source=source; page.url=link.url(); page.pageType=link.type(); page.active=true; page.persist();
        }
    }

    private Instant parseHttpDate(String value) {
        try { return value == null ? null : DateTimeFormatter.RFC_1123_DATE_TIME.parse(value, Instant::from); } catch (Exception e) { return null; }
    }

    private void scheduleNext(EventSource source) {
        int failures = Math.max(0, source.consecutiveFailures);
        long minutes = Math.max(5, source.checkIntervalMinutes) * (long)Math.min(12, Math.pow(2, failures));
        source.nextCheckAt=Instant.now().plus(Duration.ofMinutes(minutes));
    }
}
