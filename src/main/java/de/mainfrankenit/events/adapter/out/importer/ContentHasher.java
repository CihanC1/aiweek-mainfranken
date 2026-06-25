package de.mainfrankenit.events.adapter.out.importer;
import jakarta.enterprise.context.ApplicationScoped;
import org.jsoup.Jsoup;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
@ApplicationScoped
public class ContentHasher {
    public String hash(String html) {
        try {
            var doc = Jsoup.parse(html == null ? "" : html);
            doc.select("script,style,noscript,svg,iframe").remove();
            var normalized = doc.text().toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ").trim();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
