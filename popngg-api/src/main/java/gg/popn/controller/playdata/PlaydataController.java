package gg.popn.controller.playdata;

import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.domain.playdata.application.dto.request.CountPlaydataRequest;
import gg.popn.domain.playdata.application.dto.request.GetMainPlaydataRequest;
import gg.popn.domain.playdata.application.dto.request.PostPlaydataRequest;
import gg.popn.domain.playdata.application.dto.response.CountDto;
import gg.popn.domain.playdata.application.dto.response.MainPlaydataDto;
import gg.popn.domain.playdata.application.dto.response.UserWithPlaydataDto;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.model.response.SuccessResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/playdata")
@Tag(name = "Playdata", description = "Playdata operations")
public class PlaydataController {

    @PostMapping("")
    public SuccessResponse<Void> postPlaydata(@RequestBody PostPlaydataRequest request) {
        return SuccessResponse.<Void>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(null) // TODO: implement
                .build();
    }

    @GetMapping("/count/{poptomoId}")
    public SuccessResponse<CountDto> countPlaydata(
            @Valid @ModelAttribute CountPlaydataRequest request,
            @PathVariable PoptomoId poptomoId) {
        return SuccessResponse.<CountDto>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(null) // TODO: implement
                .build();
    }

    @GetMapping("/popclass/{poptomoId}")
    public SuccessResponse<UserWithPlaydataDto> getPopclassData(
            @PathVariable PoptomoId poptomoId) {
        return SuccessResponse.<UserWithPlaydataDto>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(null) // TODO: implement
                .build();
    }

    @GetMapping("/all/{poptomoId}")
    public SuccessResponse<UserWithPlaydataDto> getAllPlaydata(
            @PathVariable PoptomoId poptomoId) {
        return SuccessResponse.<UserWithPlaydataDto>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(null) // TODO: implement
                .build();
    }

    @GetMapping("/main")
    public SuccessResponse<MainPlaydataDto> getMainData(
            @Valid @ModelAttribute GetMainPlaydataRequest request,
            @PathVariable PoptomoId poptomoId) {
        return SuccessResponse.<MainPlaydataDto>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(null) // TODO: implement
                .build();
    }

}
