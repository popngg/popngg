package gg.popn.domain.user.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RankingResponse {
    private RankingDataDto myData;
    private List<RankingDataDto> allData;
    private List<Integer> medalCounts;
    private List<Integer> rankCounts;
}
