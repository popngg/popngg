package gg.popn.application.playdata.dto.query;

import java.util.List;

public record FindUserRecordsQuery(
        String keyword,
        Integer version,
        Integer levelMin,
        Integer levelMax,
        List<Integer> difficulties,
        List<Integer> medals,
        List<Integer> ranks,
        Integer scoreMin,
        Integer scoreMax,
        String sort,
        String order,
        int page,
        int size
) {
}
