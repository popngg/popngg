package gg.popn.application.song.port.in;

import gg.popn.application.song.dto.result.ChartDetailView;

public interface FindChartDetailUseCase {
    ChartDetailView findChart(long chartId);
}
