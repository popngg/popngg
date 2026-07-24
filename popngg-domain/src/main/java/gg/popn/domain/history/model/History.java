package gg.popn.domain.history.model;

import gg.popn.domain.chart.model.Chart;
import gg.popn.domain.history.model.field.Medal;
import gg.popn.domain.history.model.field.Popclass;
import gg.popn.domain.history.model.field.Rank;
import gg.popn.domain.history.model.field.Score;
import gg.popn.domain.user.model.User;
import lombok.Builder;
import lombok.Getter;

import java.util.Date;

@Getter
@Builder
public class History {
    private Long id;
    private User user;
    private Chart chart;
    private Rank rank;
    private Medal medal;
    private Score score;
    private Popclass popclass;
    private Date createdAt;
}
