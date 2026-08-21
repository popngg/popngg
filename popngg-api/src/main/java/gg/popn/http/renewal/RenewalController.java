package gg.popn.http.renewal;
import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import gg.popn.application.playdata.port.in.ImportPlaydataUseCase;
import gg.popn.domain.common.*;
import gg.popn.http.common.response.SuccessResponse;
import gg.popn.infra.security.CustomUserPrincipal;
import jakarta.validation.Valid; import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value; import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*; import java.util.Locale; import java.util.regex.Pattern;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/renewals")
public class RenewalController {
    private static final Pattern UPPER_SUFFIX = Pattern.compile("\\s*\\(UPPER\\)\\s*$", Pattern.CASE_INSENSITIVE);
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
        boolean upper=hasUpperSuffix(c.getTitle())||hasUpperSuffix(c.getGenre());String title=withoutUpperSuffix(c.getTitle());String genre=withoutUpperSuffix(c.getGenre());
        return new ImportPlaydataCommand.Row(id,null,difficulty(c.getDifficulty()),upper,null,title,genre,c.getScore(),rank(c.getRank()),medal(c.getMedal()),c.getVersionBestScore(),c.isVersionBestScorePresent(),c.getArtist());}
    private static boolean hasUpperSuffix(String value){return UPPER_SUFFIX.matcher(value).find();}
    private static String withoutUpperSuffix(String value){return UPPER_SUFFIX.matcher(value).replaceFirst("").strip();}
    private int difficulty(String v){return switch(v.toLowerCase(Locale.ROOT)){case"l","light","easy"->1;case"n","normal"->2;case"h","hyper"->3;case"ex"->4;default->throw error(HttpStatus.UNPROCESSABLE_ENTITY,"UNKNOWN_DIFFICULTY","Unknown difficulty code: "+v);};}
    private int rank(String v){return switch(v.toLowerCase(Locale.ROOT)){
        case"s_plus"->1;
        case"s"->2;
        case"aaa","a3"->3;
        case"aa_plus","a2_plus"->4;
        case"aa","a2"->5;
        case"a_plus","a1_plus"->6;
        case"a","a1"->7;
        case"b_plus"->8;
        case"b"->9;
        case"c"->10;
        case"d"->11;
        case"e"->12;
        case"none"->13;
        default->throw error(HttpStatus.UNPROCESSABLE_ENTITY,"UNKNOWN_RANK_CODE","Unknown rank code: "+v);
    };}
    private int medal(String v){return switch(v.toLowerCase(Locale.ROOT)){
        case"none"->13;
        case"a"->1;
        case"b"->2;
        case"c"->3;
        case"d"->4;
        case"e"->5;
        case"f"->6;
        case"g"->7;
        case"h"->8;
        case"i"->9;
        case"j"->10;
        case"k"->11;
        case"l"->12;
        default->throw error(HttpStatus.UNPROCESSABLE_ENTITY,"UNKNOWN_MEDAL_CODE","Unknown medal code: "+v);
    };}
    private static RenewalException error(HttpStatus s,String c,String m){return new RenewalException(s,c,m);}
}
