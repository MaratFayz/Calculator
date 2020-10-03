package LD.dao;

import LD.Application;
import LD.model.Currency.Currency;
import LD.model.Scenario.Scenario;
import LD.repository.ExchangeRateRepository;
import Utils.TestEntitiesKeeper;
import Utils.XmlDataLoader.LoadXmlFileForLeasingDepositsTest;
import Utils.XmlDataLoader.SaveEntitiesIntoDatabase;
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
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@DataJpaTest
@ContextConfiguration(classes = {Application.class, ExchangeRateDaoImpl.class})
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto = create",
        "spring.flyway.baselineOnMigrate = false"})
public class ExchangeRateDaoImplTest {

    @Autowired
    private TestEntityManager testEntityManager;
    @Autowired
    private ExchangeRateRepository exchangeRateRepository;
    private TestEntitiesKeeper testEntitiesKeeper;

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/ExchangeRateDaoImplTest/exchangeRateDaoImplTest1.xml")
    @SaveEntitiesIntoDatabase
    void getRateAtDate_shouldReturnRate_whenParametersAreCorrect() {
        LocalDate date = LocalDate.of(2020, 2, 28);

        Scenario requiredScenario = testEntitiesKeeper.getScenarios().get(0);
        Currency requiredCurrency = testEntitiesKeeper.getCurrencies().get(0);
        BigDecimal actualExchangeRate = exchangeRateRepository.getRateAtDate(date, requiredScenario, requiredCurrency);
        assertEquals(BigDecimal.ONE, actualExchangeRate.setScale(0));
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/ExchangeRateDaoImplTest/exchangeRateDaoImplTest1.xml")
    @SaveEntitiesIntoDatabase
    void getAverageRateAtDate_shouldReturnRate_whenParametersAreCorrect() {
        LocalDate date = LocalDate.of(2020, 2, 28);

        Scenario requiredScenario = testEntitiesKeeper.getScenarios().get(0);
        Currency requiredCurrency = testEntitiesKeeper.getCurrencies().get(0);
        BigDecimal actualExchangeRate = exchangeRateRepository.getAverageRateAtDate(date, requiredScenario, requiredCurrency);
        assertEquals(BigDecimal.valueOf(2), actualExchangeRate.setScale(0));
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/ExchangeRateDaoImplTest/exchangeRateDaoImplTest1.xml")
    @SaveEntitiesIntoDatabase
    void findMaxDateWithExchangeRateByCurrencyIdAndScenarioId_shouldReturnMaxDateWithExRate_whenValuesInDatabase() {
        LocalDate maxDateScenarioId1CurrencyId1 = exchangeRateRepository.findMaxDateWithExchangeRateByCurrencyIdAndScenarioId(
                1L, 1L);
        System.out.println("maxDateScenarioId1CurrencyId1 => " + maxDateScenarioId1CurrencyId1);
        assertEquals(LocalDate.of(9999,2,1), maxDateScenarioId1CurrencyId1);

        LocalDate maxDateScenarioId1CurrencyId2 = exchangeRateRepository.findMaxDateWithExchangeRateByCurrencyIdAndScenarioId(
                2L, 2L);
        System.out.println("maxDateScenarioId1CurrencyId1 => " + maxDateScenarioId1CurrencyId2);
        assertEquals(LocalDate.of(8888,2,1), maxDateScenarioId1CurrencyId2);

        LocalDate maxDateScenarioId1CurrencyId3 = exchangeRateRepository.findMaxDateWithExchangeRateByCurrencyIdAndScenarioId(
                1L, 2L);
        System.out.println("maxDateScenarioId1CurrencyId3 => " + maxDateScenarioId1CurrencyId3);
        assertEquals(LocalDate.of(7777,2,1), maxDateScenarioId1CurrencyId3);

        LocalDate maxDateScenarioId1CurrencyId4 = exchangeRateRepository.findMaxDateWithExchangeRateByCurrencyIdAndScenarioId(
                2L, 1L);
        System.out.println("maxDateScenarioId1CurrencyId4 => " + maxDateScenarioId1CurrencyId4);
        assertEquals(LocalDate.of(6666,2,1), maxDateScenarioId1CurrencyId4);
    }
}