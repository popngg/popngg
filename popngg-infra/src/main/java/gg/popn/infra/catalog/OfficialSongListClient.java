package gg.popn.infra.catalog;

import gg.popn.application.song.port.out.OfficialSongSource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class OfficialSongListClient implements OfficialSongSource {
    static final String BASE = "https://p.eagate.573.jp/game/popn/popn29/music/list.html";
    private final PageReader reader;
    private List<Song> cached = List.of();
    private Instant expires = Instant.MIN;

    public OfficialSongListClient() {
        reader = url -> {
            var response = Jsoup.connect(url).timeout(6_000).maxBodySize(2_000_001)
                    .followRedirects(false).execute();
            if (response.statusCode() != 200) throw new IllegalStateException("Official list unavailable");
            if (response.bodyAsBytes().length > 2_000_000) throw new IllegalStateException("Official page too large");
            return response.parse();
        };
    }

    OfficialSongListClient(PageReader reader) { this.reader = reader; }

    @Override
    public synchronized List<Song> find(String title, String artist, Boolean upper) {
        if (title == null || title.isBlank() || artist == null || artist.isBlank()) return List.of();
        if (Instant.now().isAfter(expires)) {
            // Publish only a complete snapshot. A login/error page must never become an empty catalog.
            List<Song> snapshot = load();
            cached = List.copyOf(snapshot);
            expires = Instant.now().plusSeconds(900);
        }
        return cached.stream().filter(song -> baseTitle(song.title()).equals(baseTitle(title))
                && song.artist().equals(artist.strip()) && (upper == null || song.upper() == upper)).toList();
    }

    private List<Song> load() {
        Instant deadline = Instant.now().plusSeconds(120);
        try {
            Document home = reader.read(BASE);
            var options = home.select("select[name=version] option");
            if (options.isEmpty()) throw new IllegalStateException("Missing version selector");
            List<Integer> versions = options.stream().map(e -> Integer.parseInt(e.attr("value")))
                    .filter(v -> v >= 0).distinct().toList();
            if (versions.size() < 29 || versions.size() > 50) throw new IllegalStateException("Unexpected versions");
            List<Song> songs = new ArrayList<>();
            int requests = 0;
            for (int version : versions) {
                String url = BASE + "?version=" + version;
                Document first = reader.read(url);
                var pages = first.select("select[name=page_sl] option");
                if (pages.isEmpty() || pages.size() > 20) throw new IllegalStateException("Missing pagination");
                for (int page = 0; page < pages.size(); page++) {
                    if (++requests > 150 || Instant.now().isAfter(deadline))
                        throw new IllegalStateException("Official catalog request limit");
                    if (!pages.get(page).attr("value").equals(Integer.toString(page)))
                        throw new IllegalStateException("Unexpected pagination");
                    songs.addAll(parse(page == 0 ? first : reader.read(url + "&page=" + page), version, url));
                }
            }
            return songs;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Official lookup interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("공식 목록을 읽지 못했습니다. 잠시 후 다시 시도하거나 `/곡추가`를 이용해 주세요.", exception);
        }
    }

    static List<Song> parse(Document document, int version, String url) {
        Element table = document.selectFirst("ul.mu_list_table:not(.mu_head)");
        if (table == null || table.childrenSize() == 0 || table.childrenSize() % 3 != 0)
            throw new IllegalStateException("Invalid music table");
        List<Song> songs = new ArrayList<>();
        for (int i = 0; i < table.childrenSize(); i += 3) {
            var metadata = table.child(i + 1).children();
            var levels = table.child(i + 2).children();
            if (metadata.size() != 3 || levels.size() != 4) throw new IllegalStateException("Invalid music row");
            String genre = metadata.get(0).text().strip(), title = metadata.get(1).text().strip();
            String artist = metadata.get(2).text().strip();
            if (genre.isBlank() || title.isBlank() || artist.isBlank()) throw new IllegalStateException("Missing metadata");
            boolean upper = hasUpper(title) || hasUpper(genre);
            List<Chart> charts = new ArrayList<>();
            List<String> labels = List.of("LIGHT", "NORMAL", "HYPER", "EX");
            for (int d = 0; d < 4; d++) {
                Element level = levels.get(d);
                if (!level.select("span").text().equals(labels.get(d))) throw new IllegalStateException("Unknown difficulty");
                String value = level.ownText().strip();
                // Missing charts are shown as '-' on the official list. Never turn them into level zero.
                if (value.equals("-") || value.equals("―") || value.equals("－")) continue;
                int number = Integer.parseInt(value);
                if (number < 1 || number > 50) throw new IllegalStateException("Invalid level");
                charts.add(new Chart(d + 1, number));
            }
            if (charts.isEmpty()) throw new IllegalStateException("No playable charts");
            songs.add(new Song(baseTitle(title), baseTitle(genre), artist, version, upper, List.copyOf(charts), url));
        }
        return songs;
    }

    static boolean hasUpper(String value) {
        return value.toUpperCase(Locale.ROOT).matches(".*[（(]UPPER[）)]$");
    }
    static String baseTitle(String value) { return value.replaceFirst("(?i)\\s*[（(]UPPER[）)]$", "").strip(); }
    @FunctionalInterface interface PageReader { Document read(String url) throws Exception; }
}
