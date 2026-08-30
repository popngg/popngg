package gg.popn.application.account.dto;

public record ProfileUpdate(String comment, boolean privateProfile, Avatar avatar, boolean removeAvatar) {
    public record Avatar(byte[] bytes, String contentType) {
    }
}
