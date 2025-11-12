package gg.popn.application.chart.service;

import gg.popn.application.chart.port.in.CreateChartUseCase;
import gg.popn.application.chart.port.in.command.CreateChartCommand;
import gg.popn.application.chart.port.out.ChartCommandPort;
import gg.popn.application.chart.port.out.ChartQueryPort;
import gg.popn.domain.chart.model.Chart;
import gg.popn.domain.chart.model.field.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateChartService implements CreateChartUseCase {
    private final ChartQueryPort chartQuery;
    private final ChartCommandPort chartCommand;


    @Override
    public Integer createChart(CreateChartCommand command) throws Exception {
        int cnt = 0;

        GenreName genreName = command.genreName();
        SongName songName = command.songName();
        Version version = command.version();
        IsUpper isUpper = command.isUpper();
        List<Level> levels = command.levels();
        SongHash hash = SongHash.from(genreName, songName, version, isUpper);

        for (int idx = 0; idx < 4; idx++) {
            Difficulty difficulty = Difficulty.builder()
                            .difficulty(idx+1)
                            .build();
            Level level = levels.get(idx);
            if (level.getValue() == 0) continue;
            List<Chart> dupCheck = chartQuery.getChartsBySongHashAndDifficulty(hash, difficulty);
            if (!dupCheck.isEmpty()) {
                throw new Exception("Song hash and difficulty is duplicated : " +
                        dupCheck.get(0).toString() + ". " +
                        "Check whether the chart has been in database. ");
            }

            Chart chart = Chart.builder()
                    .genreName(genreName)
                    .songName(songName)
                    .difficulty(difficulty)
                    .level(level)
                    .version(version)
                    .isUpper(isUpper)
                    .songHash(hash)
                    .build();

            chartCommand.save(chart);
            cnt++;
        }

        return cnt;
    }
}
