package gg.popn.http.analysis;

import gg.popn.application.analysis.RatingSnapshot;
import gg.popn.application.analysis.RatingSnapshot.ChartRating;
import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class TierListImageRendererTest {
    @Test void rendersPersonalTierListAsPng()throws Exception{
        var charts=new ArrayList<ChartRating>();var records=new ArrayList<PlaydataQueryResults.ChartPlaydata>();
        for(int i=0;i<9;i++){
            charts.add(new ChartRating(i+1,i+1,"楽曲 "+i,"ジャンル",null,49,4,false,"ELIGIBLE",List.of(),10d-i,60,30,.5,"ELIGIBLE",List.of(),1000d-i,60,95000d,95000d));
            if(i<5)records.add(new PlaydataQueryResults.ChartPlaydata(i+1,"hash","ジャンル","楽曲",4,"EX",49,29,false,new PlaydataQueryResults.Best(94000+i,9,29),new PlaydataQueryResults.Best(95000+i,9,28),new PlaydataQueryResults.Medal(i<3?6:8),null,null,null));
        }
        var snapshot=new RatingSnapshot("id","now","v1","EXPERIMENTAL","NOT_VALIDATED",50,charts);
        var user=new PlaydataQueryResults.UserPlaydata("0000-0000-0001","테스트 유저",0,0,0,records);
        byte[] png=new TierListImageRenderer().render(snapshot,user,49,RatingController.Metric.CPI);
        assertThat(png).startsWith((byte)0x89,(byte)0x50,(byte)0x4e,(byte)0x47);
        var image=ImageIO.read(new ByteArrayInputStream(png));assertThat(image.getWidth()).isEqualTo(1160);assertThat(image.getHeight()).isGreaterThan(400);
    }
    @Test void createsASeparateBandOnlyForOutlierGaps(){
        var charts=List.of(chart(1,10),chart(2,9.9),chart(3,9.8),chart(4,5),chart(5,4.9),chart(6,4.8));
        assertThat(TierListImageRenderer.bands(charts,RatingController.Metric.CPI)).hasSize(2);
    }
    @Test void doesNotCreateTinyPartitionsAndUsesClearMedalPolicy(){
        var charts=new ArrayList<ChartRating>();
        for(int i=0;i<20;i++)charts.add(chart(i+1,20-i-(i>=1?5:0)-(i>=18?5:0)));
        assertThat(TierListImageRenderer.bands(charts,RatingController.Metric.CPI)).singleElement().satisfies(b->assertThat(b).hasSize(20));
        assertThat(TierListImageRenderer.isCleared(1)).isTrue();
        assertThat(TierListImageRenderer.isCleared(11)).isTrue();
        assertThat(TierListImageRenderer.isCleared(12)).isTrue();
        assertThat(TierListImageRenderer.isCleared(8)).isFalse();
        assertThat(TierListImageRenderer.medalIconResource(1)).isEqualTo("/medals/gold-star.png");
        assertThat(TierListImageRenderer.medalIconResource(9)).isEqualTo("/medals/black-diamond.png");
        assertThat(TierListImageRenderer.medalIconResource(99)).isEqualTo("/medals/none.png");
    }
    private static ChartRating chart(long id,double cpi){return new ChartRating(id,id,"song","genre",null,49,4,false,"ELIGIBLE",List.of(),cpi,60,30,.5,"ELIGIBLE",List.of(),1000d,60,95000d,95000d);}
}
