package gg.popn.application.playdata.port.in;

import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import gg.popn.application.playdata.dto.result.ImportPlaydataResult;

public interface ImportPlaydataUseCase {
    ImportPlaydataResult importPlaydata(ImportPlaydataCommand command);
}
