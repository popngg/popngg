package gg.popn.http.chart.mapper;

import gg.popn.application.chart.port.in.command.CreateChartCommand;
import gg.popn.http.chart.request.CreateChartRequest;

public final class CreateChartAssembler {
    private CreateChartAssembler() {}

    public static CreateChartCommand toCommand(CreateChartRequest req) {
        return new CreateChartCommand(
                req.getGenreName(),
                req.getSongName(),
                req.getLevels(),
                req.getVersion(),
                req.getIsUpper()
        );
    }


}