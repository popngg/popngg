package gg.popn.infra.catalog;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class OfficialSongListClientTest {
    private static final String ROW = """
            <ul class="mu_list_table mu_head"><li>header</li></ul>
            <ul class="mu_list_table"><li><img src="ignored"></li>
            <li><p>ジャンル(UPPER)</p><p>Song &amp; Test(UPPER)</p><p>Artist</p></li>
            <li><p data-d=0><span>LIGHT</span>-</p><p data-d=25><span>NORMAL</span>25</p>
            <p data-d=40><span>HYPER</span>40</p><p data-d=49><span>EX</span>49</p></li></ul>
            """;

    @Test void parsesUpperEntitiesAndMissingChartsWithoutReadingImages() {
        var songs = OfficialSongListClient.parse(Jsoup.parse(ROW), 29, OfficialSongListClient.BASE);
        assertThat(songs).singleElement().satisfies(song -> {
            assertThat(song.title()).isEqualTo("Song & Test");
            assertThat(song.genre()).isEqualTo("ジャンル");
            assertThat(song.upper()).isTrue();
            assertThat(song.charts()).extracting(c -> c.difficulty()).containsExactly(2, 3, 4);
            assertThat(song.charts()).extracting(c -> c.level()).containsExactly(25, 40, 49);
        });
    }

    @Test void refusesIncompleteOrUnexpectedOfficialPages() {
        assertThatThrownBy(() -> OfficialSongListClient.parse(Jsoup.parse("<h1>Log in</h1>"), 29, "source"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> OfficialSongListClient.parse(Jsoup.parse(ROW.replace("<span>EX</span>49", "<span>EX</span>51")), 29, "source"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> OfficialSongListClient.parse(Jsoup.parse(ROW.replace("NORMAL", "UNKNOWN")), 29, "source"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> OfficialSongListClient.parse(Jsoup.parse(ROW.replace("<p>Artist</p>", "")), 29, "source"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void cachesOnlyCompleteSnapshotsAndLeavesAmbiguousVariantsForReview() {
        AtomicInteger calls = new AtomicInteger();
        StringBuilder versions = new StringBuilder("<select name=version>");
        for (int v = 0; v < 30; v++) versions.append("<option value=").append(v).append(">v</option>");
        versions.append("</select>");
        var client = new OfficialSongListClient(url -> {
            calls.incrementAndGet();
            if (url.equals(OfficialSongListClient.BASE)) return Jsoup.parse(versions.toString());
            return Jsoup.parse(ROW + "<select name=page_sl><option value=0>1</option></select>");
        });
        assertThat(client.find("Song & Test", "Artist", true)).hasSize(30);
        assertThat(client.find("Song & Test", "Artist", false)).isEmpty();
        assertThat(client.find("Song & Test", "Another artist", true)).isEmpty();
        assertThat(client.find("Song & Test", "", true)).isEmpty();
        assertThat(calls.get()).isEqualTo(31);
        var broken = new OfficialSongListClient(url -> Jsoup.parse("unavailable"));
        assertThatThrownBy(() -> broken.find("song", "artist", false)).isInstanceOf(IllegalStateException.class);
    }

    @Test @EnabledIfEnvironmentVariable(named="POPNGG_LIVE_OFFICIAL_TEST", matches="true")
    void readsCurrentPublicOfficialCatalog() {
        var matches = new OfficialSongListClient().find("Airplane", "Red Planets", false);
        assertThat(matches).singleElement().satisfies(song -> {
            assertThat(song.genre()).isEqualTo("ワールドオルタナティブ");
            assertThat(song.version()).isEqualTo(27);
            assertThat(song.charts()).hasSize(4);
        });
    }
}
