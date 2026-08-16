package gg.popn.application.playdata.service;

import org.springframework.stereotype.Component;

@Component
public class PlaydataUpsertPolicy {
    public Decision decide(State existing, Observation observed, int currentVersion,
                           TransitionPolicy transition) {
        if (existing == null) {
            return new Decision(true, new State(currentVersion, observed.effectiveVersionScore(), observed.rankCode(),
                    observed.score(), currentVersion, observed.rankCode(), observed.medalCode()));
        }

        int versionScore;
        Integer versionRank;
        if (existing.currentVersion() == currentVersion) {
            versionScore = Math.max(existing.versionScore(), observed.effectiveVersionScore());
            versionRank = observed.effectiveVersionScore() >= existing.versionScore()
                    ? observed.rankCode() : existing.versionRankCode();
        } else {
            if (transition == null) {
                throw new MissingGameVersionTransitionException(
                        existing.currentVersion(), currentVersion);
            }
            if (transition == TransitionPolicy.RESET) {
                versionScore = observed.effectiveVersionScore();
                versionRank = observed.rankCode();
            } else {
                versionScore = Math.max(existing.versionScore(), observed.effectiveVersionScore());
                versionRank = observed.effectiveVersionScore() >= existing.versionScore()
                        ? observed.rankCode() : existing.versionRankCode();
            }
        }

        int allTimeScore = Math.max(existing.allTimeScore(), observed.score());
        int allTimeVersion = observed.score() > existing.allTimeScore()
                ? currentVersion : existing.allTimeScoreVersion();
        Integer allTimeRank = observed.score() >= existing.allTimeScore()
                ? observed.rankCode() : existing.allTimeRankCode();
        State state = new State(currentVersion, versionScore, versionRank,
                allTimeScore, allTimeVersion, allTimeRank, observed.medalCode());
        return new Decision(!state.equals(existing), state);
    }

    public record State(
            int currentVersion,
            int versionScore,
            Integer versionRankCode,
            int allTimeScore,
            int allTimeScoreVersion,
            Integer allTimeRankCode,
            int medalCode
    ) {
    }

    public record Observation(int score, int rankCode, int medalCode, Integer versionBestScore, boolean versionBestScorePresent) {
        public Observation(int score,int rankCode,int medalCode){this(score,rankCode,medalCode,null,false);}
        int effectiveVersionScore(){return versionBestScorePresent?(versionBestScore==null?0:versionBestScore):score;}
    }

    public record Decision(boolean changed, State state) {
    }

    public enum TransitionPolicy {
        RESET,
        CARRY_OVER;

        public static TransitionPolicy fromDatabase(String value) {
            return switch (value) {
                case "RESET" -> RESET;
                case "CARRY_OVER" -> CARRY_OVER;
                default -> throw new IllegalStateException("Unsupported game version transition policy.");
            };
        }
    }

    public static final class MissingGameVersionTransitionException extends IllegalStateException {
        public MissingGameVersionTransitionException(int fromVersion, int toVersion) {
            super("No approved game version transition exists from "
                    + fromVersion + " to " + toVersion + ".");
        }
    }
}
