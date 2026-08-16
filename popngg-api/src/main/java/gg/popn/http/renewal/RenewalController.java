package gg.popn.http.renewal;
import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import gg.popn.application.playdata.port.in.ImportPlaydataUseCase;
import gg.popn.domain.common.*;
import gg.popn.http.common.response.SuccessResponse;
import gg.popn.infra.security.CustomUserPrincipal;
import jakarta.validation.Valid; import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value; import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*; import java.util.Locale;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/renewals")
public class RenewalController {
    private final ImportPlaydataUseCase importPlaydata;
    @Value("${popngg.renewal.collector-version:1}") private int collectorVersion;
    @Value("${popngg.renewal.game:popn29}") private String supportedGame;
    @PostMapping public SuccessResponse<RenewalResponse> renew(@AuthenticationPrincipal CustomUserPrincipal principal,@Valid @RequestBody RenewalRequest request){
        if(principal==null) throw error(HttpStatus.UNAUTHORIZED,"UNAUTHENTICATED","Authentication is required.");
        if(!supportedGame.equals(request.game())) throw error(HttpStatus.UNPROCESSABLE_ENTITY,"UNSUPPORTED_GAME","Unsupported game.");
        if(request.collectorVersion()!=collectorVersion) throw error(HttpStatus.UNPROCESSABLE_ENTITY,"UNSUPPORTED_COLLECTOR_VERSION","Unsupported collector version.");
        String id=principal.getPoptomoId().getValue();
        if(!id.equals(request.profile().gameId())) throw error(HttpStatus.FORBIDDEN,"GAME_ID_MISMATCH","The collected game ID does not match the authenticated user.");
        var rows=request.charts().stream().map(this::toRow).toList();
        var profile=new ImportPlaydataCommand.ProfileSnapshot(request.profile().name(),request.profile().character(),null,null,null,null);
        var result=importPlaydata.importPlaydata(new ImportPlaydataCommand(id,profile,rows));
        return SuccessResponse.<RenewalResponse>builder().code(ResponseCode.SUCCESS).message(ResponseMessage.SUCCESS).data(RenewalResponse.from(result)).build();
    }
    private ImportPlaydataCommand.Row toRow(RenewalRequest.Chart c){Long id=null;if(c.getChartId()!=null&&!c.getChartId().isBlank())try{id=Long.valueOf(c.getChartId());}catch(NumberFormatException ignored){}
        return new ImportPlaydataCommand.Row(id,null,difficulty(c.getDifficulty()),false,null,c.getTitle(),c.getGenre(),c.getScore(),rank(c.getRank()),medal(c.getMedal()),c.getVersionBestScore(),c.isVersionBestScorePresent());}
    private int difficulty(String v){return switch(v.toLowerCase(Locale.ROOT)){case"l","light","easy"->1;case"n","normal"->2;case"h","hyper"->3;case"ex"->4;default->throw error(HttpStatus.UNPROCESSABLE_ENTITY,"UNKNOWN_DIFFICULTY","Unknown difficulty code: "+v);};}
    private int rank(String v){return switch(v.toLowerCase(Locale.ROOT)){case"s"->2;case"a3"->3;case"a2"->5;case"a1"->7;case"b"->9;case"c"->10;case"d"->11;case"e"->12;default->throw error(HttpStatus.UNPROCESSABLE_ENTITY,"UNKNOWN_RANK_CODE","Unknown rank code: "+v);};}
    private int medal(String v){if("none".equalsIgnoreCase(v))throw error(HttpStatus.UNPROCESSABLE_ENTITY,"UNKNOWN_MEDAL_CODE","Played charts must have a medal.");if(v.length()==1&&v.charAt(0)>='a'&&v.charAt(0)<='k')return v.charAt(0)-'a'+1;throw error(HttpStatus.UNPROCESSABLE_ENTITY,"UNKNOWN_MEDAL_CODE","Unknown medal code: "+v);}
    private static RenewalException error(HttpStatus s,String c,String m){return new RenewalException(s,c,m);}
}
