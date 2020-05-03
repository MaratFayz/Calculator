package TestsForLeasingDeposit;

import LD.model.Company.Company;
import LD.model.Counterpartner.Counterpartner;
import LD.model.Currency.Currency;
import LD.model.DepositRate.DepositRate;
import LD.model.Duration.Duration;
import LD.model.EndDate.EndDate;
import LD.model.EndDate.EndDateID;
import LD.model.Enums.STATUS_X;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.Period.Period;
import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.PeriodsClosed.PeriodsClosedID;
import LD.model.Scenario.Scenario;
import LD.repository.*;
import LD.service.Calculators.LeasingDeposits.EntryCalculator;
import LD.service.Calculators.LeasingDeposits.GeneralDataKeeper;
import TestsForLeasingDeposit.Calculator.Builders;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@DataJpaTest
public class testGDK
{
	@Autowired
	ScenarioRepository scenarioRepository;

	@Autowired
	PeriodsClosedRepository periodsClosedRepository;

	@Autowired
	PeriodRepository periodRepository;

	@Autowired
	TestEntityManager testEntityManager;

	@Autowired
	LeasingDepositRepository leasingDepositRepository;

	@Autowired
	DepositRatesRepository depositRatesRepository;

	static Scenario fact;
	static Scenario plan2020;
	static Scenario plan2021;
	static Scenario plan2022;
	static Currency usd;
	static Company C1001;
	static Counterpartner CP;

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

	@Test
	public void test2_GDK_getFirstOpenPeriod()
	{
		fact = Builders.getSC("FACT", ScenarioStornoStatus.ADDITION);
		plan2020 = Builders.getSC("PLAN2020", ScenarioStornoStatus.FULL);

		fact = testEntityManager.persist(fact);
		plan2020 = testEntityManager.persist(plan2020);

		testEntityManager.flush();

		long all = LocalDate.of(2010, 1, 31).datesUntil(LocalDate.of(2030, 12, 31), java.time.Period.ofMonths(1)).count();

		LocalDate.of(2010, 1, 31).datesUntil(LocalDate.of(2030, 12, 31), java.time.Period.ofMonths(1)).forEach((date) ->
			{
				LocalDate newDate = date.withDayOfMonth(date.lengthOfMonth());

				Period p = Builders.getPer(newDate.getDayOfMonth(), newDate.getMonthValue(), newDate.getYear());
				p = testEntityManager.persist(p);
				testEntityManager.flush();

				Long nextIndexPC = -periodsClosedRepository.findAll().size() / 2 + all;

				PeriodsClosedID periodsClosedID = PeriodsClosedID.builder()
						.period(p)
						.scenario(fact)
						.build();

				PeriodsClosed pc = new PeriodsClosed();
				pc.setPeriodsClosedID(periodsClosedID);
				if (pc.getPeriodsClosedID().getPeriod().getDate().isBefore(Builders.getDate(31, 3, 2020))) pc.setISCLOSED(STATUS_X.X);

				testEntityManager.persist(pc);
				testEntityManager.flush();

				PeriodsClosedID periodsClosedID2 = PeriodsClosedID.builder()
						.period(p)
						.scenario(plan2020)
						.build();

				PeriodsClosed pcB = new PeriodsClosed();
				pcB.setPeriodsClosedID(periodsClosedID2);

				testEntityManager.persist(pcB);
				testEntityManager.flush();
			}
		);

		periodsClosedRepository.findAll().forEach(System.out::println);

		TreeMap<ZonedDateTime, String> answer = new TreeMap<>();
		answer.put(Builders.getDate(31, 3, 2020), "FACT");

		assertEquals(answer.firstEntry().getKey(),
				periodsClosedRepository.findAll(GeneralDataKeeper.specFirstClosedPeriod(fact)).stream()
						.collect(TreeMap::new,
								(trm, date) -> trm.put(date.getPeriodsClosedID().getPeriod().getDate(), date.getPeriodsClosedID().getScenario().getName()),
								(trm1, trm2) -> trm1.putAll(trm2)).firstEntry().getKey());
	}

