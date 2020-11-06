package LD.repository;

import LD.config.Security.model.User.User;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.Scenario.Scenario;
import Utils.Builders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.Assert.assertEquals;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto = create",
        "spring.flyway.baselineOnMigrate = false"})
public class ScenarioRepositoryTest {

    @Autowired
    private ScenarioRepository scenarioRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    public void test1_GDK_chooseScenarios() {
        User user = Builders.getAnyUser();
        user = testEntityManager.persist(user);

        Scenario fact = Builders.getSC("FACT", ScenarioStornoStatus.ADDITION, user);
        Scenario plan2020 = Builders.getSC("PLAN2020", ScenarioStornoStatus.ADDITION, user);
        Scenario plan2021 = Builders.getSC("PLAN2021", ScenarioStornoStatus.ADDITION, user);
        Scenario plan2022 = Builders.getSC("PLAN2022", ScenarioStornoStatus.ADDITION, user);

        fact = testEntityManager.persist(fact);
        plan2020 = testEntityManager.persist(plan2020);
        plan2021 = testEntityManager.persist(plan2021);
        plan2022 = testEntityManager.persist(plan2022);

        testEntityManager.flush();

        assertEquals(scenarioRepository.findByName("FACT"), fact);
        assertEquals(scenarioRepository.findByName("PLAN2020"), plan2020);
        assertEquals(scenarioRepository.findByName("PLAN2021"), plan2021);
        assertEquals(scenarioRepository.findByName("PLAN2022"), plan2022);
        assertEquals(scenarioRepository.findAll(), List.of(fact, plan2020, plan2021, plan2022));
    }
}