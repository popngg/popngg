package gg.popn.domain.chart.model.field;

import gg.popn.domain.common.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class VersionTest {
    @Test
    void acceptsEveryCatalogVersionAndEtc() {
        for (int version = 1; version <= 29; version++) {
            assertThat(Version.of(version).getValue()).isEqualTo(version);
        }
        assertThat(Version.of(29).getVersionName()).isEqualTo("High☆Cheers!!");
        assertThat(Version.of(99).getVersionName()).isEqualTo("ETC");
        assertThat(Version.of(null)).isNull();
    }

    @Test
    void rejectsUnsupportedVersions() {
        for (int version : new int[]{-1, 0, 30, 98, 100}) {
            assertThatThrownBy(() -> Version.of(version)).isInstanceOf(InvalidArgumentException.class);
            assertThatThrownBy(() -> Version.builder().version(version).build().validate())
                    .isInstanceOf(InvalidArgumentException.class);
        }
        assertThatThrownBy(() -> Version.builder().build().validate()).isInstanceOf(InvalidArgumentException.class);
    }
}
