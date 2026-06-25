package de.mainfrankenit.events.adapter.out.importer;
import de.mainfrankenit.events.application.EventDraft;
import java.util.*;
public record ParseResult(List<EventDraft> events, List<String> discoveredLinks, int confidence, List<String> warnings) {
    public static ParseResult events(List<EventDraft> events) { return new ParseResult(events, List.of(), events.isEmpty()?0:80, List.of()); }
    public static ParseResult empty() { return new ParseResult(List.of(), List.of(), 0, List.of()); }
    public ParseResult merge(ParseResult other) {
        var mergedEvents = new ArrayList<EventDraft>(); mergedEvents.addAll(events); mergedEvents.addAll(other.events);
        var mergedLinks = new ArrayList<String>(); mergedLinks.addAll(discoveredLinks); mergedLinks.addAll(other.discoveredLinks);
        var mergedWarnings = new ArrayList<String>(); mergedWarnings.addAll(warnings); mergedWarnings.addAll(other.warnings);
        return new ParseResult(mergedEvents, mergedLinks, Math.max(confidence, other.confidence), mergedWarnings);
    }
}
