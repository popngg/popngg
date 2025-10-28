package gg.popn.application.playdata.dto.response;

import gg.popn.application.playdata.dto.PlaydataDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MainPlaydataDto {
    private RankingDataDto myData;
    private List<RankingDataDto> allData;
    private List<Integer> medalCounts;
    private List<Integer> rankCounts;
}
