package gg.popn.application.song.port.out;

public interface JacketStoragePort {
    String uploadPng(String songHash, byte[] png);
    /** Publish a replacement at a new immutable URL; retain the prior object. */
    String uploadVersionedPng(String songHash, byte[] png);
    String copyFromUrl(String sourceJacketUrl, String targetSongHash);
    String copy(String sourceSongHash, String targetSongHash);
    String replacePng(String songHash, byte[] png);
    void restore(String songHash, String backupKey);
    void delete(String songHash);
}
