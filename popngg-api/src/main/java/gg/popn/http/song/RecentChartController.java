package gg.popn.http.song;

import gg.popn.application.song.port.in.FindRecentSongsUseCase;
import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.http.common.response.PageResponse;
import gg.popn.http.common.response.SuccessResponse;
import gg.popn.http.song.response.RecentChartResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/charts")
public class RecentChartController {
    private final FindRecentSongsUseCase findRecentSongsUseCase;

    @GetMapping("/recent")
    public SuccessResponse<PageResponse<RecentChartResponse>> findRecentCharts(
            @RequestParam(defaultValue = "10") int limit) {
        var result = findRecentSongsUseCase.findRecent(limit);
        return SuccessResponse.<PageResponse<RecentChartResponse>>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(PageResponse.of(
                        result.content().stream().map(RecentChartResponse::from).toList(),
                        result.totalElements(), result.page(), result.size()))
                .build();
    }
}
