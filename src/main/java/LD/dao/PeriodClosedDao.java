package LD.dao;

import LD.model.Period.Period;
import LD.model.Scenario.Scenario;

import java.time.LocalDate;

public interface PeriodClosedDao {

    LocalDate findFirstOpenPeriodDateByScenario(Scenario scenario);

    Period findFirstOpenPeriodByScenario(Scenario scenario);
}