	@Test
	public void Test3_GDK_ChooseLDs()
	{
		fact = Builders.getSC("FACT", ScenarioStornoStatus.ADDITION);
		plan2020 = Builders.getSC("PLAN2020", ScenarioStornoStatus.FULL);
		plan2021 = Builders.getSC("PLAN2021", ScenarioStornoStatus.FULL);
		plan2022 = Builders.getSC("PLAN2022", ScenarioStornoStatus.FULL);

		fact = testEntityManager.persist(fact);
		plan2020 = testEntityManager.persist(plan2020);
		plan2021 = testEntityManager.persist(plan2021);
		plan2022 = testEntityManager.persist(plan2022);

		Currency usd = Builders.getCUR("USD");
		Company C1001 = Builders.getEN("C1001", "Компания-1");
		Counterpartner CP = Builders.getCP( "ООО \"Лизинговая компания\"");

		usd = testEntityManager.persist(usd);
		C1001 = testEntityManager.persist(C1001);
		CP = testEntityManager.persist(CP);

		for(int i = 0; i < 5; i++)
		{
			for (Scenario sc : List.of(fact, plan2022, plan2020, plan2021))
			{
				LeasingDeposit leasingDeposit1 = new LeasingDeposit();
				leasingDeposit1.setCounterpartner(CP);
				leasingDeposit1.setCompany(C1001);
				leasingDeposit1.setCurrency(usd);
				leasingDeposit1.setDeposit_sum_not_disc(BigDecimal.valueOf(100000.00));
				leasingDeposit1.setStart_date(Builders.getDate(10, 3, 2017));
				leasingDeposit1.setScenario(sc);
				leasingDeposit1.setIs_created(STATUS_X.X);

				testEntityManager.persist(leasingDeposit1);
				testEntityManager.flush();
			}
		}

		for(int i = 0; i < 5; i++)
		{
			for (Scenario sc : List.of(fact, plan2022, plan2020, plan2021))
			{
				LeasingDeposit leasingDeposit1 = new LeasingDeposit();
				leasingDeposit1.setCounterpartner(CP);
				leasingDeposit1.setCompany(C1001);
				leasingDeposit1.setCurrency(usd);
				leasingDeposit1.setDeposit_sum_not_disc(BigDecimal.valueOf(100000.00));
				leasingDeposit1.setStart_date(Builders.getDate(10, 3, 2017));
				leasingDeposit1.setScenario(sc);

				testEntityManager.persist(leasingDeposit1);
				testEntityManager.flush();
			}
		}

		for(int i = 0; i < 5; i++)
		{
			for (Scenario sc : List.of(fact, plan2022, plan2020, plan2021))
			{
				LeasingDeposit leasingDeposit1 = new LeasingDeposit();
				leasingDeposit1.setCounterpartner(CP);
				leasingDeposit1.setCompany(C1001);
				leasingDeposit1.setCurrency(usd);
				leasingDeposit1.setDeposit_sum_not_disc(BigDecimal.valueOf(100000.00));
				leasingDeposit1.setStart_date(Builders.getDate(10, 3, 2017));
				leasingDeposit1.setScenario(sc);
				leasingDeposit1.setIs_deleted(STATUS_X.X);

				testEntityManager.persist(leasingDeposit1);
				testEntityManager.flush();
			}
		}

		for(int i = 0; i < 5; i++)
		{
			for (Scenario sc : List.of(fact, plan2022, plan2020, plan2021))
			{
				LeasingDeposit leasingDeposit1 = new LeasingDeposit();
				leasingDeposit1.setCounterpartner(CP);
				leasingDeposit1.setCompany(C1001);
				leasingDeposit1.setCurrency(usd);
				leasingDeposit1.setDeposit_sum_not_disc(BigDecimal.valueOf(100000.00));
				leasingDeposit1.setStart_date(Builders.getDate(10, 3, 2017));
				leasingDeposit1.setScenario(sc);
				leasingDeposit1.setIs_created(STATUS_X.X);
				leasingDeposit1.setIs_deleted(STATUS_X.X);

				testEntityManager.persist(leasingDeposit1);
				testEntityManager.flush();
			}
		}

		assertEquals(10, leasingDepositRepository.findAll(GeneralDataKeeper.specLDsForScenario(fact)).size());
	}

