package gg.popn.controller.chart;

import gg.popn.domain.chart.model.field.Difficulty;
import gg.popn.domain.chart.model.field.SongHash;
import gg.popn.model.response.SuccessResponse;
import gg.popn.domain.chart.application.dto.ChartDto;
import gg.popn.domain.chart.application.dto.GroupedChartDto;
import gg.popn.domain.chart.application.dto.request.GetChartRequest;
import gg.popn.domain.chart.application.dto.response.GroupedChartsDto;
import gg.popn.domain.chart.application.port.in.GetChartUseCase;
import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @PostMapping("/all")
    public SuccessResponse<GroupedChartsDto> getCharts(@RequestBody GetChartRequest request) {
        return SuccessResponse.<GroupedChartsDto>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(null) // TODO: implement
                .build();
    }

    @GetMapping("/{songHash}")
    public SuccessResponse<GroupedChartDto> getChartBySongHash(
            @Parameter(description = "songHash of the chart", schema = @Schema(type = "string", example = "2302440c63cbe103703f3de51ac205da"))
            @PathVariable SongHash songHash) {
        return SuccessResponse.<GroupedChartDto>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(null) // TODO: implement
                .build();
    }

    @GetMapping("/{songHash}/{difficulty}")
    public SuccessResponse<ChartDto> getChartBySongHashAndDifficulty(
            @Parameter(description = "songHash of the chart", schema = @Schema(type = "string", example = "2302440c63cbe103703f3de51ac205da"))
            @PathVariable SongHash songHash,
            @Parameter(description = "Difficulty of the chart (1~4)", schema = @Schema(type = "integer", example = "4"))
            @PathVariable Difficulty difficulty) {
        return SuccessResponse.<ChartDto>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(getChartUseCase.getChartBySongHashAndDifficulty(songHash, difficulty))
                .build();
    }

    @GetMapping("/recent")
    public SuccessResponse<GroupedChartsDto> getRecentCharts() {
        return SuccessResponse.<GroupedChartsDto>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(null) // TODO: implement
                .build();
    }
}
