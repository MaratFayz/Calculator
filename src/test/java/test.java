import junit.framework.TestCase;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.query.Query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.Date;

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
	static PERIOD per31_03_2017;
	static PERIOD per31_08_2017;
	static PERIOD per31_10_2017;
	static PERIOD per30_11_2019;

	static END_DATES ed_ld1_31032017_20102019;
	static END_DATES ed_ld1_31082017_20122019;
	static END_DATES ed_ld1_31102017_20112019;
	static END_DATES ed_ld1_30112019_20122019;

	static LD_STATUS lds;
	static LD ld1;

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

		InitializeGeneraldata();

		//----------------save test LD------------------------START
		session.save(fact);
		session.save(usd);
		session.save(C1001);
		session.save(CP);
		session.save(per31_03_2017);
		session.save(per31_08_2017);
		session.save(per31_10_2017);
		session.save(per30_11_2019);

		create_LD_1_NormalTestLD();

		session.save(ld1);
		session.save(lds);
		session.save(ed_ld1_31032017_20102019);
		session.save(ed_ld1_31082017_20122019);
		session.save(ed_ld1_31102017_20112019);
		session.save(ed_ld1_30112019_20122019);

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
		CriteriaQuery<LD> cqLD = cb.createQuery(LD.class);
		Root<LD> rootLD = cqLD.from(LD.class);
		cqLD.select(rootLD);
		Query<LD> resQLD = session.createQuery(cqLD);
		alLD = resQLD.getResultList();
		//----------------get LDs from DB------------------------END

		transaction.commit();
		session.close();
		sessionFactory.close();

		for(LD ld : alLD) ld.calculate(SCENARIO_LOAD);
	}

	public void test1_NominalValue()
	{
		//Если более года:
		assertEquals(100000.0, alLD.get(0).getDeposit_sum_not_disc());

/*		//Если удаленный:
		assertEquals(100000.0, alLD.get(1).getDeposit_sum_not_disc());

		//Если менее года:
		assertEquals(100000.0, alLD.get(2).getDeposit_sum_not_disc());*/
	}

/*	public void test2_CalculatingDiscountedValue()
	{
		//Если более года:
		assertEquals(88027.3358353828, alLD.get(0).getDeposit_sum_discounted_on_firstEndDate());

*//*		//Если удаленный:
		assertEquals(100000.0, alLD.get(1).getDeposit_sum_discounted_on_firstEndDate());

		//Если менее года:
		assertEquals(100000.0, alLD.get(2).getDeposit_sum_discounted_on_firstEndDate());*//*
	}*/

	public static COUNTERPARTNER getCP(int id, String name)
	{
		return COUNTERPARTNER.builder()
								.id(id)
								.name(name)
								.build();
	}

	public static ENTITY getEN(int id, String code, String name)
	{
		return ENTITY.builder()
				.id(id)
				.code(code)
				.name(name)
				.build();
	}

	public static CURRENCY getCUR(int id, String name)
	{
		return CURRENCY.builder()
				.id(id)
				.short_name(name)
				.build();
	}

	public static SCENARIO getSC(int id, String name)
	{
		return SCENARIO.builder()
				.id(id)
				.name(name)
				.build();
	}

	public static PERIOD getPer(int id, int day, int month, int year)
	{
		return PERIOD.builder()
					.id(id)
					.date(getDate(day, month, year))
					.build();
	}

	public static Date getDate(int day, int month, int year)
	{
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

		Calendar date = Calendar.getInstance();
		date.set(year, month - 1, day, 0, 0, 0);
		date.clear(Calendar.MILLISECOND);

		return date.getTime();
	}

	public static void InitializeGeneraldata()
	{
		fact = getSC(1, "FACT");
		usd = getCUR(1, "USD");
		C1001 = getEN(1, "C1001", "Аэрофлот");
		CP = getCP(1, "ООО \"Авиакапитал-Сервис\"");

		per31_03_2017 = getPer(1, 31, 3, 2017);
		per31_08_2017 = getPer(2, 31, 8, 2017);
		per31_10_2017 = getPer(3, 31, 10, 2017);
		per30_11_2019 = getPer(4, 30, 11, 2019);
	}

	public static void create_LD_1_NormalTestLD()
	{
		//Депозит только для факта 1
		ld1 = LD.builder()
					.id(1)
					.counterpartner(CP)
					.entity(C1001)
					.currency(usd)
					.deposit_sum_not_disc(BigDecimal.valueOf(100000))
					.start_date(getDate(10, 3, 2017))
					.percent(BigDecimal.valueOf(5.0))
					.build();

		lds = LD_STATUS.builder()
					.id(1)
					.ld(ld1)
					.scenario(fact)
					.is_created(STATUS_X.X)
					.build();

		ld1.setStatuses(Set.of(lds));


		ed_ld1_31032017_20102019 = END_DATES.builder()
							.id(1)
							.ld(ld1)
							.scenario(fact)
							.period(per31_03_2017)
							.End_Date(getDate(20, 10, 2019))
							.build();

		ed_ld1_31082017_20122019 = END_DATES.builder()
							.id(2)
							.ld(ld1)
							.scenario(fact)
							.period(per31_08_2017)
							.End_Date(getDate(20, 12, 2019))
							.build();

		ed_ld1_31102017_20112019 = END_DATES.builder()
							.id(3)
							.ld(ld1)
							.scenario(fact)
							.period(per31_10_2017)
							.End_Date(getDate(20, 11, 2019))
							.build();

		ed_ld1_30112019_20122019 = END_DATES.builder()
							.id(4)
							.ld(ld1)
							.scenario(fact)
							.period(per30_11_2019)
							.End_Date(getDate(20, 12, 2019))
							.build();

/*		ld1.setEnd_dates(Set.of(ed_ld1_31032017_20102019,
								ed_ld1_31082017_20122019,
								ed_ld1_30112019_20122019,
								ed_ld1_31102017_20112019
		));*/
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
