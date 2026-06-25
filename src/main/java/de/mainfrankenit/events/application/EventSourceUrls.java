package de.mainfrankenit.events.application;
public final class EventSourceUrls {
    public static final String AI_WEEK_EXPORT = "https://backend.timetable.ai-week.de/export/session.json";

    private EventSourceUrls() {}

    public static String importUrl(String url, String parserKey) {
        if (url == null) return null;
        var trimmed = url.trim();
        if ("ai-week".equals(parserKey) && isAiWeekPage(trimmed)) return AI_WEEK_EXPORT;
        return trimmed;
    }

    private static boolean isAiWeekPage(String url) {
        return url.startsWith("https://www.ai-week.de/programm.php")
                || url.startsWith("http://www.ai-week.de/programm.php")
                || url.startsWith("https://ai-week.de/programm.php")
                || url.startsWith("http://ai-week.de/programm.php");
    }
}