	public void saveGeneralDataForRateTests()
	{
		fact = Builders.getSC("FACT", ScenarioStornoStatus.ADDITION);
		plan2020 = Builders.getSC("PLAN2020", ScenarioStornoStatus.FULL);

		fact = testEntityManager.persist(fact);
		plan2020 = testEntityManager.persist(plan2020);

		usd = Builders.getCUR("USD");
		C1001 = Builders.getEN("C1001", "Компания-1");
		CP = Builders.getCP( "ООО \"Лизинговая компания\"");

		usd = testEntityManager.persist(usd);
		C1001 = testEntityManager.persist(C1001);
		CP = testEntityManager.persist(CP);

		Duration dur_0_12_M = Builders.getDur("<= 12 мес.", 0, 12);
		Duration dur_25_36_M = Builders.getDur("25-36 мес.", 25, 36);

		testEntityManager.persist(dur_0_12_M);
		testEntityManager.persist(dur_25_36_M);

		DepositRate depRateC1001_0_12M_1 = Builders.getDepRate(C1001, Builders.getDate(01, 01, 1970), Builders.getDate(31, 12, 2017), usd, dur_0_12_M, fact, BigDecimal.valueOf(2.0));
		DepositRate depRateC1001_0_12M_2 = Builders.getDepRate(C1001, Builders.getDate(01, 01, 2018), Builders.getDate(31, 12, 2999), usd, dur_0_12_M, fact, BigDecimal.valueOf(1000.0));
		DepositRate depRateC1001_25_36M_1 = Builders.getDepRate(C1001, Builders.getDate(01, 01, 1970), Builders.getDate(31, 12, 2017), usd, dur_25_36_M, fact, BigDecimal.valueOf(5.0));
		DepositRate depRateC1001_25_36M_2 = Builders.getDepRate(C1001, Builders.getDate(01, 01, 2018), Builders.getDate(31, 12, 2999), usd, dur_25_36_M, fact, BigDecimal.valueOf(5000));

		DepositRate depRateC1001_0_12M_P1 = Builders.getDepRate(C1001, Builders.getDate(01, 01, 1970), Builders.getDate(31, 12, 2017), usd, dur_0_12_M, plan2020, BigDecimal.valueOf(-200));
		DepositRate depRateC1001_0_12M_P2 = Builders.getDepRate(C1001, Builders.getDate(01, 01, 2018), Builders.getDate(31, 12, 2999), usd, dur_0_12_M, plan2020, BigDecimal.valueOf(-1000.0));
		DepositRate depRateC1001_25_36M_P1 = Builders.getDepRate(C1001, Builders.getDate(01, 01, 1970), Builders.getDate(31, 12, 2017), usd, dur_25_36_M, plan2020, BigDecimal.valueOf(-500));
		DepositRate depRateC1001_25_36M_P2 = Builders.getDepRate(C1001, Builders.getDate(01, 01, 2018), Builders.getDate(31, 12, 2999), usd, dur_25_36_M, plan2020, BigDecimal.valueOf(300));

		testEntityManager.persist(depRateC1001_0_12M_1);
		testEntityManager.persist(depRateC1001_0_12M_2);
		testEntityManager.persist(depRateC1001_25_36M_1);
		testEntityManager.persist(depRateC1001_25_36M_2);

		testEntityManager.persist(depRateC1001_0_12M_P1);
		testEntityManager.persist(depRateC1001_0_12M_P2);
		testEntityManager.persist(depRateC1001_25_36M_P1);
		testEntityManager.persist(depRateC1001_25_36M_P2);

		testEntityManager.flush();
	}

