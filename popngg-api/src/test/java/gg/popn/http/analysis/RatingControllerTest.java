package gg.popn.http.analysis;

import gg.popn.application.analysis.*;
import gg.popn.application.analysis.RatingSnapshot.ChartRating;
import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.application.playdata.port.in.PlaydataQueryUseCase;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RatingControllerTest {
    @Test void returnsRankedAndHeldChartsSeparately() {
        var published=new ChartRating(1,1,"Song","Genre","jacket",49,4,false,"ELIGIBLE",List.of(),1.2,60,20,.333,"ELIGIBLE",List.of(),1200d,60,95000d,95100d);
        var held=new ChartRating(2,2,"Sparse","Genre",null,49,4,false,"HOLD",List.of("INSUFFICIENT_PLAYERS"),null,12,4,.333,"HOLD",List.of("INSUFFICIENT_PLAYERS"),null,12,90000d,90000d);
        RatingQuery query=()->new RatingSnapshot("snapshot","now","v1","EXPERIMENTAL","NOT_VALIDATED",50,List.of(held,published));
        var playdata=mock(PlaydataQueryUseCase.class);
        var record=new PlaydataQueryResults.ChartPlaydata(1,"hash","Genre","Song",4,"EX",49,29,false,
                new PlaydataQueryResults.Best(94000,10,29),new PlaydataQueryResults.Best(95000,9,28),new PlaydataQueryResults.Medal(6),null,null,null);
        when(playdata.findUserPlaydata("0000-0000-0001")).thenReturn(new PlaydataQueryResults.UserPlaydata("0000-0000-0001","User",0,0,0,List.of(record)));
        var response=new RatingController(query,playdata).charts(49,RatingController.Metric.CPI,"0000-0000-0001");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        var body=(gg.popn.http.common.response.SuccessResponse<?>)response.getBody();
        var data=(RatingController.RankingResponse)body.getData();
        assertThat(data.rankings()).singleElement().satisfies(r->assertThat(r.chart().songName()).isEqualTo("Song"));
        assertThat(data.rankings().getFirst().userPerformance().allTimeScore()).isEqualTo(95000);
        assertThat(data.rankings().getFirst().userPerformance().medalCode()).isEqualTo(6);
        assertThat(data.rankings().getFirst().userPerformance().medalName()).isEqualTo("BRONZE_DIAMOND");
        assertThat(data.rankings().getFirst().userPerformance().medalLabel()).isEqualTo("BRONZE DIAMOND");
        assertThat(data.held()).singleElement().satisfies(r->{assertThat(r.chart().songName()).isEqualTo("Sparse");assertThat(r.userPerformance().played()).isFalse();});
    }
}
