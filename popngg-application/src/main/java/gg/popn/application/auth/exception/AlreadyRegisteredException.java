package gg.popn.application.auth.exception;

public class AlreadyRegisteredException extends RuntimeException {
    public AlreadyRegisteredException() {
        super("The poptomo ID is already registered.");
    }
}
