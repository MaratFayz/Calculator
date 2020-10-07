package LD.repository;


import LD.config.Security.model.User.User;
import LD.model.Enums.STATUS_X;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.Period.Period;
import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.PeriodsClosed.PeriodsClosedID;
import LD.model.Scenario.Scenario;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void findFirstOpenPeriodByScenario_shouldReturnFirstOpenPeriod_WhenValuesAreOneClosedAndOneOpenPeriod() {
        prepareAndSaveDataIntoDatabase();

        Period period1 =
                Period.builder()
                        .date(LocalDate.of(2020, 8, 31)).build();
        period1.setLastChange(ZonedDateTime.now());
        period1.setUser(user);

        LocalDate expectedDate = LocalDate.of(2020, 9, 30);
        Period period2 =
                Period.builder().date(expectedDate).build();
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

        LocalDate factDate = periodsClosedRepository.findFirstOpenPeriodDateByScenario(fact);

        log.info("factDate = {}", factDate);
        log.info("expectedDate = {}", expectedDate);

        assertEquals(expectedDate, factDate);
    }
}