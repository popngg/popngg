package gg.popn.application.song.exception;

public class CatalogItemNotFoundException extends RuntimeException {
    public CatalogItemNotFoundException(String itemType, long id) {
        super(itemType + " not found: " + id);
    }

    public CatalogItemNotFoundException(String itemType, String identifier) {
        super(itemType + " not found: " + identifier);
    }
}
