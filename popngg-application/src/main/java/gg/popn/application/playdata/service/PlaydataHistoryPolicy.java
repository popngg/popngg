package gg.popn.application.playdata.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PlaydataHistoryPolicy {
    public List<EventType> events(PlaydataUpsertPolicy.State previous,
                                  PlaydataUpsertPolicy.State current,
                                  PlaydataUpsertPolicy.TransitionPolicy transition) {
        if (previous == null) return List.of(EventType.REGISTER);
        if (previous.equals(current)) return List.of();

        var events = new ArrayList<EventType>();
        if (previous.currentVersion() != current.currentVersion()) {
            events.add(transition == PlaydataUpsertPolicy.TransitionPolicy.CARRY_OVER
                    ? EventType.VERSION_CARRIED_OVER : EventType.VERSION_INITIALIZED);
        }
        if (current.versionScore() > previous.versionScore()) events.add(EventType.SCORE_UP);
        if (current.allTimeScore() > previous.allTimeScore()) {
            events.add(EventType.ALL_TIME_SCORE_UP);
        }
        if (!same(previous.versionRankCode(), current.versionRankCode())
                || !same(previous.allTimeRankCode(), current.allTimeRankCode())) {
            events.add(EventType.RANK_CHANGED);
        }
        if (previous.medalCode() != current.medalCode()) events.add(EventType.MEDAL_CHANGED);
        return List.copyOf(events);
    }

    private static boolean same(Integer left, Integer right) {
        return left == null ? right == null : left.equals(right);
    }

    public enum EventType {
        REGISTER,
        SCORE_UP,
        ALL_TIME_SCORE_UP,
        RANK_CHANGED,
        MEDAL_CHANGED,
        VERSION_INITIALIZED,
        VERSION_CARRIED_OVER
    }
}
