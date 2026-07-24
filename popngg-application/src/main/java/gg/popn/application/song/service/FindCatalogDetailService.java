package gg.popn.application.song.service;

import gg.popn.application.song.dto.result.ChartDetailView;
import gg.popn.application.song.dto.result.SongDetailView;
import gg.popn.application.song.exception.CatalogItemNotFoundException;
import gg.popn.application.song.port.in.FindChartDetailUseCase;
import gg.popn.application.song.port.in.FindSongDetailUseCase;
import gg.popn.application.song.port.out.SongCatalogQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindCatalogDetailService implements FindSongDetailUseCase, FindChartDetailUseCase {
    private final SongCatalogQueryPort songCatalogQueryPort;

    @Override
    public SongDetailView findSong(long songId) {
        return songCatalogQueryPort.findSongDetail(songId)
                .orElseThrow(() -> new CatalogItemNotFoundException("Song", songId));
    }

    @Override
    public ChartDetailView findChart(long chartId) {
        return songCatalogQueryPort.findChartDetail(chartId)
                .orElseThrow(() -> new CatalogItemNotFoundException("Chart", chartId));
    }
}
