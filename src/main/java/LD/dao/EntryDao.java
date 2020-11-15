package LD.dao;

import LD.model.Entry.EntryDTO_out_RegLD1;
import LD.model.Entry.EntryDTO_out_RegLD2;
import LD.model.Entry.EntryDTO_out_RegLD3;

import java.util.List;

public interface EntryDao {

    List<EntryDTO_out_RegLD1> getActiveEntriesForScenarioAndFirstOpenPeriodRegLd1(Long scenarioId);

    List<EntryDTO_out_RegLD2> getActiveEntriesForScenarioAndFirstOpenPeriodRegLd2(Long scenarioId);

    List<EntryDTO_out_RegLD3> getActiveEntriesForScenarioAndFirstOpenPeriodRegLd3(Long scenarioId);
}
