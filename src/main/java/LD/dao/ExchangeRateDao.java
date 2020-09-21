package LD.dao;

import LD.model.Currency.Currency;
import LD.model.Scenario.Scenario;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@Component
public interface ExchangeRateDao {

    BigDecimal getRateAtDate(LocalDate date, Scenario scenario, Currency currency);

    BigDecimal getAverageRateAtDate(LocalDate date, Scenario scenario, Currency currency);
}
