package gg.popn.http.playdata;

import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.application.playdata.port.in.PlaydataQueryUseCase;
import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.http.common.response.SuccessResponse;
import gg.popn.http.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ChartRankingController {
    private final PlaydataQueryUseCase queryUseCase;

    @GetMapping("/api/v1/charts/{songHash}/{difficulty}/rankings")
    public SuccessResponse<PageResponse<PlaydataQueryResults.ChartRankingEntry>> findChartRankings(
            @PathVariable String songHash,
            @PathVariable int difficulty,
            @RequestParam(defaultValue = "score") String axis,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PlaydataQueryResults.ChartRankingsPage result;
        try {
            result = queryUseCase.findChartRankings(songHash, difficulty, axis, page, size);
        } catch (IllegalArgumentException exception) {
            throw new gg.popn.domain.common.exception.InvalidArgumentException("rankings", exception.getMessage());
        }
        return SuccessResponse.<PageResponse<PlaydataQueryResults.ChartRankingEntry>>builder()
                .code(ResponseCode.SUCCESS).message(ResponseMessage.SUCCESS)
                .data(PageResponse.of(result.items(), result.totalItems(), page - 1, size)).build();
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public org.springframework.http.ResponseEntity<java.util.Map<String, String>> invalidParameterType() {
        return org.springframework.http.ResponseEntity.badRequest().body(
                java.util.Map.of("code", "BAD_REQUEST", "message", "Invalid ranking parameter."));
    }
}
