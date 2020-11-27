package LD.dao;

import LD.Application;
import LD.model.Company.Company;
import LD.model.Currency.Currency;
import LD.model.Scenario.Scenario;
import LD.repository.DepositRatesRepository;
import Utils.TestEntitiesKeeper;
import Utils.XmlDataLoader.LoadXmlFileForLeasingDepositsTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.*;

import static LD.service.Calculators.LeasingDeposits.SupportEntryCalculator.calculateDurationInDaysBetween;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@DataJpaTest
@ContextConfiguration(classes = {Application.class, DepositRateDaoImpl.class})
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto = create",
        "spring.flyway.baselineOnMigrate = false"})
public class DepositRateDaoImplTest {

    @Autowired
    private TestEntityManager testEntityManager;
    @Autowired
    private DepositRatesRepository depositRatesRepository;
    private TestEntitiesKeeper testEntitiesKeeper;

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/DepositRateDaoImplTest/depositRateDaoImplTest1.xml")
    void getRateByCompanyMonthDurationCurrencyStartDateScenario_shouldReturn5_whenFactDuration30MonthUsdBefore2018() {
        LocalDate startDate = LocalDate.of(2017, 3, 10);
        LocalDate endDate = LocalDate.of(2019, 10, 20);
        int durationMonth = (int) (calculateDurationInDaysBetween(startDate, endDate) / 30.4);

        Company requiredCompany = testEntitiesKeeper.getCompany();
        Scenario requiredScenario = testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getId() == 1).findFirst().get();
        Currency requiredCurrency = testEntitiesKeeper.getCurrencies().stream().filter(c -> c.getId() == 1).findFirst().get();

        saveDepositRatesIntoTestDatabase();

        BigDecimal actualExchangeRate = depositRatesRepository.getRateByCompanyMonthDurationCurrencyStartDateScenario(requiredCompany,
                durationMonth,
                requiredCurrency,
                startDate,
                requiredScenario);
        assertEquals(BigDecimal.valueOf(5), actualExchangeRate.setScale(0));
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/DepositRateDaoImplTest/depositRateDaoImplTest1.xml")
    void getRateByCompanyMonthDurationCurrencyStartDateScenario_shouldReturn5000_whenFactDuration30MonthUsdIn2018() {
        LocalDate startDate = LocalDate.of(2018, 3, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 20);
        int durationMonth = (int) (calculateDurationInDaysBetween(startDate, endDate) / 30.4);

        Company requiredCompany = testEntitiesKeeper.getCompany();
        Scenario requiredScenario = testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getId() == 1).findFirst().get();
        Currency requiredCurrency = testEntitiesKeeper.getCurrencies().stream().filter(c -> c.getId() == 1).findFirst().get();

        saveDepositRatesIntoTestDatabase();

        BigDecimal actualExchangeRate = depositRatesRepository.getRateByCompanyMonthDurationCurrencyStartDateScenario(requiredCompany,
                durationMonth,
                requiredCurrency,
                startDate,
                requiredScenario);
        assertEquals(BigDecimal.valueOf(5000), actualExchangeRate.setScale(0));
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/DepositRateDaoImplTest/depositRateDaoImplTest1.xml")
    void getRateByCompanyMonthDurationCurrencyStartDateScenario_shouldReturn300_whenPlanDuration30MonthUsdIn2018() {
        LocalDate startDate = LocalDate.of(2018, 3, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 20);
        int durationMonth = (int) (calculateDurationInDaysBetween(startDate, endDate) / 30.4);

        Company requiredCompany = testEntitiesKeeper.getCompany();
        Scenario requiredScenario = testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getId() == 2).findFirst().get();
        Currency requiredCurrency = testEntitiesKeeper.getCurrencies().stream().filter(sc -> sc.getId() == 1).findFirst().get();

        saveDepositRatesIntoTestDatabase();

        BigDecimal actualExchangeRate = depositRatesRepository.getRateByCompanyMonthDurationCurrencyStartDateScenario(requiredCompany,
                durationMonth,
                requiredCurrency,
                startDate,
                requiredScenario);
        assertEquals(BigDecimal.valueOf(300), actualExchangeRate.setScale(0));
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/DepositRateDaoImplTest/depositRateDaoImplTest1.xml")
    void getRateByCompanyMonthDurationCurrencyStartDateScenario_shouldReturnMinus200_whenPlanDurationLess12MonthUsdBefore2018() {
        LocalDate startDate = LocalDate.of(2017, 1, 10);
        LocalDate endDate = LocalDate.of(2017, 12, 20);
        int durationMonth = (int) (calculateDurationInDaysBetween(startDate, endDate) / 30.4);

        Company requiredCompany = testEntitiesKeeper.getCompany();
        Scenario requiredScenario = testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getId() == 2).findFirst().get();
        Currency requiredCurrency = testEntitiesKeeper.getCurrencies().stream().filter(sc -> sc.getId() == 1).findFirst().get();

        saveDepositRatesIntoTestDatabase();

        BigDecimal actualExchangeRate = depositRatesRepository.getRateByCompanyMonthDurationCurrencyStartDateScenario(requiredCompany,
                durationMonth,
                requiredCurrency,
                startDate,
                requiredScenario);
        assertEquals(BigDecimal.valueOf(-200), actualExchangeRate.setScale(0));
    }

    private void saveDepositRatesIntoTestDatabase() {
        setNullIntoUserId();
        setNullIntoCompanyId();
        setNullIntoCurrencyId();
        setNullIntoScenarioId();
        setNullIntoDurationId();

        testEntityManager.persistAndFlush(testEntitiesKeeper.getUser());
        testEntityManager.persistAndFlush(testEntitiesKeeper.getCompany());
        testEntitiesKeeper.getCurrencies().forEach(c -> testEntityManager.persistAndFlush(c));
        testEntitiesKeeper.getScenarios().forEach(s -> testEntityManager.persistAndFlush(s));
        testEntitiesKeeper.getDurations().forEach(d -> testEntityManager.persistAndFlush(d));
        testEntitiesKeeper.getDepositRates().forEach(dr -> testEntityManager.persistAndFlush(dr));
    }

    private void setNullIntoUserId() {
        testEntitiesKeeper.getUser().setId(null);
    }

    private void setNullIntoCompanyId() {
        testEntitiesKeeper.getCompany().setId(null);
    }

    private void setNullIntoCurrencyId() {
        testEntitiesKeeper.getCurrencies().forEach(cu -> cu.setId(null));
    }

    private void setNullIntoScenarioId() {
        testEntitiesKeeper.getScenarios().forEach(s -> s.setId(null));
    }

    private void setNullIntoDurationId() {
        testEntitiesKeeper.getDurations().forEach(d -> d.setId(null));
    }
}
