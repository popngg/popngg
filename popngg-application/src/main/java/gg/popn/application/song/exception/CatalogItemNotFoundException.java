package gg.popn.application.song.exception;

public class CatalogItemNotFoundException extends RuntimeException {
    public CatalogItemNotFoundException(String itemType, long id) {
        super(itemType + " not found: " + id);
    }
}
