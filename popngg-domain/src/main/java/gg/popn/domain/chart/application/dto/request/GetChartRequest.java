package gg.popn.domain.chart.application.dto.request;

import lombok.Value;

import java.util.List;

@Value
public class GetChartRequest {
    Integer version;
    List<Integer> difficulties;
    Integer minLevel;
    Integer maxLevel;
    String keyword;
    String sortBy;
    String sortOrder;
}
