import junit.framework.TestCase;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.query.Query;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;

//перечень тестов:
//1. Проверить, будет ли работать программа с непроставленными данными по дате возврата для определенного периода
//2. Проверить, что депозит, который помечен как DELETED -> не имеет проводок со статусом <> DELETED
//3. Проверить, что депозит, который помечен как DELETED -> не появляется в расчете
//4. Проверить, что дисконтированная сумма считается от даты выдачи до даты закрытия на первоначальную дату
//5. Проверить, что депозит без даты возврата не считается.
//6. Валютные курсы корректно брались в зависимости от даты и валюты
//7. Ставки депозитные чтоб корректно брались в зависимсоти от срока и валюты, entity
//8. Ввод данных обязателен для двух таблиц по одному депозиту (оссновные данные и дата конца - нужна хотя бы одна запись, иначе нет расчета).
//9. Проверить, что берется последняя проводка по последнему закрытому периоду перед первой дыркой.

public class test extends TestCase
{
	StandardServiceRegistry registry;
	Metadata metadata;
	SessionFactory sessionFactory;
	Session session;
	Transaction transaction;

	static SCENARIO fact;
	static CURRENCY usd;
	static ENTITY C1001;
	static COUNTERPARTNER CP;
	static HashMap<ZonedDateTime, PERIOD> periods = new HashMap<>();
	static DURATION dur_0_12_M;
	static DURATION dur_25_36_M;
	static DEPOSIT_RATES depRateC1001_0_12M;
	static DEPOSIT_RATES depRateC1001_25_36M;
	static EXCHANGE_RATE ExRate10_03_2017USD;

	static PERIOD firstOpenPeriod;


	List<LD> alLD = new ArrayList<>();
	final String SCENARIO_LOAD = "FACT";
	final String SCENARIO_SAVE = "FACT";

	public void setUp()
	{
		registry = new StandardServiceRegistryBuilder().configure("Test_hibernate.cfg.xml").build();
		metadata = new MetadataSources(registry).getMetadataBuilder().build();
		sessionFactory = metadata.getSessionFactoryBuilder().build();
		session = sessionFactory.openSession();
		transaction = session.beginTransaction();

		InitializeGeneraldata(session);
		create_LD_1_NormalTestLD(session);

		//----------------save test LD------------------------END
		transaction.commit();
		session.close();
		sessionFactory.close();

		//----------------get LDs from DB------------------------START
		registry = new StandardServiceRegistryBuilder().configure("hibernate.cfg.xml").build();
		metadata = new MetadataSources(registry).getMetadataBuilder().build();
		sessionFactory = metadata.getSessionFactoryBuilder().build();
		session = sessionFactory.openSession();
		transaction = session.beginTransaction();

		CriteriaBuilder cb = session.getCriteriaBuilder();
/*		CriteriaQuery<PERIOD> cqOpenPer = cb.createQuery(PERIOD.class);
		Root<PERIOD> rootOpenPer
		firstOpenPeriod*/


		CriteriaQuery<LD> cqLD = cb.createQuery(LD.class);
		Root<LD> rootLD = cqLD.from(LD.class);
		cqLD.select(rootLD);
		Query<LD> resQLD = session.createQuery(cqLD);
		alLD = resQLD.getResultList();
		//----------------get LDs from DB------------------------END

		for(LD ld : alLD)
		{
			ld.countFirstEndDataAndDuration(SCENARIO_LOAD);

			CriteriaQuery<DEPOSIT_RATES> cqLDRate = cb.createQuery(DEPOSIT_RATES.class);
			Root<DEPOSIT_RATES> rootLDRates = cqLDRate.from(DEPOSIT_RATES.class);
			cqLDRate.select(rootLDRates)
					.where(
							cb.and(
									cb.equal(rootLDRates.get("entity"), ld.getEntity()),
									cb.lessThanOrEqualTo(rootLDRates.get("START_PERIOD"), ld.getStart_date()),
									cb.greaterThanOrEqualTo(rootLDRates.get("END_PERIOD"), ld.getFirstEndDate()),
									cb.equal(rootLDRates.get("currency"), ld.getCurrency()),
									cb.lessThanOrEqualTo(rootLDRates.get("duration").get("MIN_MONTH"), ld.getLDdurationMonths()),
									cb.greaterThanOrEqualTo(rootLDRates.get("duration").get("MAX_MONTH"), ld.getLDdurationMonths()),
									cb.equal(rootLDRates.get("scenario"), fact)
									));
			Query<DEPOSIT_RATES> LDRate = session.createQuery(cqLDRate);
			List<DEPOSIT_RATES> LDRateL = LDRate.getResultList();
			ld.calculate(LDRateL);
		}

		transaction.commit();
		session.close();
		sessionFactory.close();
	}

