package gg.popn.http.song.response;

import gg.popn.application.song.dto.result.DifficultyView;

public record DifficultyResponse(int code, String label, String shortLabel, int sortOrder) {
    public static DifficultyResponse from(DifficultyView view) {
        return new DifficultyResponse(view.code(), view.label(), view.shortLabel(), view.sortOrder());
    }
}
