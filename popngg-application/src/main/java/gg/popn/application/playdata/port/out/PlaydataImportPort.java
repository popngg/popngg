package gg.popn.application.playdata.port.out;

import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import gg.popn.application.playdata.dto.result.ImportPlaydataResult;

public interface PlaydataImportPort {
    ImportPlaydataResult execute(ImportPlaydataCommand command);
}
