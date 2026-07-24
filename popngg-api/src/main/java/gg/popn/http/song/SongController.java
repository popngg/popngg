package gg.popn.http.song;

import gg.popn.application.song.dto.query.FindSongsQuery;
import gg.popn.application.song.port.in.FindSongsUseCase;
import gg.popn.application.song.port.in.FindSongDetailUseCase;
import gg.popn.application.song.port.in.CreateSongUseCase;
import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.http.common.response.SuccessResponse;
import gg.popn.http.song.response.SongPageResponse;
import gg.popn.http.song.response.SongDetailResponse;
import gg.popn.http.song.response.CreateSongResponse;
import gg.popn.http.song.request.CreateSongRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/songs")
public class SongController {
    private final FindSongsUseCase findSongsUseCase;
    private final FindSongDetailUseCase findSongDetailUseCase;
    private final CreateSongUseCase createSongUseCase;

    @GetMapping
    public SuccessResponse<SongPageResponse> findSongs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer version,
            @RequestParam(required = false) Integer chartVersion,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) Boolean isUpper,
            @RequestParam(required = false) Boolean hasStrictGauge,
            @RequestParam(required = false) Boolean hasStrictJudgement,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        FindSongsQuery query = new FindSongsQuery(keyword, version, chartVersion, level,
                difficulty, isUpper, hasStrictGauge, hasStrictJudgement, page, size);
        return SuccessResponse.<SongPageResponse>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(SongPageResponse.from(findSongsUseCase.execute(query)))
                .build();
    }

    @GetMapping("/{songId}")
    public SuccessResponse<SongDetailResponse> findSong(@PathVariable long songId) {
        return SuccessResponse.<SongDetailResponse>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(SongDetailResponse.from(findSongDetailUseCase.findSong(songId)))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SuccessResponse<CreateSongResponse> createSong(@Valid @RequestBody CreateSongRequest request) {
        return SuccessResponse.<CreateSongResponse>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(CreateSongResponse.from(createSongUseCase.execute(request.toCommand())))
                .build();
    }
}
