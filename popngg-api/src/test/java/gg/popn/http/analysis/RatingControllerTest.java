package gg.popn.http.analysis;

import gg.popn.application.analysis.*;
import gg.popn.application.analysis.RatingSnapshot.ChartRating;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class RatingControllerTest {
    @Test void returnsRankedAndHeldChartsSeparately() {
        var published=new ChartRating(1,1,"Song","Genre","jacket",49,4,false,"PUBLISHED",1.2,60,20,.333,"PUBLISHED",1200d,60,95000d,95100d);
        var held=new ChartRating(2,2,"Sparse","Genre",null,49,4,false,"HOLD",null,12,4,.333,"HOLD",null,12,90000d,90000d);
        RatingQuery query=()->new RatingSnapshot("snapshot","now","v1","CANDIDATE",50,List.of(held,published));
        var response=new RatingController(query).charts(49,RatingController.Metric.CPI);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        var body=(gg.popn.http.common.response.SuccessResponse<?>)response.getBody();
        var data=(RatingController.RankingResponse)body.getData();
        assertThat(data.rankings()).singleElement().satisfies(r->assertThat(r.chart().songName()).isEqualTo("Song"));
        assertThat(data.held()).singleElement().satisfies(r->assertThat(r.songName()).isEqualTo("Sparse"));
    }
}
