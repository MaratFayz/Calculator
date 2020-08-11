package LD.repository;


import LD.config.Security.model.User.User;
import LD.model.Enums.STATUS_X;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.Period.Period;
import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.PeriodsClosed.PeriodsClosedID;
import LD.model.Scenario.Scenario;
import LD.service.Calculators.LeasingDeposits.GeneralDataKeeper;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

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

    static User user =
            User.builder().password("X").username("X").lastChange(ZonedDateTime.now()).build();
    static Scenario fact = Scenario.builder().name("FACT").status(ScenarioStornoStatus.ADDITION)
            .lastChange(ZonedDateTime.now()).user(user).build();

    @Test
    void findAll_shouldReturnListNotClosedPeriods_WhenFindFirstClosedPeriodAndScenarioContainsAtLeastOneClosedAndOneOpenPeriod() {
        Period period1 =
                Period.builder().date(ZonedDateTime.of(2020, 8, 31, 0, 0, 0, 0, ZoneId.of("UTC")))
                        .lastChange(ZonedDateTime.now()).user(user).build();
        Period period2 =
                Period.builder().date(ZonedDateTime.of(2020, 9, 30, 0, 0, 0, 0, ZoneId.of("UTC")))
                        .lastChange(ZonedDateTime.now()).user(user).build();
        Period period3 =
                Period.builder().date(ZonedDateTime.of(2020, 10, 31, 0, 0, 0, 0, ZoneId.of("UTC")))
                        .lastChange(ZonedDateTime.now()).user(user).build();

        PeriodsClosedID periodClosedID1 =
                PeriodsClosedID.builder().scenario(fact).period(period1).build();
        PeriodsClosedID periodClosedID2 =
                PeriodsClosedID.builder().scenario(fact).period(period2).build();
        PeriodsClosedID periodClosedID3 =
                PeriodsClosedID.builder().scenario(fact).period(period3).build();

        PeriodsClosed periodsClosed1 =
                PeriodsClosed.builder().periodsClosedID(periodClosedID1).ISCLOSED(STATUS_X.X)
                        .user(user).lastChange(ZonedDateTime.now()).build();
        PeriodsClosed periodsClosed2 =
                PeriodsClosed.builder().periodsClosedID(periodClosedID2).user(user)
                        .lastChange(ZonedDateTime.now()).build();
        PeriodsClosed periodsClosed3 =
                PeriodsClosed.builder().periodsClosedID(periodClosedID3).user(user)
                        .lastChange(ZonedDateTime.now()).build();

        testEntityManager.persist(fact);
        testEntityManager.persist(period1);
        testEntityManager.persist(period2);
        testEntityManager.persist(period3);
        testEntityManager.persist(periodsClosed1);
        testEntityManager.persist(periodsClosed2);
        testEntityManager.persist(periodsClosed3);

        List<PeriodsClosed> factResult =
                periodsClosedRepository.findAll(GeneralDataKeeper.specFirstClosedPeriod(fact));

        List<PeriodsClosed> expectedResult = List.of(periodsClosed2);

        log.info("factResult = {}", factResult);
        log.info("expectedResult = {}", expectedResult);

        assertTrue(factResult.containsAll(expectedResult));
        assertEquals(2, factResult.size());
    }
}
