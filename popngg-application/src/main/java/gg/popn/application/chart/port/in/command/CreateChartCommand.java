package gg.popn.application.chart.port.in.command;

import gg.popn.domain.chart.model.field.*;
import org.springframework.boot.autoconfigure.security.SecurityProperties;

import java.util.List;

public record CreateChartCommand(
        GenreName genreName,
        SongName songName,
        List<Level>levels,
        Version version,
        IsUpper isUpper

) { }