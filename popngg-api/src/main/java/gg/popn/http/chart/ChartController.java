package gg.popn.http.chart;

import gg.popn.application.chart.dto.command.CreateChartCommand;
import gg.popn.application.chart.dto.command.FindChartCommand;
import gg.popn.application.chart.dto.command.FindGroupedChartCommand;
import gg.popn.application.chart.port.in.*;
import gg.popn.http.chart.mapper.ChartAssembler;
import gg.popn.http.chart.request.CreateChartRequest;
import gg.popn.http.chart.response.ChartResponse;
import gg.popn.http.chart.response.CreateChartResponse;
import gg.popn.http.chart.response.GroupedChartListResponse;
import gg.popn.http.chart.response.GroupedChartResponse;
import gg.popn.http.common.response.SuccessResponse;
import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/chart")
@Tag(name = "Chart", description = "Chart operations")
public class ChartController {
    private final FindGroupedChartListUseCase findGroupedChartListUseCase;
    private final FindGroupedChartListRecentUseCase findGroupedChartListRecentUseCase;
    private final FindChartUseCase findChartUseCase;
    private final CreateChartUseCase createChartUseCase;
    private final FindGroupedChartUseCase findGroupedChartUseCase;

    @GetMapping("/all")
    public SuccessResponse<GroupedChartListResponse> getCharts() {

        GroupedChartListResponse response = GroupedChartListResponse.from(findGroupedChartListUseCase.execute());
        return SuccessResponse.<GroupedChartListResponse>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(response)
                .build();
    }

    @GetMapping("/{songHash}")
    public SuccessResponse<GroupedChartResponse> findGroupedChart(
            @Parameter(description = "songHash of the chart", schema = @Schema(type = "string", example = "2302440c63cbe103703f3de51ac205da"))
            @PathVariable String songHash) {
        FindGroupedChartCommand cmd = ChartAssembler.toFindGroupCommand(songHash);
        GroupedChartResponse response = GroupedChartResponse.from(findGroupedChartUseCase.execute(cmd));

        return SuccessResponse.<GroupedChartResponse>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(response)
                .build();
    }

    @GetMapping("/{songHash}/{difficulty}")
    public SuccessResponse<ChartResponse> findChart(
            @Parameter(description = "songHash of the chart", schema = @Schema(type = "string", example = "2302440c63cbe103703f3de51ac205da"))
            @PathVariable String songHash,
            @Parameter(description = "Difficulty of the chart (1~4)", schema = @Schema(type = "integer", example = "4"))
            @PathVariable Integer difficulty) {

        FindChartCommand cmd = ChartAssembler.toFindCommand(songHash,difficulty);
        ChartResponse response = ChartResponse.from(findChartUseCase.execute(cmd));

        return SuccessResponse.<ChartResponse>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(response)
                .build();
    }

    @GetMapping("/recent")
    public SuccessResponse<GroupedChartListResponse> findRecentCharts() {

        GroupedChartListResponse response = GroupedChartListResponse.from(findGroupedChartListRecentUseCase.execute());

        return SuccessResponse.<GroupedChartListResponse>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(response)
                .build();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
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
