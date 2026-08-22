package gg.popn.http.song;

import gg.popn.application.song.dto.query.FindSongsQuery;
import gg.popn.application.song.port.in.FindSongDetailUseCase;
import gg.popn.application.song.port.in.FindSongsUseCase;
import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.http.common.response.PageResponse;
import gg.popn.http.common.response.SuccessResponse;
import gg.popn.http.song.response.FrontendChartResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/charts")
public class FrontendChartController {
    private final FindSongsUseCase findSongsUseCase;
    private final FindSongDetailUseCase findSongDetailUseCase;

    @GetMapping
    public SuccessResponse<PageResponse<FrontendChartResponse>> findCharts(
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(required = false) Integer version,
            @RequestParam(required = false) Integer levelMin,
            @RequestParam(required = false) Integer levelMax,
            @RequestParam(required = false) java.util.List<Integer> difficulty,
            @RequestParam(defaultValue = "version") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 1) {
            throw new IllegalArgumentException("page must be one or greater");
        }
        var result = findSongsUseCase.execute(new FindSongsQuery(
                keyword, version, null, levelMin, levelMax, difficulty,
                null, null, null, FindSongsQuery.Sort.from(sort),
                FindSongsQuery.Order.from(order), true, page - 1, size));
        return success(PageResponse.of(
                result.content().stream().map(FrontendChartResponse::from).toList(),
                result.totalElements(), result.page(), result.size()));
    }

    @GetMapping("/{songId}")
    public SuccessResponse<FrontendChartResponse> findChart(@PathVariable long songId) {
        return success(FrontendChartResponse.from(findSongDetailUseCase.findSong(songId)));
    }

    private static <T> SuccessResponse<T> success(T data) {
        return SuccessResponse.<T>builder().code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS).data(data).build();
    }
}
