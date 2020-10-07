package LD.dao;

import LD.Application;
import LD.model.Enums.STATUS_X;
import LD.model.LeasingDeposit.LeasingDepositDTO_out_onPeriodFor2Scenarios;
import LD.model.Scenario.Scenario;
import LD.repository.DepositRatesRepository;
import LD.repository.LeasingDepositRepository;
import LD.repository.PeriodsClosedRepository;
import LD.repository.ScenarioRepository;
import Utils.TestEntitiesKeeper;
import Utils.XmlDataLoader.LoadXmlFileForLeasingDepositsTest;
import Utils.XmlDataLoader.SaveEntitiesIntoDatabase;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@DataJpaTest
@ContextConfiguration(classes = {Application.class, LeasingDepositDaoImpl.class})
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto = create",
        "spring.flyway.baselineOnMigrate = false"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Log4j2
public class LeasingDepositDaoImplTest {

    @Autowired
    private TestEntityManager testEntityManager;
    @Autowired
    private LeasingDepositDaoImpl leasingDepositDaoImpl;

    private TestEntitiesKeeper testEntitiesKeeper;

    @MockBean
    private PeriodsClosedRepository periodClosedRepository;
    @MockBean
    private ScenarioRepository scenarioRepository;
    @MockBean
    private LeasingDepositRepository leasingDepositRepository;
    @MockBean
    private DepositRatesRepository depositRatesRepository;

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/LeasingDepositDaoImplTest/leasingDepositDaoImplTest1.xml")
    @SaveEntitiesIntoDatabase
    public void getActualDepositsWithEndDatesForScenarios_shouldReturn5ItemsDepositEndDatesFor2ScenariosGreaterFirstOpenDatesOfScenarios_whenExistsData() {
        Scenario scenarioFrom = testEntitiesKeeper.getScenarios().stream().filter(s -> s.getId() == 1L).collect(Collectors.toList()).get(0);
        Scenario scenarioTo = testEntitiesKeeper.getScenarios().stream().filter(s -> s.getId() == 2L).collect(Collectors.toList()).get(0);

        when(scenarioRepository.findById(eq(scenarioFrom.getId()))).thenReturn(java.util.Optional.of(scenarioFrom));
        when(scenarioRepository.findById(eq(scenarioTo.getId()))).thenReturn(java.util.Optional.of(scenarioTo));
        when(periodClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioFrom))).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioFrom());
        when(periodClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioTo))).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        when(leasingDepositRepository.findAll()).thenReturn(testEntitiesKeeper.getLeasingDeposits());

        LeasingDepositDTO_out_onPeriodFor2Scenarios ld1 = LeasingDepositDTO_out_onPeriodFor2Scenarios.builder()
                .company("Компания-1")
                .counterpartner("Counterpartner-1")
                .currency("Доллар США")
                .deposit_sum_not_disc(BigDecimal.valueOf(100).setScale(2))
                .periodOfChangingEndDate("2019-02-28")
                .endDate("2025-12-30")
                .is_created(STATUS_X.X)
                .is_deleted(null)
                .lastChange(null)
                .scenario("FACT")
                .start_date("2019-01-01")
                .username("User-1")
                .id(1L)
                .build();

        LeasingDepositDTO_out_onPeriodFor2Scenarios ld2 = LeasingDepositDTO_out_onPeriodFor2Scenarios.builder()
                .company("Компания-1")
                .counterpartner("Counterpartner-1")
                .currency("Доллар США")
                .deposit_sum_not_disc(BigDecimal.valueOf(100).setScale(2))
                .periodOfChangingEndDate("2019-02-28")
                .endDate("2035-12-30")
                .is_created(STATUS_X.X)
                .is_deleted(null)
                .lastChange(null)
                .scenario("PLAN")
                .start_date("2019-01-01")
                .username("User-1")
                .id(1L)
                .build();

        LeasingDepositDTO_out_onPeriodFor2Scenarios ld3 = LeasingDepositDTO_out_onPeriodFor2Scenarios.builder()
                .company("Компания-1")
                .counterpartner("Counterpartner-1")
                .currency("Доллар США")
                .deposit_sum_not_disc(BigDecimal.valueOf(100).setScale(2))
                .periodOfChangingEndDate("2019-04-30")
                .endDate("2045-12-30")
                .is_created(STATUS_X.X)
                .is_deleted(null)
                .lastChange(null)
                .scenario("PLAN")
                .start_date("2019-01-01")
                .username("User-1")
                .id(1L)
                .build();

        LeasingDepositDTO_out_onPeriodFor2Scenarios ld4 = LeasingDepositDTO_out_onPeriodFor2Scenarios.builder()
                .company("Компания-1")
                .counterpartner("Counterpartner-1")
                .currency("Доллар США")
                .deposit_sum_not_disc(BigDecimal.valueOf(100).setScale(2))
                .periodOfChangingEndDate("2019-01-31")
                .endDate("2020-12-30")
                .is_created(STATUS_X.X)
                .is_deleted(null)
                .lastChange(null)
                .scenario("PLAN")
                .start_date("2019-01-01")
                .username("User-1")
                .id(2L)
                .build();

        LeasingDepositDTO_out_onPeriodFor2Scenarios ld5 = LeasingDepositDTO_out_onPeriodFor2Scenarios.builder()
                .company("Компания-1")
                .counterpartner("Counterpartner-1")
                .currency("Доллар США")
                .deposit_sum_not_disc(BigDecimal.valueOf(100).setScale(2))
                .periodOfChangingEndDate("2019-07-31")
                .endDate("2020-11-30")
                .is_created(STATUS_X.X)
                .is_deleted(null)
                .lastChange(null)
                .scenario("PLAN")
                .start_date("2019-01-01")
                .username("User-1")
                .id(2L)
                .build();

        List<LeasingDepositDTO_out_onPeriodFor2Scenarios> queryResult = leasingDepositDaoImpl.getActualDepositsWithEndDatesForScenarios(1L, 2L);
        queryResult.forEach(e -> {
            e.setLastChange(null);
            System.out.println("Запрос EndDatesScTo: " + e);
        });

        assertEquals(5, queryResult.size());
        assertTrue(queryResult.contains(ld1));
        assertTrue(queryResult.contains(ld2));
        assertTrue(queryResult.contains(ld3));
        assertTrue(queryResult.contains(ld4));
        assertTrue(queryResult.contains(ld5));
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/LeasingDepositDaoImplTest/leasingDepositDaoImplTest1.xml")
    @SaveEntitiesIntoDatabase
    void getActualDepositsWithEndDatesForScenarios_shouldReturnListWith1Item_whenNoQueryResults() {
        Scenario scenarioFrom = testEntitiesKeeper.getScenarios().stream().filter(s -> s.getId() == 1L).collect(Collectors.toList()).get(0);
        Scenario scenarioTo = testEntitiesKeeper.getScenarios().stream().filter(s -> s.getId() == 2L).collect(Collectors.toList()).get(0);

        when(scenarioRepository.findById(eq(scenarioFrom.getId()))).thenReturn(java.util.Optional.of(scenarioFrom));
        when(scenarioRepository.findById(eq(scenarioTo.getId()))).thenReturn(java.util.Optional.of(scenarioTo));
        when(periodClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioFrom))).thenReturn(null);
        when(periodClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioTo))).thenReturn(null);
        when(leasingDepositRepository.findAll()).thenReturn(testEntitiesKeeper.getLeasingDeposits());

        List<LeasingDepositDTO_out_onPeriodFor2Scenarios> queryResult = leasingDepositDaoImpl.getActualDepositsWithEndDatesForScenarios(1L, 2L);
        queryResult.forEach(e -> {
            e.setLastChange(null);
            System.out.println("Запрос EndDatesScTo: " + e);
        });

        assertEquals(1, queryResult.size());
        assertEquals(new LeasingDepositDTO_out_onPeriodFor2Scenarios(), queryResult.get(0));
    }
}