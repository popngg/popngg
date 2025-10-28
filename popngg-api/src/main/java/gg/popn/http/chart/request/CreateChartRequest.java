package gg.popn.http.chart.request;

import gg.popn.domain.chart.model.field.*;
import lombok.*;

import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChartRequest {
    GenreName genreName;
    SongName songName;
    List<Level> levels;
    Version version;
    IsUpper isUpper;
}
