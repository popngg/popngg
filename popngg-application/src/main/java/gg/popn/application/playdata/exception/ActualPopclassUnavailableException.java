package gg.popn.application.playdata.exception;

public final class ActualPopclassUnavailableException extends RuntimeException {
    public ActualPopclassUnavailableException(String poptomoId) {
        super("Version-best scores are unavailable for user " + poptomoId + ".");
    }
}
