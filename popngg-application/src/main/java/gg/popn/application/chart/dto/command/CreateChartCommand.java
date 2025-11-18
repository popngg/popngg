package gg.popn.application.chart.dto.command;

import gg.popn.domain.chart.model.field.*;
import lombok.Builder;

import java.util.List;

@Builder
public record CreateChartCommand(GenreName genreName, SongName songName, List<Level> levels, Version version,
                                 IsUpper isUpper) {

}