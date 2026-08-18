package gg.popn.http.common.response;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageResponseTest {
    @Test
    void calculatesFirstMiddleAndLastPageNavigation() {
        var first = PageResponse.of(List.of("a", "b"), 5, 0, 2);
        var middle = PageResponse.of(List.of("c", "d"), 5, 1, 2);
        var last = PageResponse.of(List.of("e"), 5, 2, 2);

        assertThat(first.totalPages()).isEqualTo(3);
        assertThat(first.hasPrev()).isFalse();
        assertThat(first.hasNext()).isTrue();
        assertThat(middle.hasPrev()).isTrue();
        assertThat(middle.hasNext()).isTrue();
        assertThat(last.hasPrev()).isTrue();
        assertThat(last.hasNext()).isFalse();
    }

    @Test
    void representsEmptyPage() {
        var page = PageResponse.of(List.of(), 0, 0, 20);

        assertThat(page.items()).isEmpty();
        assertThat(page.totalItems()).isZero();
        assertThat(page.totalPages()).isZero();
        assertThat(page.hasPrev()).isFalse();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void rejectsInvalidPageArguments() {
        assertThatThrownBy(() -> PageResponse.of(List.of(), 0, -1, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PageResponse.of(List.of(), 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
