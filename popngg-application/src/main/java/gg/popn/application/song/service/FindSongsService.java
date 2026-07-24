package gg.popn.application.song.service;

import gg.popn.application.song.dto.query.FindSongsQuery;
import gg.popn.application.song.dto.result.SongPageView;
import gg.popn.application.song.port.in.FindSongsUseCase;
import gg.popn.application.song.port.out.SongCatalogQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindSongsService implements FindSongsUseCase {
    private final SongCatalogQueryPort songCatalogQueryPort;

    @Override
    public SongPageView execute(FindSongsQuery query) {
        long total = songCatalogQueryPort.count(query);
        return SongPageView.of(songCatalogQueryPort.findPage(query), query.page(), query.size(), total);
    }
}
