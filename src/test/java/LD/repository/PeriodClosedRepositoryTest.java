package LD.repository;


import LD.config.Security.model.User.User;
import LD.model.Enums.STATUS_X;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.Period.Period;
import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.PeriodsClosed.PeriodsClosedID;
import LD.model.Scenario.Scenario;
import LD.service.Calculators.LeasingDeposits.CalculationParametersSourceImpl;
import Utils.Builders;
import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto = create",
        "spring.flyway.baselineOnMigrate = false"})
@Log4j2
public class PeriodClosedRepositoryTest {

    @Autowired
    PeriodsClosedRepository periodsClosedRepository;
    @Autowired
    TestEntityManager testEntityManager;

    User user;
    Scenario fact;

    @Test
    void findAll_shouldReturnListNotClosedPeriods_WhenFindFirstClosedPeriodAndScenarioContainsAtLeastOneClosedAndOneOpenPeriod() {
        prepareAndSaveDataIntoDatabase();

        Period period1 =
                Period.builder()
                        .date(LocalDate.of(2020, 8, 31)).build();
        period1.setLastChange(ZonedDateTime.now());
        period1.setUser(user);

        Period period2 =
                Period.builder().date(LocalDate.of(2020, 9, 30)).build();
        period2.setLastChange(ZonedDateTime.now());
        period2.setUser(user);

        Period period3 =
                Period.builder().date(LocalDate.of(2020, 10, 31)).build();
        period3.setLastChange(ZonedDateTime.now());
        period3.setUser(user);

        PeriodsClosedID periodClosedID1 =
                PeriodsClosedID.builder().scenario(fact).period(period1).build();
        PeriodsClosedID periodClosedID2 =
                PeriodsClosedID.builder().scenario(fact).period(period2).build();
        PeriodsClosedID periodClosedID3 =
                PeriodsClosedID.builder().scenario(fact).period(period3).build();

        PeriodsClosed periodsClosed1 =
                PeriodsClosed.builder().periodsClosedID(periodClosedID1).ISCLOSED(STATUS_X.X).build();
        periodsClosed1.setLastChange(ZonedDateTime.now());
        periodsClosed1.setUser(user);

        PeriodsClosed periodsClosed2 =
                PeriodsClosed.builder().periodsClosedID(periodClosedID2).build();
        periodsClosed2.setLastChange(ZonedDateTime.now());
        periodsClosed2.setUser(user);

        PeriodsClosed periodsClosed3 =
                PeriodsClosed.builder().periodsClosedID(periodClosedID3).build();
        periodsClosed3.setLastChange(ZonedDateTime.now());
        periodsClosed3.setUser(user);

        testEntityManager.persist(fact);
        testEntityManager.persist(period1);
        testEntityManager.persist(period2);
        testEntityManager.persist(period3);
        testEntityManager.persist(periodsClosed1);
        testEntityManager.persist(periodsClosed2);
        testEntityManager.persist(periodsClosed3);

        List<PeriodsClosed> factResult =
                periodsClosedRepository.findAll(CalculationParametersSourceImpl.specFirstClosedPeriod(fact));

        List<PeriodsClosed> expectedResult = List.of(periodsClosed2);

        log.info("factResult = {}", factResult);
        log.info("expectedResult = {}", expectedResult);

        assertTrue(factResult.containsAll(expectedResult));
        assertEquals(2, factResult.size());
    }

    private void prepareDataForPersisting() {
        user = User.builder().password("X").username("X").build();
        user.setLastChange(ZonedDateTime.now());

        fact = Scenario.builder().name("FACT").status(ScenarioStornoStatus.ADDITION).build();
        fact.setLastChange(ZonedDateTime.now());
        fact.setUser(user);
    }

    private void prepareAndSaveDataIntoDatabase() {
        prepareDataForPersisting();

        testEntityManager.persistAndFlush(user);
        testEntityManager.persistAndFlush(fact);
    }

    @Test
    public void test2_GDK_getFirstOpenPeriod() {
        prepareAndSaveDataIntoDatabase();

        Scenario plan2020 = Builders.getSC("PLAN2020", ScenarioStornoStatus.FULL, user);

        plan2020 = testEntityManager.persist(plan2020);

        testEntityManager.flush();

        long all = LocalDate.of(2010, 1, 31).datesUntil(LocalDate.of(2030, 12, 31), java.time.Period.ofMonths(1)).count();

        Scenario finalFact = fact;
        Scenario finalPlan202 = plan2020;
        LocalDate.of(2010, 1, 31).datesUntil(LocalDate.of(2030, 12, 31), java.time.Period.ofMonths(1)).forEach((date) ->
                {
                    LocalDate newDate = date.withDayOfMonth(date.lengthOfMonth());

                    Period p = Builders.getPer(newDate.getDayOfMonth(), newDate.getMonthValue(), newDate.getYear());
                    p.setUser(user);
                    p = testEntityManager.persist(p);
                    testEntityManager.flush();

                    Long nextIndexPC = -periodsClosedRepository.findAll().size() / 2 + all;

                    PeriodsClosedID periodsClosedID = PeriodsClosedID.builder()
                            .period(p)
                            .scenario(finalFact)
                            .build();

                    PeriodsClosed pc = new PeriodsClosed();
                    pc.setPeriodsClosedID(periodsClosedID);
                    pc.setUser(user);
                    pc.setLastChange(ZonedDateTime.now());
                    if (pc.getPeriodsClosedID().getPeriod().getDate().isBefore(Builders.getDate(31, 3, 2020)))
                        pc.setISCLOSED(STATUS_X.X);

                    testEntityManager.persist(pc);
                    testEntityManager.flush();

                    PeriodsClosedID periodsClosedID2 = PeriodsClosedID.builder()
                            .period(p)
                            .scenario(finalPlan202)
                            .build();

                    PeriodsClosed pcB = new PeriodsClosed();
                    pcB.setPeriodsClosedID(periodsClosedID2);
                    pcB.setLastChange(ZonedDateTime.now());
                    pcB.setUser(user);

                    testEntityManager.persist(pcB);
                    testEntityManager.flush();
                }
        );

        periodsClosedRepository.findAll().forEach(System.out::println);

        TreeMap<LocalDate, String> answer = new TreeMap<>();
        answer.put(Builders.getDate(31, 3, 2020), "FACT");

        Assert.assertEquals(answer.firstEntry().getKey(),
                periodsClosedRepository.findAll(CalculationParametersSourceImpl.specFirstClosedPeriod(fact)).stream()
                        .collect(TreeMap::new,
                                (trm, date) -> trm.put(date.getPeriodsClosedID().getPeriod().getDate(), date.getPeriodsClosedID().getScenario().getName()),
                                (trm1, trm2) -> trm1.putAll(trm2)).firstEntry().getKey());
    }
}