	public void test1_NominalValue()
	{
		//Если более года:
		assertEquals(BigDecimal.valueOf(100000.00).setScale(2), alLD.get(0).getDeposit_sum_not_disc());

/*		//Если удаленный:
		assertEquals(100000.0, alLD.get(1).getDeposit_sum_not_disc());

		//Если менее года:
		assertEquals(100000.0, alLD.get(2).getDeposit_sum_not_disc());*/
	}

	public void test2_CalculatingDiscountedValue()
	{
		//Если более года:
		assertEquals(BigDecimal.valueOf(88027.34).setScale(2), alLD.get(0).getDeposit_sum_discounted_on_firstEndDate().setScale(2, RoundingMode.UP));

/*		//Если удаленный:
		assertEquals(100000.0, alLD.get(1).getDeposit_sum_discounted_on_firstEndDate());

		//Если менее года:
		assertEquals(100000.0, alLD.get(2).getDeposit_sum_discounted_on_firstEndDate());*/
	}

	public void test3_FirstOpenPeriodGlobal()
	{
		assertEquals(periods.get(getDate(31,3,2020)), firstOpenPeriod);
	}

	public void test4_FirstMadeTransactionPeriod()
	{
		//ass(periods.get(getDate(31,3,2020)), firstOpenPeriod);
	}

	public static COUNTERPARTNER getCP(int id, String name)
	{
		COUNTERPARTNER cp = new COUNTERPARTNER();
		cp.setId(id);
		cp.setName(name);
		return cp;
	}

	public static ENTITY getEN(int id, String code, String name)
	{
		ENTITY en = new ENTITY();
		en.setId(id);
		en.setCode(code);
		en.setName(name);
		return en;
	}

	public static CURRENCY getCUR(int id, String name)
	{
		CURRENCY c = new CURRENCY();
		c.setId(id);
		c.setShort_name(name);
		return c;
	}

	public static SCENARIO getSC(int id, String name)
	{
		SCENARIO c = new SCENARIO();
		c.setId(id);
		c.setName(name);
		return c;
	}

	public static PERIOD getPer(int id, int day, int month, int year)
	{
		PERIOD period = new PERIOD();
		period.setId(id);
		period.setDate(getDate(day, month, year));

		return period;
	}

	public static DURATION getDur(int id, String name, int minMonths, int maxMonths)
	{
		DURATION duration = new DURATION();
		duration.setId(id);
		duration.setName(name);
		duration.setMAX_MONTH(maxMonths);
		duration.setMIN_MONTH(minMonths);

		return duration;
	}

	public static EXCHANGE_RATE getExRate(int id, SCENARIO scenario, ZonedDateTime date, double rate_at_date, double average_rate_for_month)
	{
		EXCHANGE_RATE exchange_rate = new EXCHANGE_RATE();
		exchange_rate.setId(id);
		exchange_rate.setCurrency(usd);
		exchange_rate.setScenario(scenario);
		exchange_rate.setDate(date);
		exchange_rate.setRate_at_date(BigDecimal.valueOf(rate_at_date));
		exchange_rate.setAverage_rate_for_month(BigDecimal.valueOf(average_rate_for_month));

		return exchange_rate;
	}


