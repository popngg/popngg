package gg.popn.application.playdata.dto.command;

import java.util.List;

public record ImportPlaydataCommand(
        String poptomoId,
        ProfileSnapshot profile,
        List<Row> rows
) {
    public record ProfileSnapshot(
            String userName,
            String characterName,
            Integer normalCredit,
            Integer extraCredit,
            Integer timePlay10Credit,
            Integer timePlay16Credit
    ) {
    }

    public record Row(
            Long chartId,
            Long songId,
            Integer difficultyCode,
            Boolean upper,
            String songHash,
            String songName,
            String genreName,
            Integer score,
            Integer rankCode,
            Integer medalCode,
            Integer versionBestScore,
            boolean versionBestScorePresent
    ) {
        public Row(Long chartId, Long songId, Integer difficultyCode, Boolean upper, String songHash,
                   String songName, String genreName, Integer score, Integer rankCode, Integer medalCode) {
            this(chartId,songId,difficultyCode,upper,songHash,songName,genreName,score,rankCode,medalCode,null,false);
        }
    }
}
