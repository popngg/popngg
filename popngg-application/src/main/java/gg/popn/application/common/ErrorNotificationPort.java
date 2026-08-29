package gg.popn.application.common;

public interface ErrorNotificationPort {
    void notifyServerError(String method, String path, String exceptionType, String traceId);
}
