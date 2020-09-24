package LD.dao;

import LD.Application;
import LD.model.EntryIFRSAcc.EntryIFRSAccDTO_out_form;
import LD.model.Period.Period;
import LD.model.Scenario.Scenario;
import LD.repository.EntryIFRSAccRepository;
import LD.repository.PeriodsClosedRepository;
import LD.repository.ScenarioRepository;
import Utils.TestEntitiesKeeper;
import Utils.XmlDataLoader.LoadXmlFileForLeasingDepositsTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@DataJpaTest
@ContextConfiguration(classes = {Application.class, EntryIfrsAccDaoImpl.class})
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto = create",
        "spring.flyway.baselineOnMigrate = false"})
public class EntryIfrsAccDaoImplTest {

    @Autowired
    private TestEntityManager testEntityManager;
    @Autowired
    private EntryIFRSAccRepository entryIFRSAccRepository;
    private TestEntitiesKeeper testEntitiesKeeper;
    @MockBean
    private ScenarioRepository scenarioRepository;
    @MockBean
    private PeriodsClosedRepository periodClosedRepository;

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/EntryIfrsAccDaoImplTest/entryIfrsAccDaoImplTest1.xml")
    public void sumActualEntriesIfrs_shouldReturn3Rows_when6IfrsEntriesAnd3IfrsAccounts() {
        saveDataIntoTestDatabase();

        Scenario scenario = testEntitiesKeeper.getScenarios().get(0);
        when(scenarioRepository.findById(anyLong())).thenReturn(Optional.of(scenario));
        Period period = testEntitiesKeeper.getPeriods().get(0);
        when(periodClosedRepository.findFirstOpenPeriodByScenario(eq(scenario))).thenReturn(period);

        EntryIFRSAccDTO_out_form expectedResult1 = EntryIFRSAccDTO_out_form.builder()
                .period("2020-09-30")
                .scenario("FACT")
                .account_code("A0203010100")
                .account_name("Долгосрочные депозиты по аренде ВС - основная сумма")
                .flow_code("F2000")
                .flow_name("Поступление")
                .ct("RUB")
                .dr("-")
                .pa("THP99")
                .sh("-")
                .sum(BigDecimal.valueOf(5).setScale(10))
                .build();
        EntryIFRSAccDTO_out_form expectedResult2 = EntryIFRSAccDTO_out_form.builder()
                .period("2020-09-30")
                .scenario("FACT")
                .account_code("A0208010000")
                .account_name("Долгосрочные авансы выданные")
                .flow_code("F2000")
                .flow_name("Поступление")
                .ct("RUB")
                .dr("-")
                .pa("THP99")
                .sh("-")
                .sum(BigDecimal.valueOf(10).setScale(10))
                .build();
        EntryIFRSAccDTO_out_form expectedResult3 = EntryIFRSAccDTO_out_form.builder()
                .period("2020-09-30")
                .scenario("FACT")
                .account_code("A0208010000")
                .account_name("Долгосрочные авансы выданные")
                .flow_code("F2000")
                .flow_name("Поступление")
                .ct("RUB")
                .dr("-")
                .pa("THP99")
                .sh("-")
                .sum(BigDecimal.valueOf(5).setScale(10))
                .build();

        List<EntryIFRSAccDTO_out_form> actualEntriesIfrs = entryIFRSAccRepository.sumActualEntriesIfrs(1L);
        actualEntriesIfrs.forEach(e -> {
            System.out.println("Строка рассчитанного отчёта: " + e);
        });

        System.out.println("");

        System.out.println("Строка ожидаемого отчёта: " + expectedResult1);
        System.out.println("Строка ожидаемого отчёта: " + expectedResult2);
        System.out.println("Строка ожидаемого отчёта: " + expectedResult3);

        assertEquals(3, actualEntriesIfrs.size());
        assertTrue(actualEntriesIfrs.contains(expectedResult1));
        assertTrue(actualEntriesIfrs.contains(expectedResult2));
        assertTrue(actualEntriesIfrs.contains(expectedResult3));
    }

    private void saveDataIntoTestDatabase() {
        setNullIntoUserId();
        setNullIntoCompanyId();
        setNullIntoCounterpartnerId();
        setNullIntoCurrencyId();
        setNullIntoScenarioId();
        setNullIntoDurationId();
        setNullIdPeriod();
        setLastChangeDateIntoPeriod();
        setNullIdIfrsAccount();
        setLastChangeDateIntoIfrsAccount();
        setNullIdIfrsAccount();
        setLastChangeDateIntoLeasingDeposit();
        setNullIdLeasingDeposit();
        setLastChangeDateIntoEntriesForIfrsSumDaoTest();

        testEntityManager.persistAndFlush(testEntitiesKeeper.getUser());
        testEntityManager.persistAndFlush(testEntitiesKeeper.getCompany());
        testEntityManager.persistAndFlush(testEntitiesKeeper.getCounterpartner());
        testEntitiesKeeper.getCurrencies().forEach(c -> c = testEntityManager.persistAndFlush(c));
        testEntitiesKeeper.getScenarios().forEach(s -> s = testEntityManager.persistAndFlush(s));
        testEntitiesKeeper.getDurations().forEach(d -> d = testEntityManager.persistAndFlush(d));
        testEntitiesKeeper.getDepositRates().forEach(dr -> dr = testEntityManager.persistAndFlush(dr));
        testEntitiesKeeper.getPeriods().forEach(p -> p = testEntityManager.persistAndFlush(p));
        testEntitiesKeeper.getIfrsAccounts().forEach(p -> p = testEntityManager.persistAndFlush(p));
        testEntitiesKeeper.getLeasingDeposits().forEach(p -> p = testEntityManager.persistAndFlush(p));
        testEntitiesKeeper.getEntriesForIfrsSumDaoTest().forEach(p -> testEntityManager.persistAndFlush(p));
        testEntitiesKeeper.getEntriesIfrsForIfrsSumDaoTests().forEach(e -> e = testEntityManager.persistAndFlush(e));
    }

    private void setNullIntoUserId() {
        testEntitiesKeeper.getUser().setId(null);
    }

    private void setNullIntoCompanyId() {
        testEntitiesKeeper.getCompany().setId(null);
    }

    private void setNullIntoCounterpartnerId() {
        testEntitiesKeeper.getCounterpartner().setId(null);
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

    private void setNullIdPeriod() {
        testEntitiesKeeper.getPeriods().forEach(p -> p.setId(null));
    }

    private void setLastChangeDateIntoPeriod() {
        testEntitiesKeeper.getPeriods().forEach(p -> p.setLastChange(ZonedDateTime.now()));
    }

    private void setNullIdIfrsAccount() {
        testEntitiesKeeper.getIfrsAccounts().forEach(p -> p.setId(null));
    }

    private void setLastChangeDateIntoIfrsAccount() {
        testEntitiesKeeper.getIfrsAccounts().forEach(p -> p.setLastChange(ZonedDateTime.now()));
    }

    private void setNullIdLeasingDeposit() {
        testEntitiesKeeper.getLeasingDeposits().forEach(p -> p.setId(null));
    }

    private void setLastChangeDateIntoLeasingDeposit() {
        testEntitiesKeeper.getLeasingDeposits().forEach(p -> p.setLastChange(ZonedDateTime.now()));
    }

    private void setLastChangeDateIntoEntriesForIfrsSumDaoTest() {
        testEntitiesKeeper.getEntriesForIfrsSumDaoTest().forEach(e -> e.setLastChange(ZonedDateTime.now()));
    }
}