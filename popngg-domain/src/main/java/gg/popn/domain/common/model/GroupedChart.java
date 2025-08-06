package gg.popn.domain.common.model;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class GroupedChart {
    Chart lightChart;
    Chart normalChart;
    Chart hyperChart;
    Chart exChart;
}
