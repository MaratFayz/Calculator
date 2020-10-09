package LD.repository;

import LD.config.Security.model.User.User;
import LD.model.Company.Company;
import LD.model.Counterpartner.Counterpartner;
import LD.model.Currency.Currency;
import LD.model.Enums.STATUS_X;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.Scenario.Scenario;
import Utils.Builders;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import static Utils.Builders.getAnyUser;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto = create",
        "spring.flyway.baselineOnMigrate = false"})
@Log4j2
public class LeasingDepositRepositoryTest {

    Scenario fact;
    Scenario plan2020;
    Scenario plan2021;
    Scenario plan2022;
    User user = getAnyUser();
    @Autowired
    private LeasingDepositRepository leasingDepositRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    public void Test3_GDK_ChooseLDs() {
        testEntityManager.persistAndFlush(user);

        fact = Builders.getSC("FACT", ScenarioStornoStatus.ADDITION, user);
        plan2020 = Builders.getSC("PLAN2020", ScenarioStornoStatus.FULL, user);
        plan2021 = Builders.getSC("PLAN2021", ScenarioStornoStatus.FULL, user);
        plan2022 = Builders.getSC("PLAN2022", ScenarioStornoStatus.FULL, user);

        fact = testEntityManager.persist(fact);
        plan2020 = testEntityManager.persist(plan2020);
        plan2021 = testEntityManager.persist(plan2021);
        plan2022 = testEntityManager.persist(plan2022);

        Currency usd = Builders.getCUR("USD");
        usd.setUserLastChanged(user);

        Company C1001 = Builders.getEN("C1001", "Компания-1");
        C1001.setUserLastChanged(user);

        Counterpartner CP = Builders.getCP("ООО \"Лизинговая компания\"");
        CP.setUserLastChanged(user);

        usd = testEntityManager.persist(usd);
        C1001 = testEntityManager.persist(C1001);
        CP = testEntityManager.persist(CP);

        for (int i = 0; i < 5; i++) {
            for (Scenario sc : List.of(fact, plan2022, plan2020, plan2021)) {
                LeasingDeposit leasingDeposit1 = new LeasingDeposit();
                leasingDeposit1.setCounterpartner(CP);
                leasingDeposit1.setCompany(C1001);
                leasingDeposit1.setCurrency(usd);
                leasingDeposit1.setDeposit_sum_not_disc(BigDecimal.valueOf(100000.00));
                leasingDeposit1.setStart_date(Builders.getDate(10, 3, 2017));
                leasingDeposit1.setScenario(sc);
                leasingDeposit1.setIs_created(STATUS_X.X);
                leasingDeposit1.setLastChange(ZonedDateTime.now());
                leasingDeposit1.setUserLastChanged(user);

                testEntityManager.persist(leasingDeposit1);
                testEntityManager.flush();
            }
        }

        for (int i = 0; i < 5; i++) {
            for (Scenario sc : List.of(fact, plan2022, plan2020, plan2021)) {
                LeasingDeposit leasingDeposit1 = new LeasingDeposit();
                leasingDeposit1.setCounterpartner(CP);
                leasingDeposit1.setCompany(C1001);
                leasingDeposit1.setCurrency(usd);
                leasingDeposit1.setDeposit_sum_not_disc(BigDecimal.valueOf(100000.00));
                leasingDeposit1.setStart_date(Builders.getDate(10, 3, 2017));
                leasingDeposit1.setScenario(sc);
                leasingDeposit1.setLastChange(ZonedDateTime.now());
                leasingDeposit1.setUserLastChanged(user);

                testEntityManager.persist(leasingDeposit1);
                testEntityManager.flush();
            }
        }

        for (int i = 0; i < 5; i++) {
            for (Scenario sc : List.of(fact, plan2022, plan2020, plan2021)) {
                LeasingDeposit leasingDeposit1 = new LeasingDeposit();
                leasingDeposit1.setCounterpartner(CP);
                leasingDeposit1.setCompany(C1001);
                leasingDeposit1.setCurrency(usd);
                leasingDeposit1.setDeposit_sum_not_disc(BigDecimal.valueOf(100000.00));
                leasingDeposit1.setStart_date(Builders.getDate(10, 3, 2017));
                leasingDeposit1.setScenario(sc);
                leasingDeposit1.setIs_deleted(STATUS_X.X);
                leasingDeposit1.setLastChange(ZonedDateTime.now());
                leasingDeposit1.setUserLastChanged(user);

                testEntityManager.persist(leasingDeposit1);
                testEntityManager.flush();
            }
        }

        for (int i = 0; i < 5; i++) {
            for (Scenario sc : List.of(fact, plan2022, plan2020, plan2021)) {
                LeasingDeposit leasingDeposit1 = new LeasingDeposit();
                leasingDeposit1.setCounterpartner(CP);
                leasingDeposit1.setCompany(C1001);
                leasingDeposit1.setCurrency(usd);
                leasingDeposit1.setDeposit_sum_not_disc(BigDecimal.valueOf(100000.00));
                leasingDeposit1.setStart_date(Builders.getDate(10, 3, 2017));
                leasingDeposit1.setScenario(sc);
                leasingDeposit1.setIs_created(STATUS_X.X);
                leasingDeposit1.setIs_deleted(STATUS_X.X);
                leasingDeposit1.setLastChange(ZonedDateTime.now());
                leasingDeposit1.setUserLastChanged(user);

                testEntityManager.persist(leasingDeposit1);
                testEntityManager.flush();
            }
        }

        assertEquals(10, leasingDepositRepository.getDepositsByScenario(fact.getId()).size());
    }
}