	@Test
	public void Test4_CALCULATOR_ChooseLDsDeposRates_1()
	{
		saveGeneralDataForRateTests();

		LeasingDeposit leasingDeposit1 = new LeasingDeposit();
		leasingDeposit1.setCounterpartner(CP);
		leasingDeposit1.setCompany(C1001);
		leasingDeposit1.setCurrency(usd);
		leasingDeposit1.setDeposit_sum_not_disc(BigDecimal.valueOf(100000.00));
		leasingDeposit1.setStart_date(Builders.getDate(10, 3, 2017));
		leasingDeposit1.setScenario(fact);
		leasingDeposit1.setIs_created(STATUS_X.X);

		EndDateID endDateID_31032017_20102019 = EndDateID.builder()
				.leasingDeposit_id(leasingDeposit1.getId())
				.period(Builders.getPer(31, 3, 2017))
				.scenario(fact)
				.build();

		EndDate ed_ld1_31032017_20102019 = new EndDate();
		ed_ld1_31032017_20102019.setEndDateID(endDateID_31032017_20102019);
		ed_ld1_31032017_20102019.setEnd_Date(Builders.getDate(20, 10, 2019));

		leasingDeposit1.setEnd_dates(Set.of(ed_ld1_31032017_20102019));

		leasingDeposit1 = testEntityManager.persist(leasingDeposit1);
		testEntityManager.flush();

		Mockito.when(calculator.getLDdurationMonths()).thenReturn((int) java.time.Duration.between(leasingDeposit1.getStart_date(), leasingDeposit1.getEnd_dates().stream().map(ed -> ed.getEnd_Date()).collect(Collectors.toList()).get(0)).toDays() / (365/12));

		assertEquals(List.of(BigDecimal.valueOf(5.0).setScale(2)), depositRatesRepository.findAll(EntryCalculator.getDepRateForLD(leasingDeposit1, calculator.getLDdurationMonths())).stream().map(dr -> dr.getRATE()).collect(Collectors.toList()));
	}

	@Test
	public void Test5_CALCULATOR_ChooseLDsDeposRates_2()
	{
		saveGeneralDataForRateTests();

		LeasingDeposit leasingDeposit1 = new LeasingDeposit();
		leasingDeposit1.setCounterpartner(CP);
		leasingDeposit1.setCompany(C1001);
		leasingDeposit1.setCurrency(usd);
		leasingDeposit1.setDeposit_sum_not_disc(BigDecimal.valueOf(100000.00));
		leasingDeposit1.setStart_date(Builders.getDate(10, 3, 2018));
		leasingDeposit1.setScenario(fact);
		leasingDeposit1.setIs_created(STATUS_X.X);

		EndDateID endDateID_31032018_20102020 = EndDateID.builder()
				.leasingDeposit_id(leasingDeposit1.getId())
				.period(Builders.getPer(31, 3, 2018))
				.scenario(fact)
				.build();

		EndDate ed_ld1_31032018_20102020 = new EndDate();
		ed_ld1_31032018_20102020.setEndDateID(endDateID_31032018_20102020);
		ed_ld1_31032018_20102020.setEnd_Date(Builders.getDate(20, 10, 2020));

		leasingDeposit1.setEnd_dates(Set.of(ed_ld1_31032018_20102020));

		leasingDeposit1 = testEntityManager.persist(leasingDeposit1);
		testEntityManager.flush();

		Mockito.when(calculator.getLDdurationMonths()).thenReturn((int) java.time.Duration.between(leasingDeposit1.getStart_date(), leasingDeposit1.getEnd_dates().stream().map(ed -> ed.getEnd_Date()).collect(Collectors.toList()).get(0)).toDays() / (365/12));

		assertEquals(List.of(BigDecimal.valueOf(5000).setScale(2)), depositRatesRepository.findAll(EntryCalculator.getDepRateForLD(leasingDeposit1, calculator.getLDdurationMonths())).stream().map(dr -> dr.getRATE()).collect(Collectors.toList()));
	}

