package gg.popn.http.playdata;

import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.application.playdata.dto.query.FindUserRecordsQuery;
import gg.popn.application.playdata.port.in.PlaydataQueryUseCase;
import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.http.common.response.SuccessResponse;
import gg.popn.http.common.response.PageResponse;
import gg.popn.http.playdata.response.PopclassTargetResponse;
import gg.popn.http.playdata.response.LevelStatsResponse;
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

    @GetMapping("/api/v1/users/{poptomoId}/records")
    public SuccessResponse<PageResponse<PlaydataQueryResults.UserRecord>> findUserRecords(
            @PathVariable String poptomoId,
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(required = false) Integer version,
            @RequestParam(required = false) Integer levelMin,
            @RequestParam(required = false) Integer levelMax,
            @RequestParam(required = false) List<Integer> difficulty,
            @RequestParam(required = false) List<Integer> medal,
            @RequestParam(required = false) List<Integer> rank,
            @RequestParam(required = false) Integer scoreMin,
            @RequestParam(required = false) Integer scoreMax,
            @RequestParam(defaultValue = "level") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 1) throw new IllegalArgumentException("page must be one or greater");
        var result = queryUseCase.findUserRecords(poptomoId, new FindUserRecordsQuery(
                keyword, version, levelMin, levelMax, difficulty, medal, rank,
                scoreMin, scoreMax, sort, order, page - 1, size));
        return success(PageResponse.of(
                result.items(), result.totalItems(), result.page(), result.size()));
    }

    @GetMapping("/api/v1/users/{poptomoId}/progress")
    public SuccessResponse<PlaydataQueryResults.Progress> findProgress(
            @PathVariable String poptomoId,
            @RequestParam(defaultValue = "level") String by
    ) {
        return success(queryUseCase.findProgress(poptomoId, by));
    }

    @GetMapping("/api/v1/users/{poptomoId}/level-stats")
    public SuccessResponse<List<LevelStatsResponse>> findLevelStats(
            @PathVariable String poptomoId
    ) {
        var progress = queryUseCase.findProgress(poptomoId, "level");
        return success(progress.rows().stream().map(LevelStatsResponse::from).toList());
    }

    @GetMapping("/api/v1/users/{poptomoId}/popn-class-targets/current")
    public SuccessResponse<PopclassTargetResponse.CurrentTargets> findCurrentPopclassTargets(
            @PathVariable String poptomoId
    ) {
        return success(PopclassTargetResponse.CurrentTargets.from(
                queryUseCase.findPopclass(poptomoId)));
    }

    @GetMapping("/api/v1/users/{poptomoId}/popn-class-targets/potential")
    public SuccessResponse<PopclassTargetResponse.CurrentTargets> findPotentialPopclassTargets(
            @PathVariable String poptomoId
    ) {
        return success(PopclassTargetResponse.CurrentTargets.potentialFrom(
                queryUseCase.findPotentialPopclass(poptomoId)));
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
