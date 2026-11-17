package gg.popn.http.analysis;

import gg.popn.application.tier.AchievementBoard;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AchievementBoardPngRendererTest {
    @Test void rendersSortedBannerCardsAndSupportsPagination() throws Exception {
        var loader = mock(TierListImageRenderer.class);
        var jacket = new java.awt.image.BufferedImage(250, 66, 1);
        var graphics = jacket.createGraphics();
        graphics.setColor(new java.awt.Color(75,123,229)); graphics.fillRect(0,0,250,66);
        graphics.setColor(java.awt.Color.WHITE); graphics.drawString("TEST BANNER", 65, 38); graphics.dispose();
        when(loader.loadAchievementJackets(anyList())).thenReturn(Map.of("fixture", jacket));
        var titles = List.of("Revelatune", "neu", "한글 곡명 테스트", "量子の海のリントヴルム", "Line Times", "Übertreffen");
        var charts = IntStream.range(0, 81).mapToObj(i -> new AchievementBoard.Chart(i + 1,
                titles.get(i % titles.size()), "장르 / GENRE", "fixture", i % 2 == 0 ? "EX" : "HYPER", i % 3 == 0,
                300 - i * 3, i % 5)).toList();
        var board = new AchievementBoard(48, AchievementBoard.Metric.S, Instant.parse("2026-10-19T00:00:00Z"), charts);
        assertThat(board.pages()).isEqualTo(2);
        var renderer = new AchievementBoardPngRenderer(loader);
        byte[] png = renderer.render(board, 1);
        var image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image.getWidth()).isEqualTo(1160);
        assertThat(image.getHeight()).isEqualTo(2188);
        assertThat(png.length).isLessThan(9_500_000);
        assertThat(ImageIO.read(new ByteArrayInputStream(renderer.render(board, 2))).getHeight()).isEqualTo(343);
        assertThatThrownBy(() -> renderer.render(board, 3)).isInstanceOf(IllegalArgumentException.class);
        var perfect = new AchievementBoard(40, AchievementBoard.Metric.PERFECT, board.asOf(), charts);
        assertThat(perfect.charts().getFirst().perfectCount()).isEqualTo(4);
        assertThat(perfect.charts().getFirst().chartId()).isEqualTo(5);
        var preview = new AchievementBoard(48, board.metric(), board.asOf(), charts.subList(0, 12));
        var output = Path.of("build", "previews", "achievement-board.png");
        Files.createDirectories(output.getParent());
        Files.write(output, renderer.render(preview, 1));
        when(loader.loadAchievementJackets(anyList())).thenReturn(Map.of());
        assertThat(renderer.render(perfect, 2)).isNotEmpty();
        assertThat(ImageIO.read(new ByteArrayInputStream(renderer.render(
                new AchievementBoard(50, board.metric(), board.asOf(), List.of()), 1)))).isNotNull();
    }

    @Test void sharesTierJacketLoaderAndSkipsMissingImages() throws Exception {
        var client = mock(java.net.http.HttpClient.class);
        var response = mock(java.net.http.HttpResponse.class);
        var output = new java.io.ByteArrayOutputStream();
        ImageIO.write(new java.awt.image.BufferedImage(250,66,1), "png", output);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(output.toByteArray());
        when(client.send(any(java.net.http.HttpRequest.class), any(java.net.http.HttpResponse.BodyHandler.class))).thenReturn(response);
        var renderer = new TierListImageRenderer(client);
        try {
            String url = "https://static.popn.gg/" + "a".repeat(64) + ".png";
            assertThat(renderer.loadAchievementJackets(java.util.Arrays.asList(url,url,null,"invalid"))).containsOnlyKeys(url);
        } finally { renderer.shutdown(); }
    }
}

