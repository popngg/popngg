package gg.popn.application.song.service;

import gg.popn.application.song.dto.command.UpdateSongCommand;
import gg.popn.application.song.dto.result.SongDetailView;
import gg.popn.application.song.exception.CatalogItemNotFoundException;
import gg.popn.application.song.port.in.UpdateSongUseCase;
import gg.popn.application.song.port.out.SongCatalogQueryPort;
import gg.popn.application.song.port.out.UpdateSongPort;
import gg.popn.application.song.port.out.JacketStoragePort;
import gg.popn.domain.chart.model.field.SongHashGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.util.Set;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class UpdateSongService implements UpdateSongUseCase {
    private final SongCatalogQueryPort catalog;
    private final UpdateSongPort updatePort;
    private final JacketStoragePort jacketStorage;

    @Override @Transactional(isolation = Isolation.READ_COMMITTED)
    public SongDetailView execute(UpdateSongCommand command) {
        SongDetailView current = catalog.findSongDetail(command.songId())
                .orElseThrow(() -> new CatalogItemNotFoundException("Song", command.songId()));
        var activeCharts = current.charts().stream().filter(chart -> !chart.isDeleted()).toList();
        boolean currentUpper = activeCharts.getFirst().isUpper();
        boolean upper = command.charts().isEmpty() ? currentUpper : command.charts().getFirst().isUpper();
        if (command.charts().stream().anyMatch(chart -> chart.isUpper() != upper))
            throw new IllegalArgumentException("A song cannot mix regular and Upper charts.");
        if (command.charts().stream().anyMatch(chart -> chart.level() < 1 || chart.level() > 50
                || chart.chartVersion() < 1 || (chart.chartId() == null
                && (chart.difficultyCode() < 1 || chart.difficultyCode() > 4))))
            throw new IllegalArgumentException("Chart level or version is invalid.");
        if (command.charts().stream().filter(chart -> chart.chartId() == null)
                .anyMatch(chart -> activeCharts.stream().anyMatch(existing ->
                        existing.difficulty().code() == chart.difficultyCode()
                                && existing.isUpper() == chart.isUpper())))
            throw new IllegalArgumentException("The chart difficulty already exists.");
        if (upper != currentUpper) {
            Set<Long> requested = command.charts().stream().map(UpdateSongCommand.ChartUpdate::chartId)
                    .collect(Collectors.toSet());
            if (activeCharts.stream().anyMatch(chart -> !requested.contains(chart.chartId())))
                throw new IllegalArgumentException("Every active chart is required when changing Upper status.");
        }
        String hash = SongHashGenerator.generate(command.genreName(), command.songName(),
                command.artistName(), command.version(), upper);
        String oldHash = current.song().songHash();
        boolean copied = !hash.equals(oldHash) && current.song().jacketUrl() != null
                && command.jacketUrl() == null;
        String jacketUrl = command.jacketUrl() != null ? command.jacketUrl()
                : copied ? jacketStorage.copy(oldHash, hash) : current.song().jacketUrl();
        var effective = new UpdateSongCommand(command.songId(), command.genreName(), command.songName(),
                command.artistName(), command.version(), jacketUrl, command.createdAt(), command.charts());
        try {
            updatePort.update(effective, hash);
        } catch (RuntimeException exception) {
            if (copied) jacketStorage.delete(hash);
            throw exception;
        }
        return catalog.findSongDetail(command.songId())
                .orElseThrow(() -> new CatalogItemNotFoundException("Song", command.songId()));
    }
}
