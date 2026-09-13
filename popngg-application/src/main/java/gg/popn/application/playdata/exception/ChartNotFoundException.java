package gg.popn.application.playdata.exception;

public class ChartNotFoundException extends RuntimeException {
    public ChartNotFoundException() {
        super("Chart not found.");
    }
}
