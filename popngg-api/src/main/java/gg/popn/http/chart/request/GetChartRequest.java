package gg.popn.http.chart.request;

import gg.popn.domain.chart.model.field.Difficulty;
import gg.popn.domain.chart.model.field.Level;
import gg.popn.domain.chart.model.field.Version;
import lombok.Value;

import java.util.List;

@Value
public class GetChartRequest {
    Version version;
    List<Difficulty> difficulties;
    Level minLevel;
    Level maxLevel;
    String keyword;
    String sortBy;
    String sortOrder;
}
