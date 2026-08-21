package gg.popn.http.playdata;

import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.application.playdata.port.in.PlaydataQueryUseCase;
import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.http.common.response.SuccessResponse;
import gg.popn.http.playdata.response.PopclassTargetResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Playdata", description = "Playdata query operations")
public class PlaydataController {
    private final PlaydataQueryUseCase queryUseCase;

    @GetMapping("/api/v1/users/{poptomoId}/playdata")
    public SuccessResponse<PlaydataQueryResults.UserPlaydata> findUserPlaydata(
            @PathVariable String poptomoId
    ) {
        return success(queryUseCase.findUserPlaydata(poptomoId));
    }

    @GetMapping("/api/v1/users/{poptomoId}/playdata/counts")
    public SuccessResponse<PlaydataQueryResults.Counts> count(
            @PathVariable String poptomoId,
            @RequestParam String groupBy,
            @RequestParam String target
    ) {
        return success(queryUseCase.count(poptomoId, groupBy, target));
    }

    @GetMapping("/api/v1/users/{poptomoId}/popn-class-targets/current")
    public SuccessResponse<PopclassTargetResponse.CurrentTargets> findCurrentPopclassTargets(
            @PathVariable String poptomoId
    ) {
        return success(PopclassTargetResponse.CurrentTargets.from(
                queryUseCase.findPopclass(poptomoId)));
    }

    @GetMapping("/api/v1/users/{poptomoId}/popn-class-targets/legacy")
    public SuccessResponse<List<PopclassTargetResponse>> findLegacyPopclassTargets(
            @PathVariable String poptomoId
    ) {
        return success(queryUseCase.findLegacyPopclassTargets(poptomoId).stream()
                .map(PopclassTargetResponse::legacy)
                .toList());
    }

    @GetMapping("/api/v1/charts/{chartId}/rankings")
    public SuccessResponse<PlaydataQueryResults.ChartRankings> findChartRankings(
            @PathVariable long chartId,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return success(queryUseCase.findChartRankings(chartId, limit));
    }

    private static <T> SuccessResponse<T> success(T data) {
        return SuccessResponse.<T>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(data)
                .build();
    }
}
