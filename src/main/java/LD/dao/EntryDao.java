package LD.dao;

import LD.model.Entry.EntryDTO_out_RegLD1;
import LD.model.Entry.EntryDTO_out_RegLD2;
import LD.model.Entry.EntryDTO_out_RegLD3;
import LD.model.Scenario.Scenario;

import java.util.List;

public interface EntryDao {

    List<EntryDTO_out_RegLD1> getActiveEntriesForScenarioAndFirstOpenPeriodRegLd1(Scenario scenario);

    List<EntryDTO_out_RegLD2> getActiveEntriesForScenarioAndFirstOpenPeriodRegLd2(Scenario scenario);

    List<EntryDTO_out_RegLD3> getActiveEntriesForScenarioAndFirstOpenPeriodRegLd3(Scenario scenario);
}
