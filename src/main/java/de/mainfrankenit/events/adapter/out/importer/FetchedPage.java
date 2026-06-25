package de.mainfrankenit.events.adapter.out.importer;
public record FetchedPage(String url, String body, String etag, String lastModified) {}
