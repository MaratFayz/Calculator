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
import LD.model.ExchangeRate.ExchangeRate;
import LD.model.IFRSAccount.IFRSAccount;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.Period.Period;
import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.PeriodsClosed.PeriodsClosedID;
import LD.model.Scenario.Scenario;
import LD.repository.*;
import LD.service.Calculators.LeasingDeposits.EntryCalculator;
import LD.service.EntryService;
import LD.service.EntryServiceImpl;
import LD.service.Calculators.LeasingDeposits.GeneralDataKeeper;
import TestsForLeasingDeposit.Calculator.Builders;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@RunWith(SpringRunner.class)
public class testService_Entries
{
	static Scenario fact;
	static Currency usd;
	static Company C1001;
	static Counterpartner CP;
	static HashMap<ZonedDateTime, Period> periods = new HashMap<>();
	static Duration dur_0_12_M;
	static Duration dur_25_36_M;
	static DepositRate depRateC1001_0_12M;
	static DepositRate depRateC1001_25_36M;
	static List<ExchangeRate> ExR;
	static List<IFRSAccount> ifrsAcc;

	List<LeasingDeposit> LeasingDeposits = new LinkedList<>();
	static LeasingDeposit leasingDeposit1;
	EntryCalculator lec;

	static final String SCENARIO_LOAD = "FACT";
	static final String SCENARIO_SAVE = "FACT";

	@MockBean
	static GeneralDataKeeper GDK;
	@MockBean
	static DepositRatesRepository depositRatesRepository;
	@MockBean
	static EntryRepository ld_entry_repository;
	@MockBean
	static EntryIFRSAccRepository entry_ifrs_acc_repository;
	@MockBean
	ScenarioRepository scenarioRepository;

	private EntryService entryService;

	@Before
	public void setUp() throws ExecutionException, InterruptedException
	{
		InitializeGeneraldata();
		create_LD_1_NormalTestLD();

		Specification<Scenario> specFact = GeneralDataKeeper.ScenarioForName(SCENARIO_LOAD);
		Specification<Scenario> specFact2 = GeneralDataKeeper.ScenarioForName(SCENARIO_SAVE);

		doReturn(Optional.of(fact)).when(scenarioRepository).findOne(specFact);
		doReturn(Optional.of(fact)).when(scenarioRepository).findOne(specFact2);

		Mockito.when(GDK.getLeasingDeposits()).thenReturn(List.of(leasingDeposit1));
		Mockito.when(GDK.getTo()).thenReturn(fact);
		Mockito.when(GDK.getFirstOpenPeriod_ScenarioTo()).thenReturn(Builders.getDate(31, 3, 2020));
		Mockito.when(GDK.getAllExRates()).thenReturn(ExR);
		Mockito.when(GDK.getAllPeriods()).thenReturn(List.copyOf(periods.values()));
		Mockito.when(GDK.getAllIFRSAccounts()).thenReturn(ifrsAcc);

		Specification<DepositRate> drs = EntryCalculator.getDepRateForLD(leasingDeposit1, 31);
		Mockito.when(depositRatesRepository.findAll(Mockito.any(drs.getClass()))).thenReturn(List.of(depRateC1001_25_36M));

		entryService = new EntryServiceImpl(ld_entry_repository, depositRatesRepository, entry_ifrs_acc_repository, GDK);

		//entryService.calculateEntries(SCENARIO_LOAD, SCENARIO_SAVE);
	}

	@Test
	public void test1_for_several_LDs()
	{
		ld_entry_repository.findAll().stream().forEach(System.out::println);

		//assertEquals();
	}

