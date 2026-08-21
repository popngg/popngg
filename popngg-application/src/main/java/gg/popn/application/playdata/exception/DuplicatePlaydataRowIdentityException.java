package gg.popn.application.playdata.exception;

public class DuplicatePlaydataRowIdentityException extends IllegalArgumentException {
    public DuplicatePlaydataRowIdentityException() {
        super("The renewal payload contains the same chart more than once.");
    }
}
