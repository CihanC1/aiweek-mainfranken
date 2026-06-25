package de.mainfrankenit.events.adapter.out.importer;
import de.mainfrankenit.events.domain.EventSource;
public interface EventParser { String key(); ParseResult parse(FetchedPage page, EventSource source); }
