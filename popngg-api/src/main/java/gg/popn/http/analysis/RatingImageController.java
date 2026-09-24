package gg.popn.http.analysis;

import gg.popn.application.analysis.RatingQuery;
import gg.popn.application.playdata.port.in.PlaydataQueryUseCase;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ratings/charts/image")
public class RatingImageController {
    private final RatingQuery ratings;private final PlaydataQueryUseCase playdata;private final TierListImageRenderer renderer;
    public RatingImageController(RatingQuery ratings,PlaydataQueryUseCase playdata,TierListImageRenderer renderer){this.ratings=ratings;this.playdata=playdata;this.renderer=renderer;}
    @GetMapping(produces=MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<?> image(@RequestParam int level,@RequestParam(defaultValue="CPI") RatingController.Metric metric,@RequestParam String poptomoId){
        if(level<48||level>50)return error(HttpStatus.BAD_REQUEST,"UNSUPPORTED_LEVEL");
        if(!poptomoId.matches("^\\d{4}-\\d{4}-\\d{4}$"))return error(HttpStatus.BAD_REQUEST,"INVALID_POPTOMO_ID");
        try{
            byte[] png=renderer.render(ratings.latest(),playdata.findUserPlaydata(poptomoId),level,metric);
            String filename="popngg-lv"+level+"-"+metric.name().toLowerCase()+"-"+poptomoId+".png";
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+filename+"\"").body(png);
        }catch(IllegalArgumentException e){return error(HttpStatus.NOT_FOUND,"USER_NOT_FOUND");}
        catch(IllegalStateException e){return error(HttpStatus.SERVICE_UNAVAILABLE,"RATINGS_NOT_READY");}
        catch(Exception e){return error(HttpStatus.INTERNAL_SERVER_ERROR,"IMAGE_RENDER_FAILED");}
    }
    private static ResponseEntity<byte[]> error(HttpStatus status,String code){return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(("{\"error\":\""+code+"\"}").getBytes(StandardCharsets.UTF_8));}
}
