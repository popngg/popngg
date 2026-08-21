package gg.popn.domain.chart.model.field;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SongHashGeneratorTest {
    @Test
    void normalizesUnicodeCaseAndWhitespace() {
        String first = SongHashGenerator.generate("  POP  MUSIC ", "Cafe\u0301", " Artist ", 29, false);
        String second = SongHashGenerator.generate("pop music", "Café", "artist", 29, false);

        assertThat(first).hasSize(64).isEqualTo(second);
    }

    @Test
    void separatesCanonicalFields() {
        assertThat(SongHashGenerator.generate("ab", "c", "", 29, false))
                .isNotEqualTo(SongHashGenerator.generate("a", "bc", "", 29, false));
    }

    @Test
    void upperFlagChangesHash() {
        assertThat(SongHashGenerator.generate("genre", "song", "artist", 29, false))
                .isNotEqualTo(SongHashGenerator.generate("genre", "song", "artist", 29, true));
    }

    @Test
    void upperDisplaySuffixDoesNotBecomePartOfMetadata() {
        assertThat(SongHashGenerator.generate("genre (UPPER)", "song(UPPER)", "artist", 29, true))
                .isEqualTo(SongHashGenerator.generate("genre", "song", "artist", 29, true));
    }
}
