package gg.popn.domain.chart.model.field;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SongHashGeneratorTest {
    @Test
    void normalizesUnicodeCaseAndWhitespace() {
        String first = SongHashGenerator.generate("  POP  MUSIC ", "Cafe\u0301", " Artist ", 29);
        String second = SongHashGenerator.generate("pop music", "Café", "artist", 29);

        assertThat(first).hasSize(64).isEqualTo(second);
    }

    @Test
    void separatesCanonicalFields() {
        assertThat(SongHashGenerator.generate("ab", "c", "", 29))
                .isNotEqualTo(SongHashGenerator.generate("a", "bc", "", 29));
    }
}
