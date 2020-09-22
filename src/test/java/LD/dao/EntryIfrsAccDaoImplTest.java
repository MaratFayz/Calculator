package LD.dao;

import LD.Application;
import LD.model.EntryIFRSAcc.EntryIFRSAcc;
import LD.model.EntryIFRSAcc.EntryIFRSAccID;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    public void sumActualEntriesIfrs_shouldReturnOneSumOfTwoEntriesIfrs_whenEntriesOfOneIfrsAccount() {
        Scenario scenario = testEntitiesKeeper.getScenarios().get(0);
        when(scenarioRepository.findById(anyLong())).thenReturn(Optional.of(scenario));
        Period period = testEntitiesKeeper.getPeriods().get(0);
        when(periodClosedRepository.findFirstOpenPeriodByScenario(eq(scenario))).thenReturn(period);

        testEntityManager.persistAndFlush(scenario);
        testEntityManager.persistAndFlush(period);

        EntryIFRSAccID entryIFRSAccID = EntryIFRSAccID.builder()
                .ifrsAccount(testEntitiesKeeper.getIfrsAccounts().get(0))
                .entry(testEntitiesKeeper.getEntryForEntryIfrsCalculation())
                .build();
        EntryIFRSAcc ifrsAcc = EntryIFRSAcc.builder()
                .sum(BigDecimal.ONE)
                .entryIFRSAccID(entryIFRSAccID)
                .build();

        List<Object[]> actualEntriesIfrs = entryIFRSAccRepository.sumActualEntriesIfrs(1L);
        assertNull(actualEntriesIfrs);
    }
}
