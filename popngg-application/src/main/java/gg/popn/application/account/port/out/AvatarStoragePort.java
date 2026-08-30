package gg.popn.application.account.port.out;

public interface AvatarStoragePort {
    String upload(String poptomoId, byte[] bytes, String contentType);
    void deleteIfManaged(String publicUrl);
}
