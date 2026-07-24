package gg.popn.infra.security.adapter;

import gg.popn.application.auth.port.out.PasswordResetMailPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetMailEventAdapter implements PasswordResetMailPort {
    private final ApplicationEventPublisher eventPublisher;

    public PasswordResetMailEventAdapter(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void sendResetLink(String email, String rawToken) {
        eventPublisher.publishEvent(new PasswordResetMailRequested(email, rawToken));
    }

    public record PasswordResetMailRequested(String email, String rawToken) {
    }
}
