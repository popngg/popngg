package gg.popn.http.song;

import gg.popn.application.song.port.in.FindChartDetailUseCase;
import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.http.common.response.SuccessResponse;
import gg.popn.http.song.response.ChartDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chart-details")
public class ChartDetailController {
    private final FindChartDetailUseCase findChartDetailUseCase;

    @GetMapping("/{chartId}")
    public SuccessResponse<ChartDetailResponse> findChart(@PathVariable long chartId) {
        return SuccessResponse.<ChartDetailResponse>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(ChartDetailResponse.from(findChartDetailUseCase.findChart(chartId)))
                .build();
    }
}
