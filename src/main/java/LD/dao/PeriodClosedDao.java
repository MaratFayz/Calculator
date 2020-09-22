package LD.dao;

import LD.model.Scenario.Scenario;

import java.time.LocalDate;

public interface PeriodClosedDao {

    LocalDate findFirstOpenPeriodByScenario(Scenario scenario);
}
