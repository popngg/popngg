package gg.popn.application.song.service;

import gg.popn.application.song.dto.command.CreateSongCommand;
import gg.popn.application.song.dto.result.CreateSongResult;
import gg.popn.application.song.port.in.CreateSongUseCase;
import gg.popn.application.song.port.out.CreateSongPort;
import gg.popn.domain.game.policy.DifficultyPolicy;
import gg.popn.domain.chart.model.field.SongHashGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CreateSongService implements CreateSongUseCase {
    private final CreateSongPort createSongPort;

    @Override
    @Transactional
    public CreateSongResult execute(CreateSongCommand command) {
        if (command.genreName() == null || command.genreName().isBlank()
                || command.songName() == null || command.songName().isBlank()
                || command.charts() == null || command.charts().isEmpty()) {
            throw new IllegalArgumentException("Song metadata and at least one chart are required.");
        }
        Set<String> chartKeys = new HashSet<>();
        boolean isUpperSong = command.charts().get(0).isUpper();
        for (CreateSongCommand.CreateChartCommand chart : command.charts()) {
            DifficultyPolicy.fromCode(chart.difficulty());
            if (chart.level() < 1 || chart.level() > 50) {
                throw new IllegalArgumentException("Chart level must be between 1 and 50.");
            }
            if (!chartKeys.add(chart.difficulty() + ":" + chart.isUpper())) {
                throw new IllegalArgumentException("Duplicate difficulty and Upper combination.");
            }
            if (chart.isUpper() != isUpperSong) {
                throw new IllegalArgumentException("A song cannot mix regular and Upper charts.");
            }
        }
        var normalizedCommand = new CreateSongCommand(
                SongHashGenerator.generate(command.genreName(), command.songName(),
                        command.artistName(), command.version(), isUpperSong),
                command.genreName(), command.songName(), command.artistName(), command.version(),
                command.jacketUrl(), command.charts());
        return createSongPort.create(normalizedCommand);
    }
}
