package gg.popn.domain.playdata.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CountPlaydataRequest {
    @NotNull(message = "groupBy 파라미터는 필수입니다.")
    GroupByOption groupBy;

    @NotNull(message = "target 파라미터는 필수입니다.")
    TargetOption target;
}
