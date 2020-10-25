package LD.service.Calculators.LeasingDeposits;

import LD.config.Security.model.User.User;
import LD.model.IFRSAccount.IFRSAccount;
import LD.model.Scenario.Scenario;

import java.time.LocalDate;
import java.util.List;

public interface CalculationParametersSource {

    Scenario getScenarioFrom();

    Scenario getScenarioTo();

    LocalDate getFirstOpenPeriodOfScenarioTo();

    LocalDate getEntriesCopyDateFromScenarioFromToScenarioTo();

    LocalDate getFirstOpenPeriodOfScenarioFrom();

    User getUser();

    List<IFRSAccount> getAllIfrsAccounts();
}