	public static void InitializeGeneraldata()
	{
		fact = Builders.getSC("FACT", ScenarioStornoStatus.ADDITION);
		usd = Builders.getCUR("USD");
		C1001 = Builders.getEN("C1001", "Компания-1");
		CP = Builders.getCP("ООО \"Лизинговая компания\"");

		long all = LocalDate.of(2010, 1, 31).datesUntil(LocalDate.of(2030, 12, 31), java.time.Period.ofMonths(1)).count();

		LocalDate.of(2010, 1, 31).datesUntil(LocalDate.of(2030, 12, 31), java.time.Period.ofMonths(1)).forEach((date) ->
				{
					LocalDate newDate = date.withDayOfMonth(date.lengthOfMonth());

					periods.put(Builders.getDate(newDate.getDayOfMonth(), newDate.getMonthValue(), newDate.getYear()), Builders.getPer(newDate.getDayOfMonth(), newDate.getMonthValue(), newDate.getYear()));
				}
		);

		dur_0_12_M = Builders.getDur("<= 12 мес.", 0, 12);
		dur_25_36_M = Builders.getDur("25-36 мес.", 25, 36);

		depRateC1001_0_12M = Builders.getDepRate(C1001, Builders.getDate(01, 01, 1970), Builders.getDate(31, 12, 2999), usd, dur_0_12_M, fact, BigDecimal.valueOf(2.0));
		depRateC1001_25_36M = Builders.getDepRate(C1001, Builders.getDate(01, 01, 1970), Builders.getDate(31, 12, 2999), usd, dur_25_36_M, fact, BigDecimal.valueOf(5.0));

		ExR = new ArrayList<>();
		ExR.add(Builders.getExRate(fact, Builders.getDate(10, 3, 2017), usd, BigDecimal.valueOf(58.8318), BigDecimal.ZERO));
		ExR.add(Builders.getExRate(fact, Builders.getDate(31, 3, 2017), usd, BigDecimal.valueOf(56.3779), BigDecimal.valueOf(58.1091)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(30, 4, 2017), usd, BigDecimal.valueOf(56.9838082901554), BigDecimal.valueOf(56.4315074286036)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(31, 5, 2017), usd, BigDecimal.valueOf(56.5168010059989), BigDecimal.valueOf(57.171996848083)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(30, 6, 2017), usd, BigDecimal.valueOf(59.0855029786337), BigDecimal.valueOf(57.8311009199966)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(31, 7, 2017), usd, BigDecimal.valueOf(59.543597652502), BigDecimal.valueOf(59.6707093574817)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(31, 8, 2017), usd, BigDecimal.valueOf(58.7306), BigDecimal.valueOf(59.6497128133555)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(30, 9, 2017), usd, BigDecimal.valueOf(58.0168999895548), BigDecimal.valueOf(57.6953966972068)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(31, 10, 2017), usd, BigDecimal.valueOf(57.8716), BigDecimal.valueOf(57.7305008320361)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(30, 11, 2017), usd, BigDecimal.valueOf(58.3311), BigDecimal.valueOf(58.9212082863353)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(31, 12, 2017), usd, BigDecimal.valueOf(57.6002), BigDecimal.valueOf(58.5887999151509)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(31, 1, 2018), usd, BigDecimal.valueOf(56.2914), BigDecimal.valueOf(56.7874891077606)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(28, 2, 2018), usd, BigDecimal.valueOf(55.6717), BigDecimal.valueOf(56.8124108208847)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(31, 3, 2018), usd, BigDecimal.valueOf(57.2649), BigDecimal.valueOf(57.0343978412931)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(30, 4, 2018), usd, BigDecimal.valueOf(61.9997), BigDecimal.valueOf(60.4623078997034)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(31, 5, 2018), usd, BigDecimal.valueOf(62.5937), BigDecimal.valueOf(62.2090013772315)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(30, 6, 2018), usd, BigDecimal.valueOf(62.7565), BigDecimal.valueOf(62.7143124565438)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(31, 7, 2018), usd, BigDecimal.valueOf(62.7805), BigDecimal.valueOf(62.8828032372803)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(31, 8, 2018), usd, BigDecimal.valueOf(68.0821), BigDecimal.valueOf(66.1231037757643)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(30, 9, 2018), usd, BigDecimal.valueOf(65.5906), BigDecimal.valueOf(67.6597104818259)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(31, 10, 2018), usd, BigDecimal.valueOf(65.7742), BigDecimal.valueOf(65.8868068638933)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(30, 11, 2018), usd, BigDecimal.valueOf(66.6342), BigDecimal.valueOf(65.8868102499607)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(31, 12, 2018), usd, BigDecimal.valueOf(69.4706), BigDecimal.valueOf(65.8867929292929)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(31, 1, 2019), usd, BigDecimal.valueOf(67.5795), BigDecimal.valueOf(65.8868071622573)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(28, 2, 2019), usd, BigDecimal.valueOf(68.5500), BigDecimal.valueOf(65.8867934993621)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(31, 3, 2019), usd, BigDecimal.valueOf(68.3300), BigDecimal.valueOf(65.8867985728187)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(30, 4, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.886789061497)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(31, 5, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8868017917687)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(30, 6, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867890889642)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(31, 7, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867887476067)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(31, 8, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867919915907)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(30, 9, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867896047699)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(31, 10, 2019), usd, BigDecimal.valueOf(67.5699997265878), BigDecimal.valueOf(65.8867901653654)));
		ExR.add(Builders.getExRate(fact, Builders.getDate(30, 11, 2019), usd, BigDecimal.valueOf(64.8114), BigDecimal.valueOf(63.3386)));

		Long i = 1L;
		for (Period p : periods.values())
		{
			PeriodsClosedID periodsClosedID = PeriodsClosedID.builder()
					.scenario(fact)
					.period(p)
					.build();

			PeriodsClosed pc = new PeriodsClosed();
			pc.setPeriodsClosedID(periodsClosedID);
			if (p.getDate().isBefore(Builders.getDate(31, 3, 2020))) pc.setISCLOSED(STATUS_X.X);
			i++;
		}

		ifrsAcc = new ArrayList<>();
		ifrsAcc.add(new IFRSAccount(0L,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F2000", "Поступление", "-", "THP99", "RUB", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.1=N5"));

		ifrsAcc.add(new IFRSAccount(1L,
				"A0208010000",
				"Долгосрочные авансы выданные",
				"F2000", "Поступление", "-", "THP99", "RUB", "-", true, "Reg.LeasingDeposit.model.LeasingDeposit.1=N5"));

		ifrsAcc.add(new IFRSAccount(2L,
				"A0208010000",
				"Долгосрочные авансы выданные",
				"F2000", "Поступление", "-", "THP99", "RUB", "-", true, "Reg.LeasingDeposit.model.LeasingDeposit.1=M5"));

		ifrsAcc.add(new IFRSAccount(3L,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F2000", "Поступление", "-", "THP99", "RUB", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.1=M5"));

		ifrsAcc.add(new IFRSAccount(4L,
				"P0302990000",
				"Прочие финансовые расходы",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", true, "Reg.LeasingDeposit.model.LeasingDeposit.1=U5 + Reg.LeasingDeposit.model.LeasingDeposit.1=V5"));

		ifrsAcc.add(new IFRSAccount(5L,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.1=U5"));

		ifrsAcc.add(new IFRSAccount(6L,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.1=V5"));

		ifrsAcc.add(new IFRSAccount(7L,
				"P0301990000",
				"Прочие финансовые доходы",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", true, "Reg.LeasingDeposit.model.LeasingDeposit.1=W5 + Reg.LeasingDeposit.model.LeasingDeposit.1=X5"));

		ifrsAcc.add(new IFRSAccount(8L,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.1=W5"));

		ifrsAcc.add(new IFRSAccount(9L,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.1=X5"));

		ifrsAcc.add(new IFRSAccount(10L,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.2=M5"));

		ifrsAcc.add(new IFRSAccount(11L,
				"P0301020000",
				"Процентные доходы по страховым депозитам",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", true, "Reg.LeasingDeposit.model.LeasingDeposit.2=M5"));

		ifrsAcc.add(new IFRSAccount(12L,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F1600", "Курсовая разница", "-", "THP99", "RUB", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.3=N5"));

		ifrsAcc.add(new IFRSAccount(13L,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F1600", "Курсовая разница", "-", "THP99", "RUB", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.3=O5"));

		ifrsAcc.add(new IFRSAccount(14L,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F1600", "Курсовая разница", "-", "THP99", "RUB", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.3=T5"));

		ifrsAcc.add(new IFRSAccount(15L,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F1600", "Курсовая разница", "-", "THP99", "RUB", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.3=U5"));

		ifrsAcc.add(new IFRSAccount(16L,
				"P0301310000",
				"Положительные курсовые разницы",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.3=V5"));

		ifrsAcc.add(new IFRSAccount(17L,
				"P0302310000",
				"Отрицательные курсовые разницы",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.3=W5"));

		ifrsAcc.add(new IFRSAccount(18L,
				"A0107010000",
				"Краткосрочные авансы выданные",
				"F3000", "Выбытие", "-", "THP99", "RUB", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.3=X5 + Reg.LeasingDeposit.model.LeasingDeposit.3=Y5"));

		ifrsAcc.add(new IFRSAccount(19L,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F3000", "Выбытие", "-", "THP99", "RUB", "-", true, "Reg.LeasingDeposit.model.LeasingDeposit.3=X5"));

		ifrsAcc.add(new IFRSAccount(20L,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F3000", "Выбытие", "-", "THP99", "RUB", "-", true, "Reg.LeasingDeposit.model.LeasingDeposit.3=Y5"));

		ifrsAcc.add(new IFRSAccount(21L,
				"P0301310000",
				"Положительные курсовые разницы",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.4_MA_AFL=B1"));

		ifrsAcc.add(new IFRSAccount(22L,
				"P0302310000",
				"Отрицательные курсовые разницы",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.4_MA_AFL=C1"));

		ifrsAcc.add(new IFRSAccount(23L,
				"A0107010000",
				"Краткосрочные авансы выданные",
				"F1600", "Курсовая разница", "-", "THP99", "RUB", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.4_MA_AFL=A1"));

		ifrsAcc.add(new IFRSAccount(24L,
				"A0215010100",
				"АПП воздушные суда и авиационные двигатели - ПСт",
				"F2006", "Ввод в эксплуатацию", "-", "THP99", "-", "-", false, "APP-1"));

		ifrsAcc.add(new IFRSAccount(25L,
				"A0215020100",
				"АПП земля - ПСт",
				"F2006", "Ввод в эксплуатацию", "-", "THP99", "-", "-", false, "APP-2"));

		ifrsAcc.add(new IFRSAccount(26L,
				"A0215030100",
				"АПП здания - ПСт",
				"F2006", "Ввод в эксплуатацию", "-", "THP99", "-", "-", false, "APP-3"));

		ifrsAcc.add(new IFRSAccount(27L,
				"A0215040100",
				"АПП машины и оборудование - ПСт",
				"F2006", "Ввод в эксплуатацию", "-", "THP99", "-", "-", false, "APP-4"));

		ifrsAcc.add(new IFRSAccount(28L,
				"A0215050100",
				"АПП прочие ОС - ПСт",
				"F2006", "Ввод в эксплуатацию", "-", "THP99", "-", "-", false, "APP-5"));

		ifrsAcc.add(new IFRSAccount(29L,
				"A0215060100",
				"АПП незавершенное строительство - первоначальная стоимость",
				"F2006", "Ввод в эксплуатацию", "-", "THP99", "-", "-", false, "APP-6"));

		ifrsAcc.add(new IFRSAccount(30L,
				"A0208010000",
				"Долгосрочные авансы выданные",
				"F3000", "Выбытие", "-", "THP99", "RUB", "-", true, "APP-7"));

		ifrsAcc.add(new IFRSAccount(31L,
				"A0208010000",
				"Долгосрочные авансы выданные",
				"F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB", "-", true, "Reg.LeasingDeposit.model.LeasingDeposit.3=AE5-Reg.LeasingDeposit.model.LeasingDeposit.3=AF5"));

		ifrsAcc.add(new IFRSAccount(32L,
				"A0107010000",
				"Краткосрочные авансы выданные",
				"F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.3=AE5-Reg.LeasingDeposit.model.LeasingDeposit.3=AF5"));

		ifrsAcc.add(new IFRSAccount(33L,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB", "-", true, "Reg.LeasingDeposit.model.LeasingDeposit.3=AA5-Reg.LeasingDeposit.model.LeasingDeposit.3=AC5"));

		ifrsAcc.add(new IFRSAccount(34L,
				"A0102010100",
				"Краткосрочные депозиты по аренде ВС - основная сумма",
				"F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.3=AA5-Reg.LeasingDeposit.model.LeasingDeposit.3=AC5"));

		ifrsAcc.add(new IFRSAccount(35L,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB", "-", true, "Reg.LeasingDeposit.model.LeasingDeposit.3=AB5-Reg.LeasingDeposit.model.LeasingDeposit.3=AD5"));

		ifrsAcc.add(new IFRSAccount(36L,
				"A0102010200",
				"Краткосрочные депозиты по аренде ВС - проценты",
				"F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB", "-", false, "Reg.LeasingDeposit.model.LeasingDeposit.3=AB5-Reg.LeasingDeposit.model.LeasingDeposit.3=AD5"));
	}

	public static void create_LD_1_NormalTestLD()
	{
		//Депозит только для факта 1
		leasingDeposit1 = new LeasingDeposit();
		leasingDeposit1.setId(1L);
		leasingDeposit1.setCounterpartner(CP);
		leasingDeposit1.setCompany(C1001);
		leasingDeposit1.setCurrency(usd);
		leasingDeposit1.setDeposit_sum_not_disc(BigDecimal.valueOf(100000.00));
		leasingDeposit1.setStart_date(Builders.getDate(10, 3, 2017));
		leasingDeposit1.setScenario(fact);
		leasingDeposit1.setIs_created(STATUS_X.X);

		EndDateID endDateID_31032017_20102019 = EndDateID.builder()
				.leasingDeposit_id(leasingDeposit1.getId())
				.period(periods.get(Builders.getDate(31, 3, 2017)))
				.scenario(fact)
				.build();

		EndDate ed_ld1_31032017_20102019 = new EndDate();
		ed_ld1_31032017_20102019.setEndDateID(endDateID_31032017_20102019);
		ed_ld1_31032017_20102019.setEnd_Date(Builders.getDate(20, 10, 2019));

		EndDateID endDateID_31082017_20122019  = EndDateID.builder()
				.leasingDeposit_id(leasingDeposit1.getId())
				.period(periods.get(Builders.getDate(31, 8, 2017)))
				.scenario(fact)
				.build();

		EndDate ed_ld1_31082017_20122019 = new EndDate();
		ed_ld1_31082017_20122019.setEndDateID(endDateID_31082017_20122019);
		ed_ld1_31082017_20122019.setEnd_Date(Builders.getDate(20, 12, 2019));

		EndDateID endDateID_31102017_20112019 = EndDateID.builder()
				.leasingDeposit_id(leasingDeposit1.getId())
				.period(periods.get(Builders.getDate(31, 10, 2017)))
				.scenario(fact)
				.build();

		EndDate ed_ld1_31102017_20112019 = new EndDate();
		ed_ld1_31102017_20112019.setEndDateID(endDateID_31102017_20112019);
		ed_ld1_31102017_20112019.setEnd_Date(Builders.getDate(20, 11, 2019));

		EndDateID endDateID_30112019_20122019 = EndDateID.builder()
				.leasingDeposit_id(leasingDeposit1.getId())
				.period(periods.get(Builders.getDate(30, 11, 2019)))
				.scenario(fact)
				.build();

		EndDate ed_ld1_30112019_20122019 = new EndDate();
		ed_ld1_30112019_20122019.setEndDateID(endDateID_30112019_20122019);
		ed_ld1_30112019_20122019.setEnd_Date(Builders.getDate(03, 11, 2019));

		leasingDeposit1.setEnd_dates(Set.of(ed_ld1_31032017_20102019, ed_ld1_31082017_20122019, ed_ld1_31102017_20112019, ed_ld1_30112019_20122019));
	}
}