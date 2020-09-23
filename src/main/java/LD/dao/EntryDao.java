package LD.dao;

import LD.model.Scenario.Scenario;

import java.util.List;

public interface EntryDao {

    List<Object[]> getActiveEntriesForScenarioAndFirstOpenPeriodRegLd1(Scenario scenario);

    List<Object[]> getActiveEntriesForScenarioAndFirstOpenPeriodRegLd2(Scenario scenario);

    List<Object[]> getActiveEntriesForScenarioAndFirstOpenPeriodRegLd3(Scenario scenario);
}