	//public static Date getDate(int day, int month, int year)
	public static ZonedDateTime getDate(int day, int month, int year)
	{
		return ZonedDateTime.of(year, month, day, 0, 0, 0, 0, ZoneId.of("UTC"));
	}

	public static DEPOSIT_RATES getDepRate(int id,
										   ENTITY entity,
										   ZonedDateTime START_PERIOD,
										   ZonedDateTime END_PERIOD,
										   CURRENCY currency,
										   DURATION duration,
										   SCENARIO scenario,
										   BigDecimal rate)
	{
		DEPOSIT_RATES deposit_rates = new DEPOSIT_RATES();
		deposit_rates.setId(id);
		deposit_rates.setEntity(entity);
		deposit_rates.setSTART_PERIOD(START_PERIOD);
		deposit_rates.setEND_PERIOD(END_PERIOD);
		deposit_rates.setCurrency(currency);
		deposit_rates.setDuration(duration);
		deposit_rates.setScenario(scenario);
		deposit_rates.setRATE(rate);

		return deposit_rates;
	}

	public static void InitializeGeneraldata(Session session)
	{
		fact = getSC(1, "FACT");
		usd = getCUR(1, "USD");
		C1001 = getEN(1, "C1001", "Аэрофлот");
		CP = getCP(1, "ООО \"Авиакапитал-Сервис\"");

		periods.put(getDate(31, 3, 2017), getPer(1, 31, 3, 2017));
		periods.put(getDate(30, 4, 2017), getPer(2, 30, 4, 2017));
		periods.put(getDate(31, 5, 2017), getPer(3, 31, 5, 2017));
		periods.put(getDate(30, 6, 2017), getPer(4, 30, 6, 2017));
		periods.put(getDate(31, 7, 2017), getPer(5, 31, 7, 2017));
		periods.put(getDate(31, 8, 2017), getPer(6, 31, 8, 2017));
		periods.put(getDate(30, 9, 2017), getPer(7, 30, 9, 2017));
		periods.put(getDate(31, 10, 2017), getPer(8, 31, 10, 2017));
		periods.put(getDate(30, 11, 2017), getPer(9, 30, 11, 2017));
		periods.put(getDate(31, 12, 2017), getPer(10, 31, 12, 2017));
		periods.put(getDate(31, 1, 2018), getPer(11, 31, 1, 2018));
		periods.put(getDate(28, 2, 2018), getPer(12, 28, 2, 2018));
		periods.put(getDate(31, 3, 2018), getPer(13, 31, 3, 2018));
		periods.put(getDate(30, 4, 2018), getPer(14, 30, 4, 2018));
		periods.put(getDate(31, 5, 2018), getPer(15, 31, 5, 2018));
		periods.put(getDate(30, 6, 2018), getPer(16, 30, 6, 2018));
		periods.put(getDate(31, 7, 2018), getPer(17, 31, 7, 2018));
		periods.put(getDate(31, 8, 2018), getPer(18, 31, 8, 2018));
		periods.put(getDate(30, 9, 2018), getPer(19, 30, 9, 2018));
		periods.put(getDate(31, 10, 2018), getPer(20, 31, 10, 2018));
		periods.put(getDate(30, 11, 2018), getPer(21, 30, 11, 2018));
		periods.put(getDate(31, 12, 2018), getPer(22, 31, 12, 2018));
		periods.put(getDate(31, 1, 2019), getPer(23, 31, 1, 2019));
		periods.put(getDate(28, 2, 2019), getPer(24, 28, 2, 2019));
		periods.put(getDate(31, 3, 2019), getPer(25, 31, 3, 2019));
		periods.put(getDate(30, 4, 2019), getPer(26, 30, 4, 2019));
		periods.put(getDate(31, 5, 2019), getPer(27, 31, 5, 2019));
		periods.put(getDate(30, 6, 2019), getPer(28, 30, 6, 2019));
		periods.put(getDate(31, 7, 2019), getPer(29, 31, 7, 2019));
		periods.put(getDate(31, 8, 2019), getPer(30, 31, 8, 2019));
		periods.put(getDate(30, 9, 2019), getPer(31, 30, 9, 2019));
		periods.put(getDate(31, 10, 2019), getPer(32, 31, 10, 2019));
		periods.put(getDate(30, 11, 2019), getPer(33, 30, 11, 2019));
		periods.put(getDate(31, 12, 2019), getPer(34, 31, 12, 2019));
		periods.put(getDate(31, 1, 2020), getPer(35, 31, 1, 2020));
		periods.put(getDate(29, 2, 2020), getPer(36, 29, 2, 2020));
		periods.put(getDate(31, 3, 2020), getPer(37, 31, 3, 2020));
		periods.put(getDate(30, 4, 2020), getPer(38, 30, 4, 2020));
		periods.put(getDate(31, 5, 2020), getPer(39, 31, 5, 2020));
		periods.put(getDate(30, 6, 2020), getPer(40, 30, 6, 2020));

		dur_0_12_M = getDur(1, "<= 12 мес.", 0, 12);
		dur_25_36_M = getDur(3, "25-36 мес.", 25, 36);

		depRateC1001_0_12M = getDepRate(1, C1001, getDate(01, 01, 1970), getDate(31,12,2999), usd, dur_0_12_M, fact, BigDecimal.valueOf(2.0));
		depRateC1001_25_36M = getDepRate(3, C1001, getDate(01, 01, 1970), getDate(31,12,2999), usd, dur_25_36_M, fact, BigDecimal.valueOf(5.0));

		ExRate10_03_2017USD = getExRate(1, fact, getDate(10, 3, 2017), 58.8318, 0.0);

		session.save(fact);
		session.save(usd);
		session.save(C1001);
		session.save(CP);
		periods.values().stream().forEach(value -> session.save(value));

		int i=1;
		for(PERIOD p : periods.values())
		{
			PERIODS_CLOSED pc = new PERIODS_CLOSED();
			pc.setId(i);
			pc.setPeriod(p);
			pc.setScenario(fact);
			if(p.getDate().isBefore(getDate(31,3,2020))) pc.setISCLOSED(STATUS_X.X);
			session.save(pc);
			i++;
		}

		session.save(dur_25_36_M);
		session.save(depRateC1001_25_36M);
		session.save(dur_0_12_M);
		session.save(depRateC1001_0_12M);
		session.save(ExRate10_03_2017USD);
	}

