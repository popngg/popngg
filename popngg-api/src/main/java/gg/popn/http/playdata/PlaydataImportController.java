package gg.popn.http.playdata;

import gg.popn.application.playdata.port.in.ImportPlaydataUseCase;
import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.http.common.response.SuccessResponse;
import gg.popn.http.playdata.request.ImportPlaydataRequest;
import gg.popn.http.playdata.response.ImportPlaydataResponse;
import gg.popn.infra.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/playdata")
public class PlaydataImportController {
    private final ImportPlaydataUseCase importPlaydata;

    @PostMapping("/imports")
    public SuccessResponse<ImportPlaydataResponse> importPlaydata(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody ImportPlaydataRequest request) {
        if (principal == null) {
            throw new IllegalArgumentException("Authentication is required.");
        }
        var result = importPlaydata.importPlaydata(
                request.toCommand(principal.getPoptomoId().getValue()));
        return SuccessResponse.<ImportPlaydataResponse>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(ImportPlaydataResponse.from(result))
                .build();
    }
}
