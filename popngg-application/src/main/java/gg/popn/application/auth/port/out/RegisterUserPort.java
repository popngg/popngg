package gg.popn.application.auth.port.out;

public interface RegisterUserPort {
    boolean exists(String poptomoId);
    void create(String poptomoId, String passwordHash, boolean hidden);
}
