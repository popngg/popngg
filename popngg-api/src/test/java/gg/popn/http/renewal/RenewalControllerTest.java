package gg.popn.http.renewal;

import gg.popn.application.playdata.dto.result.ImportPlaydataResult;
import gg.popn.application.playdata.port.in.ImportPlaydataUseCase;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import gg.popn.infra.security.CustomUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RenewalControllerTest {
    private final ImportPlaydataUseCase useCase = mock(ImportPlaydataUseCase.class);
    private final RenewalController controller = new RenewalController(useCase);

    @Test
    void mapsHandoffPayloadToExistingImport() {
        ReflectionTestUtils.setField(controller, "collectorVersion", 1);
        ReflectionTestUtils.setField(controller, "supportedGame", "popn29");
        var chart = new RenewalRequest.Chart();
        chart.setChartId("42"); chart.setTitle("song"); chart.setGenre("genre"); chart.setArtist("artist");
        chart.setDifficulty("ex"); chart.setMedal("c");
        chart.setRank("s"); chart.setScore(99000); chart.setVersionBestScore(98000);
        var request = new RenewalRequest(1, "popn29", Instant.now(),
                new RenewalRequest.Profile("1234-5678-9012", "name", "character", "170.13"),
                List.of(chart), List.of(), new RenewalRequest.Stats(1,1,1,1,1,1,100));
        when(useCase.importPlaydata(argThat(command -> command.rows().getFirst().chartId() == 42
                && command.rows().getFirst().versionBestScore() == 98000
                && command.rows().getFirst().artistName().equals("artist"))))
                .thenReturn(new ImportPlaydataResult(7,1,1,1,1,0,List.of()));

        var response = controller.renew(principal(), request);

        assertThat(response.getData().renewLogId()).isEqualTo(7);
    }

    @Test
    void mapsNoneMedalToNoMedalCode() {
        ReflectionTestUtils.setField(controller, "collectorVersion", 1);
        ReflectionTestUtils.setField(controller, "supportedGame", "popn29");
        var chart = new RenewalRequest.Chart();
        chart.setChartId("42"); chart.setTitle("song"); chart.setGenre("genre");
        chart.setDifficulty("light"); chart.setMedal("none");
        chart.setRank("e"); chart.setScore(12345);
        var request = new RenewalRequest(1, "popn29", Instant.now(),
                new RenewalRequest.Profile("1234-5678-9012", "name", null, null),
                List.of(chart), List.of(), new RenewalRequest.Stats(1,1,1,1,1,1,100));
        when(useCase.importPlaydata(argThat(command -> command.rows().getFirst().medalCode() == 13
                && command.rows().getFirst().score() == 12345)))
                .thenReturn(new ImportPlaydataResult(8,1,1,1,1,0,List.of()));

        var response = controller.renew(principal(), request);

        assertThat(response.getData().renewLogId()).isEqualTo(8);
    }

    @ParameterizedTest
    @CsvSource({
            "a, 1", "b, 2", "c, 3", "d, 4", "e, 5", "f, 6", "g, 7",
            "h, 8", "i, 9", "j, 10", "k, 11", "l, 12", "none, 13"
    })
    void mapsCurrentMedalNames(String medal, int expectedCode) {
        ReflectionTestUtils.setField(controller, "collectorVersion", 1);
        ReflectionTestUtils.setField(controller, "supportedGame", "popn29");
        var chart = new RenewalRequest.Chart();
        chart.setChartId("42"); chart.setTitle("song"); chart.setGenre("genre");
        chart.setDifficulty("ex"); chart.setMedal(medal);
        chart.setRank("s"); chart.setScore(90000);
        var request = new RenewalRequest(1, "popn29", Instant.now(),
                new RenewalRequest.Profile("1234-5678-9012", "name", null, null),
                List.of(chart), List.of(), new RenewalRequest.Stats(1,1,1,1,1,1,100));
        when(useCase.importPlaydata(argThat(command ->
                command.rows().getFirst().medalCode() == expectedCode)))
                .thenReturn(new ImportPlaydataResult(11,1,1,1,1,0,List.of()));

        var response = controller.renew(principal(), request);

        assertThat(response.getData().renewLogId()).isEqualTo(11);
    }

    @Test
    void mapsNoneRankToNoRankCode() {
        ReflectionTestUtils.setField(controller, "collectorVersion", 1);
        ReflectionTestUtils.setField(controller, "supportedGame", "popn29");
        var chart = new RenewalRequest.Chart();
        chart.setChartId("42"); chart.setTitle("song"); chart.setGenre("genre");
        chart.setDifficulty("light"); chart.setMedal("c");
        chart.setRank("none"); chart.setScore(12345);
        var request = new RenewalRequest(1, "popn29", Instant.now(),
                new RenewalRequest.Profile("1234-5678-9012", "name", null, null),
                List.of(chart), List.of(), new RenewalRequest.Stats(1,1,1,1,1,1,100));
        when(useCase.importPlaydata(argThat(command -> command.rows().getFirst().rankCode() == 13
                && command.rows().getFirst().score() == 12345)))
                .thenReturn(new ImportPlaydataResult(9,1,1,1,1,0,List.of()));

        var response = controller.renew(principal(), request);

        assertThat(response.getData().renewLogId()).isEqualTo(9);
    }

    @ParameterizedTest
    @CsvSource({
            "s_plus, 1", "s, 2", "aaa, 3", "aa_plus, 4", "aa, 5",
            "a_plus, 6", "a, 7", "b_plus, 8", "b, 9", "c, 10",
            "d, 11", "e, 12", "none, 13",
            "a3, 3", "a2_plus, 4", "a2, 5", "a1_plus, 6", "a1, 7"
    })
    void mapsCurrentAndLegacyRankNames(String rank, int expectedCode) {
        ReflectionTestUtils.setField(controller, "collectorVersion", 1);
        ReflectionTestUtils.setField(controller, "supportedGame", "popn29");
        var chart = new RenewalRequest.Chart();
        chart.setChartId("42"); chart.setTitle("song"); chart.setGenre("genre");
        chart.setDifficulty("ex"); chart.setMedal("c");
        chart.setRank(rank); chart.setScore(90000);
        var request = new RenewalRequest(1, "popn29", Instant.now(),
                new RenewalRequest.Profile("1234-5678-9012", "name", null, null),
                List.of(chart), List.of(), new RenewalRequest.Stats(1,1,1,1,1,1,100));
        when(useCase.importPlaydata(argThat(command ->
                command.rows().getFirst().rankCode() == expectedCode)))
                .thenReturn(new ImportPlaydataResult(10,1,1,1,1,0,List.of()));

        var response = controller.renew(principal(), request);

        assertThat(response.getData().renewLogId()).isEqualTo(10);
    }

    @Test
    void stripsUpperSuffixAndMatchesTheUpperChart() {
        ReflectionTestUtils.setField(controller, "collectorVersion", 1);
        ReflectionTestUtils.setField(controller, "supportedGame", "popn29");
        var chart = new RenewalRequest.Chart();
        chart.setChartId("Bphwoc7OmreNwltHB5NYZA==");
        chart.setTitle("Fate No.23(UPPER)"); chart.setGenre("レヴェラチューン(UPPER)");
        chart.setArtist("PON feat.秋成"); chart.setDifficulty("ex");
        chart.setMedal("g"); chart.setRank("a1"); chart.setScore(84771);
        var request = new RenewalRequest(1, "popn29", Instant.now(),
                new RenewalRequest.Profile("1234-5678-9012", "name", null, null),
                List.of(chart), List.of(), new RenewalRequest.Stats(1,1,1,1,1,1,100));
        when(useCase.importPlaydata(argThat(command -> {
            var row = command.rows().getFirst();
            return row.chartId() == null && row.upper()
                    && row.songName().equals("Fate No.23")
                    && row.genreName().equals("レヴェラチューン")
                    && row.artistName().equals("PON feat.秋成");
        }))).thenReturn(new ImportPlaydataResult(10,1,1,1,1,0,List.of()));

        var response = controller.renew(principal(), request);

        assertThat(response.getData().renewLogId()).isEqualTo(10);
    }

    @Test
    void rejectsDifferentGameId() {
        ReflectionTestUtils.setField(controller, "collectorVersion", 1);
        ReflectionTestUtils.setField(controller, "supportedGame", "popn29");
        var request = new RenewalRequest(1,"popn29",Instant.now(),
                new RenewalRequest.Profile("9999-9999-9999",null,null,null),List.of(),List.of(),
                new RenewalRequest.Stats(0,0,0,0,0,0,0));
        assertThatThrownBy(() -> controller.renew(principal(), request))
                .isInstanceOf(RenewalException.class)
                .extracting("code").isEqualTo("GAME_ID_MISMATCH");
    }

    private static CustomUserPrincipal principal() {
        return new CustomUserPrincipal(PoptomoId.of("1234-5678-9012"), UserRole.of("USER"));
    }
}