	public static void create_LD_1_NormalTestLD(Session session)
	{
		//Депозит только для факта 1
		LD ld1 = new LD();
		ld1.setId(1);
		ld1.setCounterpartner(CP);
		ld1.setEntity(C1001);
		ld1.setCurrency(usd);
		ld1.setDeposit_sum_not_disc(BigDecimal.valueOf(100000));
		ld1.setStart_date(getDate(10, 3, 2017));

		LD_STATUS lds = new LD_STATUS();
		lds.setId(1);
		lds.setLd(ld1);
		lds.setScenario(fact);
		lds.setIs_created(STATUS_X.X);

		END_DATES ed_ld1_31032017_20102019 = new END_DATES();
		ed_ld1_31032017_20102019.setId(1);
		ed_ld1_31032017_20102019.setLd(ld1);
		ed_ld1_31032017_20102019.setScenario(fact);
		ed_ld1_31032017_20102019.setPeriod(periods.get(getDate(31,3,2017)));
		ed_ld1_31032017_20102019.setEnd_Date(getDate(20, 10, 2019));

		END_DATES ed_ld1_31082017_20122019 = new END_DATES();
		ed_ld1_31082017_20122019.setId(2);
		ed_ld1_31082017_20122019.setLd(ld1);
		ed_ld1_31082017_20122019.setScenario(fact);
		ed_ld1_31082017_20122019.setPeriod(periods.get(getDate(31,8,2017)));
		ed_ld1_31082017_20122019.setEnd_Date(getDate(20, 12, 2019));

		END_DATES ed_ld1_31102017_20112019 = new END_DATES();
		ed_ld1_31102017_20112019.setId(3);
		ed_ld1_31102017_20112019.setLd(ld1);
		ed_ld1_31102017_20112019.setScenario(fact);
		ed_ld1_31102017_20112019.setPeriod(periods.get(getDate(31,10,2017)));
		ed_ld1_31102017_20112019.setEnd_Date(getDate(20, 11, 2019));

		END_DATES ed_ld1_30112019_20122019 = new END_DATES();
		ed_ld1_30112019_20122019.setId(4);
		ed_ld1_30112019_20122019.setLd(ld1);
		ed_ld1_30112019_20122019.setScenario(fact);
		ed_ld1_30112019_20122019.setPeriod(periods.get(getDate(30,11,2019)));
		ed_ld1_30112019_20122019.setEnd_Date(getDate(20, 12, 2019));

		session.save(ld1);
		session.save(lds);
		session.save(ed_ld1_31032017_20102019);
		session.save(ed_ld1_31082017_20122019);
		session.save(ed_ld1_31102017_20112019);
		session.save(ed_ld1_30112019_20122019);
	}

/*	public static LD getLD_2_DeletedTestLD()
	{
		//Депозит только для факта 2 - Удаленный
		dataLD = new HashMap<>();
		dataLD.put("ID", "LDTEST0002");
		dataLD.put("ID_ENTITY", "C1001");
		dataLD.put("ID_COUNTERPARTY", "7");
		dataLD.put("ID_CURRENCY", "1");
		dataLD.put("START_DATE", "2017-03-10");
		dataLD.put("DEPOSIT_SUM_NOT_DISC", "100000");
		dataLD.put("TO_DELETED", "X");
		dataLD.put("ID_SCENARIO", "FACT");
		LeasingDeposits.add(dataLD);
	}*/

/*	public static LD getLD_3_NormalTestLDLess1Year()
	{
		//Депозит только для факта 3 - До года
		dataLD = new HashMap<>();
		dataLD.put("ID", "LDTEST0003");
		dataLD.put("ID_ENTITY", "C1001");
		dataLD.put("ID_COUNTERPARTY", "2");
		dataLD.put("ID_CURRENCY", "1");
		dataLD.put("START_DATE", "2017-03-10");
		dataLD.put("DEPOSIT_SUM_NOT_DISC", "100000");
		dataLD.put("TO_DELETED", "null");
		dataLD.put("ID_SCENARIO", "FACT");
		LeasingDeposits.add(dataLD);

		table_LDeposits = new ResultSet_Sim.ResultSetT(LeasingDeposits);

		dataLD = new HashMap<>();
		dataLD.put("ID_LD", "LDTEST0003");
		dataLD.put("ID_PERIOD", "032017");
		dataLD.put("END_DATE", "2017-10-20");
		LDWIthEndDatesAndPeriods.add(dataLD);

		table_LDWIthEndDatesAndPeriods = new ResultSet_Sim.ResultSetT(LDWIthEndDatesAndPeriods);
	}*/

/*	public static List<CurrencyExchangeRate> getCER()
	{
		dataLD = new HashMap<>();
		dataLD.put("ID_CURRENCY", "USD");
		dataLD.put("DATE", "2017-03-10");
		dataLD.put("ID_SCENARIO", "FACT");
		dataLD.put("RATE", "XXX");
		CurrencyExchange.add(dataLD);
	}*/
}
