package gg.popn.http.analysis;

import gg.popn.application.analysis.RatingSnapshot;
import gg.popn.application.analysis.RatingSnapshot.ChartRating;
import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.application.user.dto.result.UserProfileResult;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class TierListImageRendererTest {
    @Test void rendersPersonalTierListAsPng()throws Exception{
        var charts=new ArrayList<ChartRating>();var records=new ArrayList<PlaydataQueryResults.ChartPlaydata>();
        for(int i=0;i<9;i++){
            charts.add(new ChartRating(i+1,i+1,"楽曲 "+i,"ジャンル",null,49,4,false,i==0?"SUPER_EXTRA":"NONE",
                    i==0,i==0,i==0?"CANDIDATE":"NOT_CALCULATED","NOT_CALCULATED",
                    "ELIGIBLE",List.of(),10d-i,60,30,.5,"ELIGIBLE",List.of(),1000d-i,60,95000d,95000d));
            if(i<5)records.add(new PlaydataQueryResults.ChartPlaydata(i+1,"hash","ジャンル","楽曲",4,"EX",49,29,false,new PlaydataQueryResults.Best(94000+i,9,29),new PlaydataQueryResults.Best(95000+i,9,28),new PlaydataQueryResults.Medal(i<3?6:8),null,null,null));
        }
        var snapshot=new RatingSnapshot("id","now","v1","EXPERIMENTAL","NOT_VALIDATED",50,charts);
        var user=new PlaydataQueryResults.UserPlaydata("0000-0000-0001","테스트 유저",177750,177770,9813,records);
        var profile=new UserProfileResult("0000-0000-0001","테스트 유저","ミミ","프로필 한마디",null,false,
                177750,180000,9813,0,0,0,0,List.of(
                new UserProfileResult.MedalSummary("clear",49,1,1),
                new UserProfileResult.MedalSummary("full-combo",47,1,1),
                new UserProfileResult.MedalSummary("perfect",43,1,1)),null);
        var renderer=new TierListImageRenderer();
        try {
            byte[] png=renderer.render(snapshot,user,profile,49,RatingController.Metric.CPI);
            assertThat(png).startsWith((byte)0x89,(byte)0x50,(byte)0x4e,(byte)0x47);
            var image=ImageIO.read(new ByteArrayInputStream(png));assertThat(image.getWidth()).isEqualTo(1160);assertThat(image.getHeight()).isGreaterThan(400);
        } finally {renderer.shutdown();}
    }
    @Test void headerCountsIncludeHoldChartsAndIgnoreEmptyStoredRows()throws Exception{
        var held=new ChartRating(3,3,"held","genre",null,49,4,false,"HOLD",List.of("INSUFFICIENT_PLAYERS"),null,10,5,.5,"HOLD",List.of("INSUFFICIENT_PLAYERS"),null,10,95000d,95000d);
        var charts=List.of(chart(1,3),chart(2,2),held);
        var records=List.of(record(1,6,95000),record(2,13,0),record(3,8,90000));
        var byId=new HashMap<Long,PlaydataQueryResults.ChartPlaydata>();records.forEach(r->byId.put(r.chartId(),r));
        assertThat(TierListImageRenderer.summary(charts,byId)).isEqualTo(new TierListImageRenderer.LevelSummary(3,2,1));
        assertThat(TierListImageRenderer.isPlayed(record(4,13,95000))).isTrue();
        assertThat(TierListImageRenderer.isPlayed(record(4,8,0))).isTrue();
        var user=new PlaydataQueryResults.UserPlaydata("0000-0000-0001","User",0,0,0,records);
        var profile=new UserProfileResult("0000-0000-0001","User","", "",null,false,0,0,0,0,0,0,0);
        var renderer=new TierListImageRenderer();
        try {
            var before=new RatingSnapshot("before","now","v1","EXPERIMENTAL","NOT_VALIDATED",50,List.of(chart(1,3),chart(2,2),chart(3,1)));
            var after=new RatingSnapshot("after","now","v1","EXPERIMENTAL","NOT_VALIDATED",50,charts);
            var a=ImageIO.read(new ByteArrayInputStream(renderer.render(before,user,profile,49,RatingController.Metric.CPI)));
            var b=ImageIO.read(new ByteArrayInputStream(renderer.render(after,user,profile,49,RatingController.Metric.CPI)));
            // Eligibility changes partition placement, never the user's level progress.
            assertThat(a.getRGB(0,0,1160,272,null,0,1160)).containsExactly(b.getRGB(0,0,1160,272,null,0,1160));
        } finally {renderer.shutdown();}
    }
    @Test void distinguishesUpperChartsEvenWhenTitlesMatch(){
        var regular=chart(1,1);
        var upper=new ChartRating(2,1,regular.songName(),"genre",null,49,4,true,"ELIGIBLE",List.of(),1d,60,30,.5,"ELIGIBLE",List.of(),1000d,60,95000d,95000d);
        assertThat(TierListImageRenderer.chartTitle(regular)).isEqualTo("song");
        assertThat(TierListImageRenderer.chartTitle(upper)).isEqualTo("Ⓤ song");
    }
    private static PlaydataQueryResults.ChartPlaydata record(long id,int medal,int score){return new PlaydataQueryResults.ChartPlaydata(id,"hash","genre","song",4,"EX",49,29,false,new PlaydataQueryResults.Best(score,9,29),new PlaydataQueryResults.Best(score,9,29),new PlaydataQueryResults.Medal(medal),null,null,null);}
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
        assertThat(TierListImageRenderer.rankIconResource(1)).isEqualTo("/ranks/s-plus.png");
        assertThat(TierListImageRenderer.rankIconResource(9)).isEqualTo("/ranks/b.png");
        assertThat(TierListImageRenderer.rankIconResource(99)).isEqualTo("/ranks/none.png");
    }
    @Test void splitsLargeCatchAllPartitionsAtTheirStrongestRemainingGaps(){
        var charts=new ArrayList<ChartRating>();
        for(int i=0;i<61;i++)charts.add(chart(i+1,100-i));
        var bands=TierListImageRenderer.bands(charts,RatingController.Metric.CPI);
        assertThat(bands).allSatisfy(band->assertThat(band.size()).isLessThanOrEqualTo(20));
        assertThat(bands.stream().flatMap(Collection::stream).toList()).containsExactlyElementsOf(charts);
    }
    private static ChartRating chart(long id,double cpi){return new ChartRating(id,id,"song","genre",null,49,4,false,"ELIGIBLE",List.of(),cpi,60,30,.5,"ELIGIBLE",List.of(),1000d,60,95000d,95000d);}
}
