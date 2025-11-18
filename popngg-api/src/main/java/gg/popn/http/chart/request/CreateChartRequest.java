package gg.popn.http.chart.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChartRequest {

    @Schema(description = "Genre name", example = "THX 4")
    @NotBlank(message = "genreName is required.")
    private String genreName;

    @Schema(description = "Song title", example = "THX 4")
    @NotBlank(message = "songName is required.")
    private String songName;

    @Schema(description = "Available chart levels", example = "[19, 34, 44, 50]")
    @NotNull(message = "levels cannot be null.")
    @Size(min = 1, message = "levels must include at least one level.")
    private List<Integer> levels;

    @Schema(description = "Version ID (Pop'n Music version number)", example = "28")
    @NotNull(message = "version is required.")
    private Integer version;

    @Schema(
            description = "Upper chart indicator (1 = upper chart, 0 = normal chart). " +
                    "Used to distinguish UPPER charts in Pop'n Music.",
            example = "0"
    )
    @NotNull(message = "isUpper cannot be null.")
    private Integer isUpper;
}
