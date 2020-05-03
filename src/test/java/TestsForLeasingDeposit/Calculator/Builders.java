package TestsForLeasingDeposit.Calculator;

import LD.model.Company.Company;
import LD.model.Counterpartner.Counterpartner;
import LD.model.Currency.Currency;
import LD.model.DepositRate.DepositRate;
import LD.model.DepositRate.DepositRateID;
import LD.model.Duration.Duration;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.ExchangeRate.ExchangeRate;
import LD.model.ExchangeRate.ExchangeRateID;
import LD.model.IFRSAccount.IFRSAccount;
import LD.model.Period.Period;
import LD.model.Scenario.Scenario;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class Builders
{
	public static Scenario getSC(String name, ScenarioStornoStatus status)
	{
		Scenario c = new Scenario();
		c.setName(name);
		c.setStatus(status);
		return c;
	}

	public static ZonedDateTime getDate(int day, int month, int year)
	{
		return ZonedDateTime.of(year, month, day, 0, 0, 0, 0, ZoneId.of("UTC"));
	}

	public static Period getPer(int day, int month, int year)
	{
		Period period = new Period();
		period.setDate(getDate(day, month, year));

		return period;
	}

	public static Counterpartner getCP(String name)
	{
		Counterpartner cp = new Counterpartner();
		cp.setName(name);
		return cp;
	}

	public static Company getEN(String code, String name)
	{
		Company en = new Company();
		en.setCode(code);
		en.setName(name);
		return en;
	}

	public static Currency getCUR(String name)
	{
		Currency c = new Currency();
		c.setShort_name(name);
		return c;
	}

	public static Duration getDur(String name, int minMonths, int maxMonths)
	{
		Duration duration = new Duration();
		duration.setName(name);
		duration.setMAX_MONTH(maxMonths);
		duration.setMIN_MONTH(minMonths);

		return duration;
	}

	public static DepositRate getDepRate(Company company,
										 ZonedDateTime START_PERIOD,
										 ZonedDateTime END_PERIOD,
										 Currency currency,
										 Duration duration,
										 Scenario scenario,
										 BigDecimal rate)
	{
		DepositRateID depositRateID = DepositRateID.builder()
				.company(company)
				.currency(currency)
				.duration(duration)
				.END_PERIOD(END_PERIOD)
				.START_PERIOD(START_PERIOD)
				.scenario(scenario)
				.build();

		DepositRate depositRate = DepositRate.builder()
				.depositRateID(depositRateID)
				.RATE(rate)
				.build();

		return depositRate;
	}

	public static ExchangeRate getExRate(Scenario scenario, ZonedDateTime date, Currency currency, BigDecimal rate_at_date, BigDecimal average_rate_for_month)
	{
		ExchangeRateID exRID = ExchangeRateID.builder()
			.currency(currency)
			.scenario(scenario)
			.date(date)
			.build();

		ExchangeRate exchange_rate = new ExchangeRate();
		exchange_rate.setExchangeRateID(exRID);
		exchange_rate.setRate_at_date(rate_at_date);
		exchange_rate.setAverage_rate_for_month(average_rate_for_month);

		return exchange_rate;
	}

	public static List<IFRSAccount> getAllIFRSAcc()
	{
		List<IFRSAccount> ifrsAcc = new ArrayList<>();

		ifrsAcc.add(new IFRSAccount(0L,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F2000", "Поступление", "-", "THP99", "RUB", "-", false, "Reg.LD.1=N5"));

		ifrsAcc.add(new IFRSAccount(1L,
				"A0208010000",
				"Долгосрочные авансы выданные",
				"F2000", "Поступление", "-", "THP99", "RUB", "-", true, "Reg.LD.1=N5"));

		ifrsAcc.add(new IFRSAccount(2L,
				"A0208010000",
				"Долгосрочные авансы выданные",
				"F2000", "Поступление", "-", "THP99", "RUB", "-", true, "Reg.LD.1=M5"));

		ifrsAcc.add(new IFRSAccount(3L,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F2000", "Поступление", "-", "THP99", "RUB", "-", false, "Reg.LD.1=M5"));

		ifrsAcc.add(new IFRSAccount(4L,
				"P0302990000",
				"Прочие финансовые расходы",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", true, "Reg.LD.1=U5 + Reg.LD.1=V5"));

		ifrsAcc.add(new IFRSAccount(5L,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB", "-", false, "Reg.LD.1=U5"));

		ifrsAcc.add(new IFRSAccount(6L,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB", "-", false, "Reg.LD.1=V5"));

		ifrsAcc.add(new IFRSAccount(7L,
				"P0301990000",
				"Прочие финансовые доходы",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", true, "Reg.LD.1=W5 + Reg.LD.1=X5"));

		ifrsAcc.add(new IFRSAccount(8L,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB", "-", false, "Reg.LD.1=W5"));

		ifrsAcc.add(new IFRSAccount(9L,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB", "-", false, "Reg.LD.1=X5"));

		ifrsAcc.add(new IFRSAccount(10L,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB", "-", false, "Reg.LD.2=M5"));

		ifrsAcc.add(new IFRSAccount(11L,
				"P0301020000",
				"Процентные доходы по страховым депозитам",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", true, "Reg.LD.2=M5"));

		ifrsAcc.add(new IFRSAccount(12L,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F1600", "Курсовая разница", "-", "THP99", "RUB", "-", false, "Reg.LD.3=N5"));

		ifrsAcc.add(new IFRSAccount(13L,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F1600", "Курсовая разница", "-", "THP99", "RUB", "-", false, "Reg.LD.3=O5"));

		ifrsAcc.add(new IFRSAccount(14L,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F1600", "Курсовая разница", "-", "THP99", "RUB", "-", false, "Reg.LD.3=T5"));

		ifrsAcc.add(new IFRSAccount(15L,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F1600", "Курсовая разница", "-", "THP99", "RUB", "-", false, "Reg.LD.3=U5"));

		ifrsAcc.add(new IFRSAccount(16L,
				"P0301310000",
				"Положительные курсовые разницы",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", false, "Reg.LD.3=V5"));

		ifrsAcc.add(new IFRSAccount(17L,
				"P0302310000",
				"Отрицательные курсовые разницы",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", false, "Reg.LD.3=W5"));

		ifrsAcc.add(new IFRSAccount(18L,
				"A0107010000",
				"Краткосрочные авансы выданные",
				"F3000", "Выбытие", "-", "THP99", "RUB", "-", false, "Reg.LD.3=X5 + Reg.LD.3=Y5"));

		ifrsAcc.add(new IFRSAccount(19L,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F3000", "Выбытие", "-", "THP99", "RUB", "-", true, "Reg.LD.3=X5"));

		ifrsAcc.add(new IFRSAccount(20L,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F3000", "Выбытие", "-", "THP99", "RUB", "-", true, "Reg.LD.3=Y5"));

		ifrsAcc.add(new IFRSAccount(21L,
				"P0301310000",
				"Положительные курсовые разницы",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", false, "Reg.LD.4_MA_AFL=B1"));

		ifrsAcc.add(new IFRSAccount(22L,
				"P0302310000",
				"Отрицательные курсовые разницы",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", false, "Reg.LD.4_MA_AFL=C1"));

		ifrsAcc.add(new IFRSAccount(23L,
				"A0107010000",
				"Краткосрочные авансы выданные",
				"F1600", "Курсовая разница", "-", "THP99", "RUB", "-", false, "Reg.LD.4_MA_AFL=A1"));

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
				"F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB", "-", true, "Reg.LD.3=AE5-Reg.LD.3=AF5"));

		ifrsAcc.add(new IFRSAccount(32L,
				"A0107010000",
				"Краткосрочные авансы выданные",
				"F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB", "-", false, "Reg.LD.3=AE5-Reg.LD.3=AF5"));

		ifrsAcc.add(new IFRSAccount(33L,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB", "-", true, "Reg.LD.3=AA5-Reg.LD.3=AC5"));

		ifrsAcc.add(new IFRSAccount(34L,
				"A0102010100",
				"Краткосрочные депозиты по аренде ВС - основная сумма",
				"F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB", "-", false, "Reg.LD.3=AA5-Reg.LD.3=AC5"));

		ifrsAcc.add(new IFRSAccount(35L,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB", "-", true, "Reg.LD.3=AB5-Reg.LD.3=AD5"));

		ifrsAcc.add(new IFRSAccount(36L,
				"A0102010200",
				"Краткосрочные депозиты по аренде ВС - проценты",
				"F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB", "-", false, "Reg.LD.3=AB5-Reg.LD.3=AD5"));

		return ifrsAcc;
	}

}
