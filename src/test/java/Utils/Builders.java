package Utils;

import LD.config.Security.model.User.User;
import LD.model.Company.Company;
import LD.model.Counterpartner.Counterpartner;
import LD.model.Currency.Currency;
import LD.model.DepositRate.DepositRate;
import LD.model.DepositRate.DepositRateID;
import LD.model.Duration.Duration;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.ExchangeRate.ExchangeRate;
import LD.model.ExchangeRate.ExchangeRateID;
import LD.model.Period.Period;
import LD.model.Scenario.Scenario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

public class Builders {

    public static Scenario getSC(String name, ScenarioStornoStatus status, User user) {
        Scenario c = new Scenario();
        c.setName(name);
        c.setStatus(status);
        c.setLastChange(ZonedDateTime.now());
        c.setUserLastChanged(user);
        return c;
    }

    public static User getAnyUser() {
        User user = User.builder()
                .username("1")
                .password("2")
                .build();

        user.setLastChange(ZonedDateTime.now());
        return user;
    }

    public static LocalDate getDate(int day, int month, int year) {
        return LocalDate.of(year, month, day);
    }

    public static Period getPer(int day, int month, int year) {
        Period period = new Period();
        period.setDate(getDate(day, month, year));
        period.setLastChange(ZonedDateTime.now());

        return period;
    }

    public static Counterpartner getCP(String name) {
        Counterpartner cp = new Counterpartner();
        cp.setName(name);
        cp.setLastChange(ZonedDateTime.now());
        return cp;
    }

    public static Company getEN(String code, String name) {
        Company en = new Company();
        en.setCode(code);
        en.setName(name);
        en.setLastChange(ZonedDateTime.now());
        return en;
    }

    public static Currency getCUR(String name) {
        Currency c = new Currency();
        c.setShort_name(name);
        c.setName(name);
        c.setLastChange(ZonedDateTime.now());
        return c;
    }

    public static Duration getDur(String name, int minMonths, int maxMonths) {
        Duration duration = new Duration();
        duration.setName(name);
        duration.setMAX_MONTH(maxMonths);
        duration.setMIN_MONTH(minMonths);

        return duration;
    }

    public static DepositRate getDepRate(Company company,
                                         LocalDate START_PERIOD,
                                         LocalDate END_PERIOD,
                                         Currency currency,
                                         Duration duration,
                                         Scenario scenario,
                                         BigDecimal rate) {
        DepositRateID depositRateID = DepositRateID.builder()
                .company(company)
                .currency(currency)
                .duration(duration)
                .END_PERIOD(END_PERIOD)
                .START_PERIOD(START_PERIOD)
                .scenario(scenario)
                .build();

        DepositRate depositRate = DepositRate.builder()
                .depositRateID(depositRateID)
                .RATE(rate)
                .build();

        return depositRate;
    }

    public static ExchangeRate getExRate(Scenario scenario, LocalDate date, Currency currency,
                                         BigDecimal rate_at_date,
                                         BigDecimal average_rate_for_month) {
        ExchangeRateID exRID = ExchangeRateID.builder()
                .currency(currency)
                .scenario(scenario)
                .date(date)
                .build();

        ExchangeRate exchange_rate = new ExchangeRate();
        exchange_rate.setExchangeRateID(exRID);
        exchange_rate.setRate_at_date(rate_at_date);
        exchange_rate.setAverage_rate_for_month(average_rate_for_month);

        return exchange_rate;
    }
}
