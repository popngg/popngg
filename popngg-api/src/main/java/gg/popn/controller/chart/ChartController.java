package gg.popn.controller.chart;

import gg.popn.controller.model.response.SuccessResponse;
import gg.popn.domain.chart.application.dto.ChartDto;
import gg.popn.domain.chart.application.dto.GetChartResponse;
import gg.popn.domain.chart.application.port.in.GetChartUseCase;
import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/chart")
@Tag(name = "Chart", description = "Chart operations")
public class ChartController {
    private final GetChartUseCase getChartUseCase;

    @GetMapping("/level/{level}")
    public SuccessResponse<GetChartResponse> getChartsByLevel(@PathVariable Integer level) {
        return SuccessResponse.<GetChartResponse>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(getChartUseCase.getChartsByLevel(level))
                .build();
    }

    @GetMapping("/{songHash}/{difficulty}")
    public SuccessResponse<ChartDto> getChartBySongHashAndDifficulty(@PathVariable String songHash, @PathVariable Integer difficulty) {
        return SuccessResponse.<ChartDto>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(getChartUseCase.getChartBySongHashAndDifficulty(songHash, difficulty))
                .build();
    }
}
