package gg.popn.infra.db.adapter;

import gg.popn.application.song.dto.command.CreateSongCommand;
import gg.popn.application.song.port.in.CreateSongUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewedSongRegistrationJdbcAdapterTest {
    private JdbcTemplate jdbc;
    private TransactionTemplate transaction;
    private ReviewedSongRegistrationJdbcAdapter adapter;
    private final CreateSongUseCase creator = mock(CreateSongUseCase.class);

    @BeforeEach void setUp() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:official-" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(ds);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(ds));
        UserDirectoryTestSchema.create(jdbc);
        jdbc.execute("CREATE TABLE songs(song_id BIGINT PRIMARY KEY,song_name VARCHAR(255),artist_name VARCHAR(255),genre_name VARCHAR(255))");
        jdbc.execute("CREATE TABLE charts(chart_id BIGINT PRIMARY KEY,song_id BIGINT,is_upper BOOLEAN,is_deleted BOOLEAN)");
        jdbc.execute("CREATE TABLE unknown_chart_reports(report_id BIGINT PRIMARY KEY,song_name VARCHAR(255),artist_name VARCHAR(255),resolved BOOLEAN)");
        jdbc.update("INSERT INTO unknown_chart_reports VALUES(7,'Song','Artist',FALSE)");
        adapter = new ReviewedSongRegistrationJdbcAdapter(jdbc,creator);
        when(creator.execute(any())).thenAnswer(call -> {
            jdbc.update("INSERT INTO songs VALUES(55,'Song','Artist','new genre')");
            jdbc.update("INSERT INTO charts VALUES(1,55,FALSE,FALSE)");
            return new gg.popn.application.song.dto.result.CreateSongResult(55,List.of(1L));
        });
    }

    @Test void writesAndResolvesAtomicallyAndPreventsDuplicateConfirmation() {
        assertThat(transaction.execute(s -> adapter.register(7,command(null))).songId()).isEqualTo(55);
        assertThat(jdbc.queryForObject("SELECT resolved FROM unknown_chart_reports WHERE report_id=7",Boolean.class)).isTrue();
        assertThatThrownBy(() -> transaction.execute(s -> adapter.register(7,command(null)))).hasMessageContaining("이미 등록");
        verify(creator,times(1)).execute(any());
    }

    @Test void staleGenreAndDeletedChartsStillBlockDuplicateSongsButUpperIsSeparate() {
        jdbc.update("INSERT INTO songs VALUES(55,'Song','Artist','old genre')");
        jdbc.update("INSERT INTO charts VALUES(1,55,FALSE,TRUE)");
        assertThat(adapter.findExisting("Song","Artist",false)).containsExactly(55L);
        assertThat(adapter.findExisting("Song","Artist",true)).isEmpty();
        assertThatThrownBy(() -> transaction.execute(s -> adapter.register(7,command(null)))).hasMessageContaining("곡수정");
        verifyNoInteractions(creator);
    }

    @Test void chartlessSongsChangedReportsAndJacketsAreRejected() {
        jdbc.update("INSERT INTO songs VALUES(55,'Song','Artist','old genre')");
        assertThat(adapter.findExisting("Song","Artist",true)).containsExactly(55L);
        jdbc.update("DELETE FROM songs");
        jdbc.update("UPDATE unknown_chart_reports SET resolved=TRUE");
        assertThatThrownBy(() -> transaction.execute(s -> adapter.register(7,command(null)))).hasMessageContaining("처리되었습니다");
        assertThatThrownBy(() -> transaction.execute(s -> adapter.register(7,command("https://image")))).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(creator);
    }

    @Test void failedRegistrationRollsBackCatalogAndKeepsReportUnresolved() {
        doAnswer(call -> {
            jdbc.update("INSERT INTO songs VALUES(55,'Song','Artist','genre')");
            throw new IllegalStateException("failed chart insert");
        }).when(creator).execute(any());
        assertThatThrownBy(() -> transaction.execute(s -> adapter.register(7,command(null)))).hasMessageContaining("failed chart");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM songs",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT resolved FROM unknown_chart_reports WHERE report_id=7",Boolean.class)).isFalse();
    }

    private CreateSongCommand command(String jacket) {
        return new CreateSongCommand(null,"Genre","Song","Artist",29,jacket,
                List.of(new CreateSongCommand.CreateChartCommand(4,49,29,false,false,false)));
    }
}
