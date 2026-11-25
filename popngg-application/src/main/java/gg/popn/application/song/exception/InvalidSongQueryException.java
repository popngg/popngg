package gg.popn.application.song.exception;

public class InvalidSongQueryException extends IllegalArgumentException {
    public InvalidSongQueryException(String message) {
        super(message);
    }
}
