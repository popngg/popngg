package gg.popn.http.analysis;

import gg.popn.application.analysis.AchievementConstants;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AchievementConstantImageRendererTest {
    @Test void rendersChartsWithHeldReferenceConstantsForBothAxes()throws Exception{
        var renderer=new AchievementConstantImageRenderer();
        try{
            for(var axis:AchievementConstants.Axis.values()){
                String target=axis==AchievementConstants.Axis.MEDAL?"CLEAR":"AAA";
                var held=new AchievementConstants.Constant(target,1d,null,null,null,20,10,
                        "HOLD",List.of("INSUFFICIENT_PLAYERS"));
                var ready=new AchievementConstants.Constant(target,1d,49d,48.9,49.1,100,50,
                        "EXPERIMENTAL",List.of());
                var charts=List.of(
                        new AchievementConstants.Chart(1,"held","genre",null,49,4,false,
                                false,false,"NONE",List.of(held)),
                        new AchievementConstants.Chart(2,"ready","genre",null,49,4,false,
                                false,false,"NONE",List.of(ready)));
                var snapshot=new AchievementConstants.Snapshot(1,"source","achievement-v1","EXPERIMENTAL",
                        Instant.parse("2026-09-25T00:00:00Z"),axis,49,charts);
                byte[] png=renderer.render(snapshot);
                assertThat(ImageIO.read(new ByteArrayInputStream(png)).getWidth()).isEqualTo(1160);
            }
        }finally{renderer.close();}
    }

    @Test void rendersFiveConstantsForEveryChartAsPng()throws Exception{
        var charts=new ArrayList<AchievementConstants.Chart>();
        for(int chart=0;chart<12;chart++){
            var values=new ArrayList<AchievementConstants.Constant>();
            for(int target=0;target<5;target++)values.add(new AchievementConstants.Constant(
                    List.of("CLEAR","BRONZE_DIAMOND","BRONZE_STAR","FULL_COMBO","PERFECT").get(target),
                    1d,chart==11&&target==4?null:48d+chart/20d+target/10d,null,null,100,50,
                    chart==11&&target==4?"HOLD":"EXPERIMENTAL",chart==11&&target==4?List.of("INSUFFICIENT_ACHIEVERS"):List.of()));
            charts.add(new AchievementConstants.Chart(chart+1,"곡 "+chart,"장르",null,49,4,false,
                    chart==0,chart==1,chart==2?"EXTRA":"NONE",values));
        }
        var snapshot=new AchievementConstants.Snapshot(1,"source","achievement-v1","EXPERIMENTAL",
                Instant.parse("2026-09-25T00:00:00Z"),AchievementConstants.Axis.MEDAL,49,charts);
        var renderer=new AchievementConstantImageRenderer();
        try{
            byte[] png=renderer.render(snapshot);
            assertThat(png).startsWith((byte)0x89,(byte)0x50,(byte)0x4e,(byte)0x47);
            var image=ImageIO.read(new ByteArrayInputStream(png));
            assertThat(image.getWidth()).isEqualTo(1160);assertThat(image.getHeight()).isGreaterThan(500);
        }finally{renderer.close();}
    }
}
