package gg.popn.application.playdata.dto.request;

import gg.popn.domain.chart.model.field.Difficulty;
import gg.popn.domain.chart.model.field.SongHash;
import gg.popn.domain.playdata.model.field.Popclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetMainPlaydataRequest {
    private SongHash songHash;
    private Difficulty difficulty;
    private Popclass minPopclass;
    private Popclass maxPopclass;
    private RankingSortByOption sortBy;
}
