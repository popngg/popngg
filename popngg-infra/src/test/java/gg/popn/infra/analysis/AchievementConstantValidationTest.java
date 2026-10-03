package gg.popn.infra.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.analysis.AchievementConstants;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AchievementConstantValidationTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final AchievementConstantJdbcAdapter adapter =
            new AchievementConstantJdbcAdapter(jdbc, new ObjectMapper());

    @Test void rejectsMissingAndNonFiniteExperimentalValuesBeforeReadingDatabase() {
        for (Double value : Arrays.asList(null, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            rejects(row(value, 49d, List.of(48d, 50d), "EXPERIMENTAL", List.of()));
            rejects(row(1d, value, List.of(48d, 50d), "EXPERIMENTAL", List.of()));
        }
    }

    @Test void rejectsMalformedExperimentalIntervalsAndReasonsBeforeReadingDatabase() {
        for (List<Double> interval : Arrays.<List<Double>>asList(null, List.of(), List.of(48d),
                List.of(48d, 49d, 50d), List.of(50d, 48d), List.of(Double.NaN, 50d),
                List.of(48d, Double.POSITIVE_INFINITY), Arrays.asList(null, 50d))) {
            rejects(row(1d, 49d, interval, "EXPERIMENTAL", List.of()));
        }
        rejects(row(1d, 49d, List.of(48d, 50d), "EXPERIMENTAL", List.of("INSUFFICIENT_PLAYERS")));
    }

    @Test void rejectsInconsistentHoldRowsBeforeReadingDatabase() {
        rejects(row(1d, 49d, null, "HOLD", List.of("INSUFFICIENT_PLAYERS")));
        rejects(row(1d, null, List.of(48d, 50d), "HOLD", List.of("INSUFFICIENT_PLAYERS")));
        rejects(row(1d, null, null, "HOLD", List.of()));
        rejects(row(1d, null, null, "HOLD", null));
        rejects(row(1d, null, null, "HOLD", List.of(" ")));
        rejects(row(1d, null, null, "HOLD", Collections.singletonList(null)));
        rejects(row(Double.NaN, null, null, "HOLD", List.of("INSUFFICIENT_PLAYERS")));
    }

    @Test void rejectsNullRowsTargetsAndImpossibleCountsBeforeReadingDatabase() {
        rejects(null);
        rejects(new AchievementConstants.ImportRow(1,"song",49,"medal",null,1d,49d,List.of(48d,50d),
                100,50,"EXPERIMENTAL",List.of()));
        rejects(new AchievementConstants.ImportRow(1,"song",49,"medal","CLEAR",1d,49d,List.of(48d,50d),
                -1,0,"EXPERIMENTAL",List.of()));
        rejects(new AchievementConstants.ImportRow(1,"song",49,"medal","CLEAR",1d,49d,List.of(48d,50d),
                100,101,"EXPERIMENTAL",List.of()));
    }

    private void rejects(AchievementConstants.ImportRow row) {
        var request = new AchievementConstants.Import("source:validation","achievement-v1","EXPERIMENTAL",
                Instant.parse("2026-09-25T00:00:00Z"), AchievementConstants.Axis.MEDAL,
                Collections.singletonList(row));
        assertThatThrownBy(() -> adapter.validateImport(request)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid achievement constant row");
        verifyNoInteractions(jdbc);
    }

    private static AchievementConstants.ImportRow row(Double raw, Double value, List<Double> interval,
                                                      String status, List<String> reasons) {
        return new AchievementConstants.ImportRow(1,"song",49,"medal","CLEAR",raw,value,interval,
                100,50,status,reasons);
    }
}
