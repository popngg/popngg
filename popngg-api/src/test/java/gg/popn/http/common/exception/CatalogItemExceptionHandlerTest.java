package gg.popn.http.common.exception;

import gg.popn.application.song.exception.CatalogItemNotFoundException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogItemExceptionHandlerTest {
    @Test
    void returnsNotFoundResponse() {
        var response = new BaseExceptionHandler()
                .handleCatalogItemNotFound(new CatalogItemNotFoundException("Song", 10));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).containsEntry("code", "CATALOG_ITEM_NOT_FOUND");
    }
}
