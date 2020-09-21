package LD.service.Calculators.LeasingDeposits;

import LD.config.Security.model.User.User;
import LD.model.IFRSAccount.IFRSAccount;
import LD.model.Scenario.Scenario;

import java.time.LocalDate;
import java.util.List;

public interface CalculationParametersSource {

    void prepareParameters(LocalDate copyDate, Long scenarioFrom_id, Long scenarioTo_id);

    Scenario getScenarioFrom();

    Scenario getScenarioTo();

    LocalDate getFirstOpenPeriod_ScenarioTo();

    LocalDate getPeriod_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo();

    LocalDate getFirstOpenPeriod_ScenarioFrom();

    User getUser();

    List<IFRSAccount> getAllIFRSAccounts();
}
