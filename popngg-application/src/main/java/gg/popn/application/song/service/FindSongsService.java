package gg.popn.application.song.service;

import gg.popn.application.song.dto.query.FindSongsQuery;
import gg.popn.application.song.dto.result.SongPageView;
import gg.popn.application.song.port.in.FindSongsUseCase;
import gg.popn.application.song.port.out.SongCatalogQueryPort;
import lombok.RequiredArgsConstructor;
import gg.popn.application.song.exception.InvalidSongQueryException;
import gg.popn.application.song.port.in.FindRecentSongsUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindSongsService implements FindSongsUseCase, FindRecentSongsUseCase {
    private final SongCatalogQueryPort songCatalogQueryPort;

    @Override
    public SongPageView findRecent(int limit) {
        if (limit < 1 || limit > 10) {
            throw new InvalidSongQueryException("limit must be between 1 and 10");
        }
        return execute(new FindSongsQuery(null, null, null, null, null, null,
                null, null, null, FindSongsQuery.Sort.CREATED_AT, FindSongsQuery.Order.DESC,
                true, 0, limit));
    }

    @Override
    public SongPageView execute(FindSongsQuery query) {
        long total = songCatalogQueryPort.count(query);
        return SongPageView.of(songCatalogQueryPort.findPage(query), query.page(), query.size(), total);
    }
}
