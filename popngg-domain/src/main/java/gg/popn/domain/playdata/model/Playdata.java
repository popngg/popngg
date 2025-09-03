package gg.popn.domain.playdata.model;

import gg.popn.domain.chart.model.Chart;
import gg.popn.domain.playdata.model.field.Medal;
import gg.popn.domain.playdata.model.field.Popclass;
import gg.popn.domain.playdata.model.field.Rank;
import gg.popn.domain.playdata.model.field.Score;
import gg.popn.domain.user.model.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Playdata {
    private Long id;
    private User user;
    private Chart chart;
    private Rank rank;
    private Medal medal;
    private Score score;
    private Popclass popclass;
}
