package gg.popn.http.chart;

import gg.popn.application.chart.dto.command.CreateChartCommand;
import gg.popn.application.chart.port.in.CreateChartUseCase;
import gg.popn.http.chart.mapper.ChartAssembler;
import gg.popn.http.chart.request.CreateChartRequest;
import gg.popn.http.chart.response.CreateChartResponse;
import gg.popn.http.common.response.SuccessResponse;
import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/chart")
public class ChartController {
    private final CreateChartUseCase createChartUseCase;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("")
    public SuccessResponse<CreateChartResponse> addChart(@RequestBody CreateChartRequest request) throws Exception {
        CreateChartCommand cmd = ChartAssembler.toCreateCommand(request);
        CreateChartResponse response = CreateChartResponse.from(createChartUseCase.execute(cmd));

        return SuccessResponse.<CreateChartResponse>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(response)
                .build();
    }
}
