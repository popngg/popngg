package gg.popn.domain.chart.application.dto.request;

import gg.popn.domain.chart.model.field.GenreName;
import gg.popn.domain.chart.model.field.IsUpper;
import gg.popn.domain.chart.model.field.SongName;
import gg.popn.domain.chart.model.field.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostChartRequest {

    GenreName genreName;
    SongName songName;
    List<Integer> levels;
    Version version;
    IsUpper isUpper;
}
