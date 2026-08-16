package gg.popn.application.auth.dto.command;

public record RegisterCommand(String poptomoId, String password, boolean hidden) {
}
