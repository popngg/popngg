package gg.popn.http.playdata;

import gg.popn.application.playdata.dto.result.PopclassRecalculationResult;
import gg.popn.application.playdata.port.in.RecalculatePopclassUseCase;
import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.http.common.response.SuccessResponse;
import gg.popn.infra.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/playdata/popclass")
@Tag(name = "Popclass", description = "Popclass calculation operations")
public class PopclassRecalculationController {
    private final RecalculatePopclassUseCase useCase;

    @PostMapping("/recalculate")
    @Operation(summary = "Recalculate and store the authenticated user's popclasses")
    public SuccessResponse<PopclassRecalculationResult> recalculate(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Authentication is required.");
        }
        var result = useCase.recalculate(principal.getPoptomoId().getValue());
        return SuccessResponse.<PopclassRecalculationResult>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(result)
                .build();
    }
}
