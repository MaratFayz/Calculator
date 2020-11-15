package LD.dao;

import LD.model.Company.Company;
import LD.model.Currency.Currency;
import LD.model.Scenario.Scenario;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DepositRateDao {

    BigDecimal getRateByCompanyMonthDurationCurrencyStartDateScenario(Company company, Integer durationMonth,
                                                                      Currency currency, LocalDate depositStartDate,
                                                                      Scenario scenario);
}
