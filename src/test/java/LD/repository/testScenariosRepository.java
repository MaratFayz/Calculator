package LD.repository;

import LD.model.Company.Company;
import LD.model.Counterpartner.Counterpartner;
import LD.model.Currency.Currency;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.Scenario.Scenario;
import LD.service.Calculators.LeasingDeposits.EntryCalculator;
import LD.service.Calculators.LeasingDeposits.GeneralDataKeeper;
import TestsForLeasingDeposit.Calculator.Builders;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@DataJpaTest
public class testScenariosRepository
{
	@Autowired
	ScenarioRepository scenarioRepository;

	@Autowired
	TestEntityManager testEntityManager;

	static Scenario fact;
	static Scenario plan2020;
	static Scenario plan2021;
	static Scenario plan2022;

	@Mock
	EntryCalculator calculator;

	@Test
	public void test1_GDK_chooseScenarios()
	{
		fact = Builders.getSC("FACT", ScenarioStornoStatus.ADDITION);
		plan2020 = Builders.getSC("PLAN2020", ScenarioStornoStatus.ADDITION);
		plan2021 = Builders.getSC("PLAN2021", ScenarioStornoStatus.ADDITION);
		plan2022 = Builders.getSC("PLAN2022", ScenarioStornoStatus.ADDITION);

		fact = testEntityManager.persist(fact);
		plan2020 = testEntityManager.persist(plan2020);
		plan2021 = testEntityManager.persist(plan2021);
		plan2022 = testEntityManager.persist(plan2022);

		testEntityManager.flush();

		assertEquals(scenarioRepository.findOne(GeneralDataKeeper.ScenarioForName("FACT")).orElseThrow(), fact);
		assertEquals(scenarioRepository.findOne(GeneralDataKeeper.ScenarioForName("PLAN2020")).orElseThrow(), plan2020);
		assertEquals(scenarioRepository.findOne(GeneralDataKeeper.ScenarioForName("PLAN2021")).orElseThrow(), plan2021);
		assertEquals(scenarioRepository.findOne(GeneralDataKeeper.ScenarioForName("PLAN2022")).orElseThrow(), plan2022);
		assertEquals(scenarioRepository.findAll(), List.of(fact, plan2020, plan2021, plan2022));
	}
}
