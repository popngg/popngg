package gg.popn.infra.db.mapper;

import gg.popn.domain.chart.model.Chart;
import gg.popn.domain.chart.model.field.Difficulty;
import gg.popn.domain.chart.model.field.SongHash;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChartMapperTest {
    @Test
    void groupsChartsBySongHashAndDifficulty() {
        Chart light = chart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 1);
        Chart ex = chart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 4);
        Chart normal = chart("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", 2);

        var result = ChartMapper.toGroupedChart(List.of(light, ex, normal));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getLightChart()).isSameAs(light);
        assertThat(result.get(0).getExChart()).isSameAs(ex);
        assertThat(result.get(1).getNormalChart()).isSameAs(normal);
    }

    @Test
    void returnsEmptyListForNoCharts() {
        assertThat(ChartMapper.toGroupedChart(List.of())).isEmpty();
        assertThat(ChartMapper.toGroupedChart(null)).isEmpty();
    }

    private static Chart chart(String hash, int difficulty) {
        return Chart.builder()
                .songHash(SongHash.of(hash))
                .difficulty(Difficulty.of(difficulty))
                .build();
    }
}
