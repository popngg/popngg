package gg.popn.http.playdata;

import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.application.playdata.port.in.PlaydataQueryUseCase;
import gg.popn.application.song.port.in.FindSongDetailUseCase;
import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.domain.common.exception.ChartNotFoundException;
import gg.popn.http.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequiredArgsConstructor
public class ChartRankingController {
    private final FindSongDetailUseCase songDetailUseCase;
    private final PlaydataQueryUseCase playdataQueryUseCase;

    @GetMapping("/api/v1/charts/{songHash}/{difficulty}/rankings")
    public SuccessResponse<PlaydataQueryResults.ChartRankings> findRankings(
            @PathVariable String songHash,
            @PathVariable String difficulty,
            @RequestParam(defaultValue = "100") int limit
    ) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100.");
        }
        int difficultyCode = switch (difficulty.toLowerCase(Locale.ROOT)) {
            case "easy", "e" -> 1;
            case "normal", "n" -> 2;
            case "hyper", "h" -> 3;
            case "ex" -> 4;
            default -> throw new IllegalArgumentException("Unsupported difficulty.");
        };
        long chartId = songDetailUseCase.findSong(songHash).charts().stream()
                .filter(chart -> !chart.isDeleted() && chart.difficulty().code() == difficultyCode)
                .findFirst()
                .orElseThrow(ChartNotFoundException::new)
                .chartId();
        return SuccessResponse.<PlaydataQueryResults.ChartRankings>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(playdataQueryUseCase.findChartRankings(chartId, limit))
                .build();
    }
}