	@Test
	public void Test6_CALCULATOR_ChooseLDsDeposRates_3()
	{
		saveGeneralDataForRateTests();

		LeasingDeposit leasingDeposit1 = new LeasingDeposit();
		leasingDeposit1.setCounterpartner(CP);
		leasingDeposit1.setCompany(C1001);
		leasingDeposit1.setCurrency(usd);
		leasingDeposit1.setDeposit_sum_not_disc(BigDecimal.valueOf(100000.00));
		leasingDeposit1.setStart_date(Builders.getDate(10, 3, 2018));
		leasingDeposit1.setScenario(plan2020);
		leasingDeposit1.setIs_created(STATUS_X.X);

		EndDateID endDateID_31032018_20102020 = EndDateID.builder()
				.leasingDeposit_id(leasingDeposit1.getId())
				.period(Builders.getPer(31, 3, 2018))
				.scenario(fact)
				.build();

		EndDate ed_ld1_31032018_20102020 = new EndDate();
		ed_ld1_31032018_20102020.setEndDateID(endDateID_31032018_20102020);
		ed_ld1_31032018_20102020.setEnd_Date(Builders.getDate(20, 10, 2020));

		leasingDeposit1.setEnd_dates(Set.of(ed_ld1_31032018_20102020));

		leasingDeposit1 = testEntityManager.persist(leasingDeposit1);
		testEntityManager.flush();

		Mockito.when(calculator.getLDdurationMonths()).thenReturn((int) java.time.Duration.between(leasingDeposit1.getStart_date(), leasingDeposit1.getEnd_dates().stream().map(ed -> ed.getEnd_Date()).collect(Collectors.toList()).get(0)).toDays() / (365/12));

		assertEquals(List.of(BigDecimal.valueOf(300).setScale(2)), depositRatesRepository.findAll(EntryCalculator.getDepRateForLD(leasingDeposit1, calculator.getLDdurationMonths())).stream().map(dr -> dr.getRATE()).collect(Collectors.toList()));
	}

	@Test
	public void Test7_CALCULATOR_ChooseLDsDeposRates_4()
	{
		saveGeneralDataForRateTests();



		LeasingDeposit leasingDeposit1 = new LeasingDeposit();
		leasingDeposit1.setCounterpartner(CP);
		leasingDeposit1.setCompany(C1001);
		leasingDeposit1.setCurrency(usd);
		leasingDeposit1.setDeposit_sum_not_disc(BigDecimal.valueOf(100000.00));
		leasingDeposit1.setStart_date(Builders.getDate(10, 1, 2017));
		leasingDeposit1.setScenario(plan2020);
		leasingDeposit1.setIs_created(STATUS_X.X);

		leasingDeposit1 = testEntityManager.persist(leasingDeposit1);
		testEntityManager.flush();

		EndDateID endDateID_10012017_20122017 = EndDateID.builder()
				//.leasingDeposit_id(leasingDeposit1.getId())
				.period(Builders.getPer(31, 1, 2017))
				.scenario(plan2020)
				.build();

		EndDate ed_ld1_10012017_20122017 = new EndDate();
		ed_ld1_10012017_20122017.setEndDateID(endDateID_10012017_20122017);
		ed_ld1_10012017_20122017.setEnd_Date(Builders.getDate(20, 12, 2017));

		leasingDeposit1.setEnd_dates(Set.of(ed_ld1_10012017_20122017));
//		leasingDeposit1 = testEntityManager.persist(leasingDeposit1);
//		testEntityManager.flush();

		//testEntityManager.persist(ed_ld1_10012017_20122017);


		Mockito.when(calculator.getLDdurationMonths()).thenReturn((int) java.time.Duration.between(leasingDeposit1.getStart_date(), leasingDeposit1.getEnd_dates().stream().map(ed -> ed.getEnd_Date()).collect(Collectors.toList()).get(0)).toDays() / (365/12));

		System.out.println("depositRatesRepository.findAll(EntryCalculator.getDepRateForLD(leasingDeposit1, calculator.getLDdurationMonths())) = " + depositRatesRepository.findAll(EntryCalculator.getDepRateForLD(leasingDeposit1, calculator.getLDdurationMonths())));

		assertEquals(List.of(BigDecimal.valueOf(-200).setScale(2)), depositRatesRepository.findAll(EntryCalculator.getDepRateForLD(leasingDeposit1, calculator.getLDdurationMonths())).stream().map(dr -> dr.getRATE()).collect(Collectors.toList()));
	}

}
