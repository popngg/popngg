package gg.popn.http.analysis;

import gg.popn.application.analysis.*;
import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.application.playdata.port.in.PlaydataQueryUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RatingImageControllerTest {
    @Test void returnsDownloadablePng()throws Exception{
        RatingQuery ratings=()->new RatingSnapshot("id","now","v1","EXPERIMENTAL","NOT_VALIDATED",50,List.of());
        var playdata=mock(PlaydataQueryUseCase.class);var renderer=mock(TierListImageRenderer.class);
        var user=new PlaydataQueryResults.UserPlaydata("0000-0000-0001","User",0,0,0,List.of());
        when(playdata.findUserPlaydata("0000-0000-0001")).thenReturn(user);when(renderer.render(any(),eq(user),eq(49),eq(RatingController.Metric.CPI))).thenReturn(new byte[]{1,2,3});
        var response=new RatingImageController(ratings,playdata,renderer).image(49,RatingController.Metric.CPI,"0000-0000-0001");
        assertThat(response.getStatusCode().value()).isEqualTo(200);assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("popngg-lv49-cpi-0000-0000-0001.png");
        assertThat(response.getBody()).isEqualTo(new byte[]{1,2,3});
    }
    @Test void rejectsBotAndMalformedIds(){
        var controller=new RatingImageController(mock(RatingQuery.class),mock(PlaydataQueryUseCase.class),mock(TierListImageRenderer.class));
        assertThat(controller.image(49,RatingController.Metric.CPI,"BOT-1-1").getStatusCode().value()).isEqualTo(400);
    }
}
