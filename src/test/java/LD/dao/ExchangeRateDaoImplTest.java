package LD.dao;

import LD.Application;
import LD.model.Currency.Currency;
import LD.model.Scenario.Scenario;
import LD.repository.ExchangeRateRepository;
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
    void getRateAtDate_shouldReturnRate_whenParametersAreCorrect() {
        saveExchangeRatesIntoTestDatabase();
        LocalDate date = LocalDate.of(2020, 2, 28);

        Scenario requiredScenario = testEntitiesKeeper.getScenarios().get(0);
        Currency requiredCurrency = testEntitiesKeeper.getCurrencies().get(0);
        BigDecimal actualExchangeRate = exchangeRateRepository.getRateAtDate(date, requiredScenario, requiredCurrency);
        assertEquals(BigDecimal.ONE, actualExchangeRate.setScale(0));
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/ExchangeRateDaoImplTest/exchangeRateDaoImplTest1.xml")
    void getAverageRateAtDate_shouldReturnRate_whenParametersAreCorrect() {
        saveExchangeRatesIntoTestDatabase();
        LocalDate date = LocalDate.of(2020, 2, 28);

        Scenario requiredScenario = testEntitiesKeeper.getScenarios().get(0);
        Currency requiredCurrency = testEntitiesKeeper.getCurrencies().get(0);
        BigDecimal actualExchangeRate = exchangeRateRepository.getAverageRateAtDate(date, requiredScenario, requiredCurrency);
        assertEquals(BigDecimal.valueOf(2), actualExchangeRate.setScale(0));
    }

    private void saveExchangeRatesIntoTestDatabase() {
        setNullIntoUserId();
        setNullIntoCurrencyId();
        setNullIntoScenarioId();
        testEntityManager.persistAndFlush(testEntitiesKeeper.getUser());
        testEntitiesKeeper.getCurrencies().forEach(c -> testEntityManager.persistAndFlush(c));
        testEntitiesKeeper.getScenarios().forEach(s -> testEntityManager.persistAndFlush(s));
        testEntitiesKeeper.getExRates().forEach(er -> testEntityManager.persistAndFlush(er));
    }

    private void setNullIntoUserId() {
        testEntitiesKeeper.getUser().setId(null);
    }

    private void setNullIntoCurrencyId() {
        testEntitiesKeeper.getCurrencies().forEach(cu -> cu.setId(null));
    }

    private void setNullIntoScenarioId() {
        testEntitiesKeeper.getScenarios().forEach(s -> s.setId(null));
    }
}
