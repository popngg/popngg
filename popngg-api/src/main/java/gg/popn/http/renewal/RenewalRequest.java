package gg.popn.http.renewal;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
public record RenewalRequest(@Min(1) int collectorVersion, @NotBlank String game,
        @NotNull Instant collectedAt, @NotNull @Valid Profile profile,
        @NotEmpty @Size(max=10000) List<@Valid Chart> charts,
        @NotNull @Size(max=1000) List<@Valid Warning> warnings, @NotNull @Valid Stats stats) {
    public record Profile(@NotBlank String gameId, @Size(max=64) String name,
                          @Size(max=128) String character, @Size(max=32) String popnClass) {}
    public static final class Chart {
        private String chartId,title,genre,artist,difficulty,medal,rank; private int level,score;
        private Integer versionBestScore; private boolean versionBestScorePresent;
        public String getChartId(){return chartId;} public void setChartId(String v){chartId=v;}
        @NotBlank @Size(max=255) public String getTitle(){return title;} public void setTitle(String v){title=v;}
        @NotBlank @Size(max=255) public String getGenre(){return genre;} public void setGenre(String v){genre=v;}
        @Size(max=255) public String getArtist(){return artist;} public void setArtist(String v){artist=v;}
        @NotBlank public String getDifficulty(){return difficulty;} public void setDifficulty(String v){difficulty=v;}
        @Min(1) @Max(50) public int getLevel(){return level;} public void setLevel(int v){level=v;}
        @NotBlank public String getMedal(){return medal;} public void setMedal(String v){medal=v;}
        @NotBlank public String getRank(){return rank;} public void setRank(String v){rank=v;}
        @Min(0) @Max(100000) public int getScore(){return score;} public void setScore(int v){score=v;}
        public Integer getVersionBestScore(){return versionBestScore;}
        @JsonSetter("versionBestScore") public void setVersionBestScore(Integer v){versionBestScorePresent=true;versionBestScore=v;}
        @JsonIgnore public boolean isVersionBestScorePresent(){return versionBestScorePresent;}
    }
    public record Warning(@NotBlank String code,@NotBlank @Size(max=1000) String message){}
    public record Stats(@Min(0) int levelsScanned,@Min(0) int pagesFetched,@Min(0) int detailsFetched,
                        @Min(0) int chartsFound,@Min(0) int chartsPlayed,@Min(0) long elapsedMs,
                        @Min(0) @Max(4194304) long payloadBytes){}
}
