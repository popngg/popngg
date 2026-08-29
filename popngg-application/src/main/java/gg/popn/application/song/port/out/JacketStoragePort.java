package gg.popn.application.song.port.out;

public interface JacketStoragePort {
    String uploadPng(String songHash, byte[] png);
    String copy(String sourceSongHash, String targetSongHash);
    String replacePng(String songHash, byte[] png);
    void restore(String songHash, String backupKey);
    void delete(String songHash);
}
