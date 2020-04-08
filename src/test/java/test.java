import junit.framework.TestCase;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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
	static EXCHANGE_RATE ExRate10_03_2017USD, ExRate31_03_2017USD,
			ExRate30_04_2017USD, ExRate31_05_2017USD, ExRate30_06_2017USD, ExRate31_07_2017USD, ExRate31_08_2017USD, ExRate30_09_2017USD,
			ExRate31_10_2019USD, ExRate31_10_2017USD, ExRate30_11_2017USD, ExRate31_12_2017USD,	ExRate31_01_2018USD,
	 		ExRate28_02_2018USD, ExRate31_03_2018USD, ExRate30_04_2018USD, ExRate31_05_2018USD, ExRate30_06_2018USD,
			ExRate31_07_2018USD, ExRate31_08_2018USD, ExRate30_09_2018USD, ExRate31_10_2018USD, ExRate30_11_2018USD,
			ExRate31_12_2018USD, ExRate31_01_2019USD, ExRate28_02_2019USD, ExRate31_03_2019USD, ExRate30_04_2019USD,
			ExRate31_05_2019USD, ExRate30_06_2019USD, ExRate31_07_2019USD, ExRate31_08_2019USD, ExRate30_09_2019USD,
			ExRate30_11_2019USD;

	static ZonedDateTime firstOpenPeriod;

	LinkedList<LD> LDs = new LinkedList<>();
	GeneralDataKeeper GDK;

	final String SCENARIO_LOAD = "FACT";
	final String SCENARIO_SAVE = "FACT";

	public void setUp() throws BrokenBarrierException, InterruptedException
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

		GDK = GeneralDataKeeper.getInstance(sessionFactory, SCENARIO_LOAD, SCENARIO_SAVE);
		CyclicBarrier cyclicBarrier = new CyclicBarrier(GDK.getLDs().size() + 1);
		ExecutorService threadExecutor = Executors.newFixedThreadPool(10);

		LDs = GDK.getLDs();

		for (LD ld : LDs)
		{
			ld.registerGeneralData(GDK, cyclicBarrier, sessionFactory, GDK.getTo());
			threadExecutor.execute(ld);
		}

		cyclicBarrier.await();

		sessionFactory.close();
		threadExecutor.shutdown();
	}

	public void test1_NominalValue()
	{
		//Если более года:
		assertEquals(BigDecimal.valueOf(100000.00).setScale(2), LDs.get(0).getDeposit_sum_not_disc());

/*		//Если удаленный:
		assertEquals(100000.0, LDs.get(1).getDeposit_sum_not_disc());

		//Если менее года:
		assertEquals(100000.0, LDs.get(2).getDeposit_sum_not_disc());*/
	}

	public void test2_CalculatingDiscountedValue()
	{
		//Если более года:
		assertEquals(BigDecimal.valueOf(88027.34).setScale(2), LDs.get(0).getDeposit_sum_discounted_on_firstEndDate().setScale(2, RoundingMode.HALF_UP));

/*		//Если удаленный:
		assertEquals(100000.0, LDs.get(1).getDeposit_sum_discounted_on_firstEndDate());

		//Если менее года:
		assertEquals(100000.0, LDs.get(2).getDeposit_sum_discounted_on_firstEndDate());*/
	}

	public void test3_FirstOpenPeriodGlobal()
	{
		assertEquals(getDate(31,3,2020), GDK.getFirstOpenPeriod());
	}

	public void test4_FirstPeriodWithOutTransaction()
	{
		assertEquals(getDate(31,3,2017), LDs.get(0).getLastPeriodWithTransactionUTC());
	}

	public void test5_NumberOfNewTransactions()
	{
		assertEquals(33, LDs.get(0).getCalculatedTransactions().size());
	}

	public void test6_Reg_LD_1()
	{
		//31.03.2017
		var LD1_31032017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 3, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_31032017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31032017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31032017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5883180), LD1_31032017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 10, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31032017.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_31032017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//30.04.2017
		var LD1_30042017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 4, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_30042017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_30042017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 10, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_30042017.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_30042017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//31.05.2017
		var LD1_31052017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 5, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_31052017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31052017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 10, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31052017.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_31052017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//30.06.2017
		var LD1_30062017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 6, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_30062017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_30062017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 10, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_30062017.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_30062017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//31.07.2017
		var LD1_31072017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 7, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_31072017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31072017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 10, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31072017.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_31072017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//31.08.2017
		var LD1_31082017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 8, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_31082017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31082017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 12, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31082017.getEnd_date_at_this_period());
		assertEquals(BigDecimal.valueOf(-12688), LD1_31082017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-746430), LD1_31082017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-42056), LD1_31082017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-509), LD1_31082017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-821), LD1_31082017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-42565), LD1_31082017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-821), LD1_31082017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//30.09.2017
		var LD1_30092017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 9, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_30092017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_30092017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 12, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_30092017.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_30092017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//31.10.2017
		var LD1_31102017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 10, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_31102017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31102017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31102017.getEnd_date_at_this_period());
		assertEquals(BigDecimal.valueOf(-12337), LD1_31102017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-725789), LD1_31102017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(20641), LD1_31102017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-286), LD1_31102017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(563), LD1_31102017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(20355), LD1_31102017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(563), LD1_31102017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//30.11.2017
		var LD1_30112017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 11, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_30112017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_30112017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_30112017.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_30112017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//31.12.2017
		var LD1_31122017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 12, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_31122017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31122017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31122017.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_31122017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//31.01.2018
		var LD1_31012018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 1, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_31012018.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31012018.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31012018.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_31012018.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//28.02.2018
		var LD1_28022018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(28, 2, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_28022018.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_28022018.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_28022018.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_28022018.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//31.03.2018
		var LD1_31032018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 3, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_31032018.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31032018.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31032018.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_31032018.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//30.04.2018
		var LD1_30042018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 4, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_30042018.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_30042018.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_30042018.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_30042018.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//31.05.2018
		var LD1_31052018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 5, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_31052018.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31052018.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31052018.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_31052018.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//30.06.2018
		var LD1_30062018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 6, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_30062018.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_30062018.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_30062018.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_30062018.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//31.07.2018
		var LD1_31072018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 7, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_31072018.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31072018.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31072018.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_31072018.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//31.08.2018
		var LD1_31082018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 8, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_31082018.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31082018.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31082018.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_31082018.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//30.09.2018
		var LD1_30092018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 9, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_30092018.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_30092018.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_30092018.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_30092018.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//31.10.2018
		var LD1_31102018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 10, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_31102018.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31102018.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31102018.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_31102018.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//30.11.2018
		var LD1_30112018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 11, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_30112018.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_30112018.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112018.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112018.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_30112018.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_30112018.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112018.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112018.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112018.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112018.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112018.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112018.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112018.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112018.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//31.12.2018
		var LD1_31122018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 12, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_31122018.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31122018.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122018.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122018.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31122018.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_31122018.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122018.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122018.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122018.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122018.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122018.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122018.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122018.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122018.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//31.01.2019
		var LD1_31012019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 1, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_31012019.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31012019.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012019.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012019.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31012019.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_31012019.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012019.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012019.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012019.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012019.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012019.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012019.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012019.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012019.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//28.02.2019
		var LD1_28022019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(28, 2, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_28022019.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_28022019.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022019.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022019.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_28022019.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_28022019.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022019.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022019.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022019.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022019.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022019.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022019.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022019.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022019.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//31.03.2019
		var LD1_31032019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 3, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_31032019.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31032019.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032019.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032019.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31032019.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_31032019.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032019.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032019.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032019.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032019.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032019.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032019.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032019.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032019.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//30.04.2019
		var LD1_30042019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 4, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_30042019.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_30042019.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042019.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042019.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_30042019.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_30042019.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042019.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042019.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042019.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042019.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042019.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042019.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042019.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042019.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//31.05.2019
		var LD1_31052019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 5, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_31052019.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31052019.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052019.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052019.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31052019.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_31052019.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052019.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052019.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052019.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052019.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052019.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052019.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052019.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052019.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//30.06.2019
		var LD1_30062019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 6, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_30062019.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_30062019.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062019.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062019.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_30062019.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_30062019.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062019.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062019.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062019.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062019.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062019.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062019.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062019.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062019.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//31.07.2019
		var LD1_31072019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 7, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_31072019.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31072019.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072019.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072019.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31072019.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_31072019.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072019.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072019.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072019.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072019.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072019.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072019.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072019.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072019.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//31.08.2019
		var LD1_31082019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 8, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_31082019.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31082019.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082019.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082019.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31082019.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_31082019.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082019.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082019.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082019.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082019.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082019.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082019.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082019.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082019.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//30.09.2019
		var LD1_30092019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 9, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_30092019.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_30092019.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092019.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092019.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_30092019.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_30092019.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092019.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092019.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092019.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092019.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092019.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092019.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092019.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092019.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//31.10.2019
		var LD1_31102019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 10, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_31102019.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_31102019.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102019.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102019.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31102019.getEnd_date_at_this_period());
		assertEquals(BigDecimal.ZERO, LD1_31102019.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102019.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102019.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102019.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102019.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102019.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102019.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102019.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102019.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

		//30.11.2019
		var LD1_30112019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 11, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(-11973), LD1_30112019.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-704373), LD1_30112019.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112019.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112019.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(ZonedDateTime.of(2019, 11, 3, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_30112019.getEnd_date_at_this_period());
		assertEquals(BigDecimal.valueOf(-12137), LD1_30112019.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-714056), LD1_30112019.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(11733), LD1_30112019.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(1743), LD1_30112019.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(1855), LD1_30112019.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112019.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112019.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(13476), LD1_30112019.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(1855), LD1_30112019.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));
	}

	public void test7_Reg_LD_2()
	{
		//31.03.2017
		var LD1_31032017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 3, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.ZERO, LD1_31032017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(247), LD1_31032017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(247), LD1_31032017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(14379), LD1_31032017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(14379), LD1_31032017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//30.04.2017
		var LD1_30042017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 4, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(247), LD1_30042017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(355), LD1_30042017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(602), LD1_30042017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(14379), LD1_30042017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(20017), LD1_30042017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(34396), LD1_30042017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//31.05.2017
		var LD1_31052017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 5, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(602), LD1_31052017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(368), LD1_31052017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(970), LD1_31052017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(34396), LD1_31052017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(21041), LD1_31052017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(55436), LD1_31052017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//30.06.2017
		var LD1_30062017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 6, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(970), LD1_30062017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(358), LD1_30062017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(1328), LD1_30062017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(55436), LD1_30062017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(20681), LD1_30062017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(76117), LD1_30062017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//31.07.2017
		var LD1_31072017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 7, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(1328), LD1_31072017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(371), LD1_31072017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(1699), LD1_31072017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(76117), LD1_31072017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(22140), LD1_31072017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(98258), LD1_31072017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//31.08.2017
		var LD1_31082017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 8, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(1685), LD1_31082017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(370), LD1_31082017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(2055), LD1_31082017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(97460), LD1_31082017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(22044), LD1_31082017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(119504), LD1_31082017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//30.09.2017
		var LD1_30092017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 9, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(2055), LD1_30092017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(359), LD1_30092017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(2414), LD1_30092017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(119504), LD1_30092017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(20718), LD1_30092017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(140222), LD1_30092017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//31.10.2017
		var LD1_31102017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 10, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(2423), LD1_31102017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(374), LD1_31102017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(2797), LD1_31102017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(140785), LD1_31102017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(21596), LD1_31102017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(162381), LD1_31102017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//30.11.2017
		var LD1_30112017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 11, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(2797), LD1_30112017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(363), LD1_30112017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(3161), LD1_30112017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(162381), LD1_30112017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(21417), LD1_30112017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(183798), LD1_30112017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//31.12.2017
		var LD1_31122017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 12, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(3161), LD1_31122017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(377), LD1_31122017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(3538), LD1_31122017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(183798), LD1_31122017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(22096), LD1_31122017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(205894), LD1_31122017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//31.01.2018
		var LD1_31012018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 1, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(3538), LD1_31012018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(379), LD1_31012018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(3917), LD1_31012018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(205894), LD1_31012018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(21506), LD1_31012018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(227400), LD1_31012018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//28.02.2018
		var LD1_28022018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(28, 2, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(3917), LD1_28022018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(343), LD1_28022018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(4260), LD1_28022018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(227400), LD1_28022018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(19510), LD1_28022018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(246910), LD1_28022018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//31.03.2018
		var LD1_31032018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 3, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(4260), LD1_31032018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(382), LD1_31032018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(4642), LD1_31032018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(246910), LD1_31032018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(21770), LD1_31032018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(268680), LD1_31032018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//30.04.2018
		var LD1_30042018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 4, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(4642), LD1_30042018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(371), LD1_30042018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5013), LD1_30042018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(268680), LD1_30042018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(22426), LD1_30042018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(291106), LD1_30042018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//31.05.2018
		var LD1_31052018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 5, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(5013), LD1_31052018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(385), LD1_31052018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5398), LD1_31052018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(291106), LD1_31052018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(23940), LD1_31052018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(315046), LD1_31052018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//30.06.2018
		var LD1_30062018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 6, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(5398), LD1_30062018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(374), LD1_30062018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5772), LD1_30062018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(315046), LD1_30062018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(23451), LD1_30062018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(338497), LD1_30062018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//31.07.2018
		var LD1_31072018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 7, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(5772), LD1_31072018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(388), LD1_31072018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(6160), LD1_31072018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(338497), LD1_31072018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(24397), LD1_31072018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(362894), LD1_31072018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//31.08.2018
		var LD1_31082018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 8, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(6160), LD1_31082018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(390), LD1_31082018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(6549), LD1_31082018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(362894), LD1_31082018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(25761), LD1_31082018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(388655), LD1_31082018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//30.09.2018
		var LD1_30092018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 9, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(6549), LD1_30092018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(379), LD1_30092018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(6928), LD1_30092018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(388655), LD1_30092018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(25614), LD1_30092018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(414269), LD1_30092018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//31.10.2018
		var LD1_31102018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 10, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(6928), LD1_31102018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(393), LD1_31102018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(7321), LD1_31102018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(414269), LD1_31102018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(25879), LD1_31102018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(440148), LD1_31102018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//30.11.2018
		var LD1_30112018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 11, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(7321), LD1_30112018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(382), LD1_30112018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(7702), LD1_30112018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(440148), LD1_30112018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(25147), LD1_30112018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(465295), LD1_30112018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//31.12.2018
		var LD1_31122018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 12, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(7702), LD1_31122018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(396), LD1_31122018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(8098), LD1_31122018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(465295), LD1_31122018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(26091), LD1_31122018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(491386), LD1_31122018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//31.01.2019
		var LD1_31012019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 1, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(8098), LD1_31012019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(398), LD1_31012019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(8496), LD1_31012019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(491386), LD1_31012019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(26199), LD1_31012019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(517585), LD1_31012019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//28.02.2019
		var LD1_28022019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(28, 2, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(8496), LD1_28022019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(361), LD1_28022019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(8856), LD1_28022019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(517585), LD1_28022019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(23757), LD1_28022019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(541342), LD1_28022019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//31.03.2019
		var LD1_31032019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 3, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(8856), LD1_31032019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(401), LD1_31032019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(9257), LD1_31032019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(541342), LD1_31032019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(26407), LD1_31032019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(567749), LD1_31032019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//30.04.2019
		var LD1_30042019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 4, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(9257), LD1_30042019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(389), LD1_30042019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(9647), LD1_30042019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(567749), LD1_30042019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(25659), LD1_30042019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(593409), LD1_30042019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//31.05.2019
		var LD1_31052019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 5, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(9647), LD1_31052019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(404), LD1_31052019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(10051), LD1_31052019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(593409), LD1_31052019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(26623), LD1_31052019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(620032), LD1_31052019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//30.06.2019
		var LD1_30062019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 6, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(10051), LD1_30062019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(393), LD1_30062019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(10443), LD1_30062019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(620032), LD1_30062019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(25869), LD1_30062019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(645901), LD1_30062019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//31.07.2019
		var LD1_31072019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 7, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(10443), LD1_31072019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(407), LD1_31072019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(10851), LD1_31072019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(645901), LD1_31072019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(26841), LD1_31072019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(672742), LD1_31072019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//31.08.2019
		var LD1_31082019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 8, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(10851), LD1_31082019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(409), LD1_31082019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(11260), LD1_31082019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(672742), LD1_31082019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(26952), LD1_31082019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(699694), LD1_31082019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//30.09.2019
		var LD1_30092019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 9, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(11260), LD1_30092019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(397), LD1_30092019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(11657), LD1_30092019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(699694), LD1_30092019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(26190), LD1_30092019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(725884), LD1_30092019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//31.10.2019
		var LD1_31102019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 10, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(11657), LD1_31102019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(412), LD1_31102019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(12070), LD1_31102019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(725884), LD1_31102019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(27173), LD1_31102019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(753057), LD1_31102019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

		//30.11.2019
		var LD1_30112019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 11, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(12097), LD1_30112019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(40), LD1_30112019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(12137), LD1_30112019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(754770), LD1_30112019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(2539), LD1_30112019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(757310), LD1_30112019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));
	}

	public void test8_Reg_LD_3()
	{
		//31.03.2017
		var LD1_31032017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 3, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(88027), LD1_31032017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_31032017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(4962796), LD1_31032017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-216010), LD1_31032017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(13951), LD1_31032017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-428), LD1_31032017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(216439), LD1_31032017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_31032017.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_31032017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//30.04.2017
		var LD1_30042017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 4, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(88027), LD1_30042017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(4962796), LD1_30042017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5016133), LD1_30042017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(53336), LD1_30042017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(13951), LD1_30042017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(34313), LD1_30042017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(346), LD1_30042017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-53682), LD1_30042017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_30042017.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_30042017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//31.05.2017
		var LD1_31052017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 5, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(88027), LD1_31052017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5016133), LD1_31052017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(4975023), LD1_31052017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-41109), LD1_31052017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(34313), LD1_31052017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(54832), LD1_31052017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-522), LD1_31052017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(41632), LD1_31052017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_31052017.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_31052017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//30.06.2017
		var LD1_30062017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 6, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(88027), LD1_30062017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(4975023), LD1_30062017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5201139), LD1_30062017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(226116), LD1_30062017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(54832), LD1_30062017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(78453), LD1_30062017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(2941), LD1_30062017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-229057), LD1_30062017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_30062017.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_30062017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//31.07.2017
		var LD1_31072017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 7, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(88027), LD1_31072017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5201139), LD1_31072017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5241464), LD1_31072017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(40325), LD1_31072017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(78453), LD1_31072017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(101155), LD1_31072017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(561), LD1_31072017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-40886), LD1_31072017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_31072017.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_31072017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//31.08.2017
		var LD1_31082017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 8, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87312), LD1_31082017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5198899), LD1_31082017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5127914), LD1_31082017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-70985), LD1_31082017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(100333), LD1_31082017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(120667), LD1_31082017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-1710), LD1_31082017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(72694), LD1_31082017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_31082017.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_31082017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//30.09.2017
		var LD1_30092017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 9, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87312), LD1_30092017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5127914), LD1_30092017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5065600), LD1_30092017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-62315), LD1_30092017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(120667), LD1_30092017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(140035), LD1_30092017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-1351), LD1_30092017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(63666), LD1_30092017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_30092017.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_30092017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//31.10.2017
		var LD1_31102017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 10, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_31102017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5085954), LD1_31102017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5073217), LD1_31102017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-12737), LD1_31102017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(140597), LD1_31102017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(161894), LD1_31102017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-299), LD1_31102017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(13037), LD1_31102017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_31102017.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_31102017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//30.11.2017
		var LD1_30112017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 11, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_30112017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5073217), LD1_30112017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5113498), LD1_30112017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(40281), LD1_30112017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(161894), LD1_30112017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(184382), LD1_30112017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(1071), LD1_30112017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-41352), LD1_30112017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_30112017.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_30112017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//31.12.2017
		var LD1_31122017 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 12, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_31122017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5113498), LD1_31122017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5049425), LD1_31122017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-64073), LD1_31122017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(184382), LD1_31122017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(203795), LD1_31122017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-2683), LD1_31122017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(66756), LD1_31122017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_31122017.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_31122017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//31.01.2018
		var LD1_31012018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 1, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_31012018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5049425), LD1_31012018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(4934691), LD1_31012018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-114734), LD1_31012018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(203795), LD1_31012018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(220482), LD1_31012018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-4819), LD1_31012018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(119552), LD1_31012018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_31012018.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_31012018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//28.02.2018
		var LD1_28022018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(28, 2, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_28022018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(4934691), LD1_28022018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(4880366), LD1_28022018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-54325), LD1_28022018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(220482), LD1_28022018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(237173), LD1_28022018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-2819), LD1_28022018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(57144), LD1_28022018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_28022018.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_28022018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//31.03.2018
		var LD1_31032018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 3, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_31032018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(4880366), LD1_31032018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5020031), LD1_31032018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(139665), LD1_31032018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(237173), LD1_31032018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(265819), LD1_31032018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(6875), LD1_31032018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-146541), LD1_31032018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_31032018.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_31032018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//30.04.2018
		var LD1_30042018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 4, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_30042018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5020031), LD1_30042018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5435100), LD1_30042018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(415068), LD1_30042018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(265819), LD1_30042018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(310793), LD1_30042018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(22549), LD1_30042018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-437617), LD1_30042018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_30042018.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_30042018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//31.05.2018
		var LD1_31052018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 5, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_31052018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5435100), LD1_31052018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5487172), LD1_31052018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(52072), LD1_31052018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(310793), LD1_31052018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(337859), LD1_31052018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(3126), LD1_31052018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-55198), LD1_31052018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_31052018.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_31052018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//30.06.2018
		var LD1_30062018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 6, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_30062018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5487172), LD1_30062018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5501443), LD1_30062018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(14272), LD1_30062018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(337859), LD1_30062018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(362204), LD1_30062018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(895), LD1_30062018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-15166), LD1_30062018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_30062018.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_30062018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//31.07.2018
		var LD1_31072018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 7, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_31072018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5501443), LD1_31072018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5503547), LD1_31072018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(2104), LD1_31072018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(362204), LD1_31072018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(386700), LD1_31072018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(99), LD1_31072018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-2203), LD1_31072018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_31072018.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_31072018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//31.08.2018
		var LD1_31082018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 8, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_31082018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5503547), LD1_31082018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5968303), LD1_31082018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(464756), LD1_31082018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(386700), LD1_31082018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(445880), LD1_31082018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(33419), LD1_31082018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-498175), LD1_31082018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_31082018.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_31082018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//30.09.2018
		var LD1_30092018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 9, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_30092018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5968303), LD1_30092018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5749890), LD1_30092018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-218413), LD1_30092018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(445880), LD1_30092018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(454393), LD1_30092018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-17101), LD1_30092018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(235514), LD1_30092018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_30092018.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_30092018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//31.10.2018
		var LD1_31102018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 10, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_31102018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5749890), LD1_31102018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5765985), LD1_31102018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(16095), LD1_31102018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(454393), LD1_31102018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(481500), LD1_31102018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(1228), LD1_31102018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-17323), LD1_31102018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.LT, LD1_31102018.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_31102018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//30.11.2018
		var LD1_30112018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 11, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_30112018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5765985), LD1_30112018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5841375), LD1_30112018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(75390), LD1_30112018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(481500), LD1_30112018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(513228), LD1_30112018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(6581), LD1_30112018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-81971), LD1_30112018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.ST, LD1_30112018.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.valueOf(5841375), LD1_30112018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(513228), LD1_30112018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_30112018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//31.12.2018
		var LD1_31122018 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 12, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_31122018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5841375), LD1_31122018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(6090024), LD1_31122018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(248648), LD1_31122018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(513228), LD1_31122018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(562584), LD1_31122018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(23266), LD1_31122018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-271914), LD1_31122018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31122018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.ST, LD1_31122018.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.valueOf(6090024), LD1_31122018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(562584), LD1_31122018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5841375), LD1_31122018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(513228), LD1_31122018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_31122018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_31122018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//31.01.2019
		var LD1_31012019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 1, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_31012019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(6090024), LD1_31012019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5924243), LD1_31012019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-165780), LD1_31012019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(562584), LD1_31012019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(574142), LD1_31012019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-14641), LD1_31012019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(180421), LD1_31012019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31012019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.ST, LD1_31012019.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.valueOf(5924243), LD1_31012019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(574142), LD1_31012019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(6090024), LD1_31012019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(562584), LD1_31012019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_31012019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_31012019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//28.02.2019
		var LD1_28022019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(28, 2, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_28022019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5924243), LD1_28022019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(6009321), LD1_28022019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(85077), LD1_28022019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(574142), LD1_28022019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(607105), LD1_28022019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(9205), LD1_28022019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-94283), LD1_28022019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_28022019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.ST, LD1_28022019.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.valueOf(6009321), LD1_28022019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(607105), LD1_28022019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5924243), LD1_28022019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(574142), LD1_28022019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_28022019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_28022019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//31.03.2019
		var LD1_31032019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 3, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_31032019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(6009321), LD1_31032019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5990035), LD1_31032019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-19286), LD1_31032019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(607105), LD1_31032019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(632543), LD1_31032019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-969), LD1_31032019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(20255), LD1_31032019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31032019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.ST, LD1_31032019.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.valueOf(5990035), LD1_31032019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(632543), LD1_31032019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(6009321), LD1_31032019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(607105), LD1_31032019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_31032019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_31032019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//30.04.2019
		var LD1_30042019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 4, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_30042019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5990035), LD1_30042019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923411), LD1_30042019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-66624), LD1_30042019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(632543), LD1_30042019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(651822), LD1_30042019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-6380), LD1_30042019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(73004), LD1_30042019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30042019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.ST, LD1_30042019.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.valueOf(5923411), LD1_30042019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(651822), LD1_30042019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5990035), LD1_30042019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(632543), LD1_30042019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_30042019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_30042019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//31.05.2019
		var LD1_31052019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 5, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_31052019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923411), LD1_31052019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923411), LD1_31052019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(651822), LD1_31052019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(679125), LD1_31052019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(680), LD1_31052019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-680), LD1_31052019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31052019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.ST, LD1_31052019.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.valueOf(5923411), LD1_31052019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(679125), LD1_31052019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923411), LD1_31052019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(651822), LD1_31052019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_31052019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_31052019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//30.06.2019
		var LD1_30062019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 6, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_30062019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923411), LD1_30062019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923411), LD1_30062019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(679125), LD1_30062019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(705656), LD1_30062019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(661), LD1_30062019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-661), LD1_30062019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30062019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.ST, LD1_30062019.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.valueOf(5923411), LD1_30062019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(705656), LD1_30062019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923411), LD1_30062019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(679125), LD1_30062019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_30062019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_30062019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//31.07.2019
		var LD1_31072019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 7, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_31072019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923411), LD1_31072019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923411), LD1_31072019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(705656), LD1_31072019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(733182), LD1_31072019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(686), LD1_31072019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-686), LD1_31072019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31072019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.ST, LD1_31072019.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.valueOf(5923411), LD1_31072019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(733182), LD1_31072019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923411), LD1_31072019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(705656), LD1_31072019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_31072019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_31072019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//31.08.2019
		var LD1_31082019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 8, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_31082019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923411), LD1_31082019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923411), LD1_31082019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(733182), LD1_31082019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(760823), LD1_31082019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(689), LD1_31082019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-689), LD1_31082019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31082019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.ST, LD1_31082019.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.valueOf(5923411), LD1_31082019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(760823), LD1_31082019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923411), LD1_31082019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(733182), LD1_31082019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_31082019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_31082019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//30.09.2019
		var LD1_30092019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 9, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_30092019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923411), LD1_30092019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923411), LD1_30092019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(760823), LD1_30092019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(787682), LD1_30092019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(669), LD1_30092019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-669), LD1_30092019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30092019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.ST, LD1_30092019.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.valueOf(5923411), LD1_30092019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(787682), LD1_30092019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923411), LD1_30092019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(760823), LD1_30092019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_30092019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_30092019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//31.10.2019
		var LD1_31102019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(31, 10, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87663), LD1_31102019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923411), LD1_31102019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923411), LD1_31102019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(787682), LD1_31102019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(815549), LD1_31102019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(694), LD1_31102019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-694), LD1_31102019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_31102019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.ST, LD1_31102019.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.valueOf(5923411), LD1_31102019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(815549), LD1_31102019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923411), LD1_31102019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(787682), LD1_31102019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_31102019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178807), LD1_31102019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

		//30.11.2019
		var LD1_30112019 = LDs.get(0).getCalculatedTransactions().stream()
				.filter(tr -> tr.getPeriod().getDate().isEqual(getDate(30, 11, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
				.filter(tr -> tr.getScenario().getName().equals("FACT"))
				.filter(tr -> tr.getStatus().equals(TRAN_STATUS.ACTUAL))
				.collect(Collectors.toList()).get(0);

		assertEquals(BigDecimal.valueOf(87863), LD1_30112019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5936886), LD1_30112019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5694508), LD1_30112019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-242378), LD1_30112019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(817405), LD1_30112019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(786632), LD1_30112019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(-33312), LD1_30112019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(275690), LD1_30112019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5694508), LD1_30112019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(786632), LD1_30112019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
		assertEquals(TERM_ST_LT.ST, LD1_30112019.getLDTERM_REG_LD_3_Z());
		assertEquals(BigDecimal.ZERO, LD1_30112019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.ZERO, LD1_30112019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5923410.57179), LD1_30112019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(5, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(815549.13522), LD1_30112019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(5, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178806.61640), LD1_30112019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(4, RoundingMode.HALF_UP));
		assertEquals(BigDecimal.valueOf(5178806.61640), LD1_30112019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(4, RoundingMode.HALF_UP));
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

	public static SCENARIO getSC(int id, String name, STORNO_SCENARIO_STATUS status)
	{
		SCENARIO c = new SCENARIO();
		c.setId(id);
		c.setName(name);
		c.setStatus(status);
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

	public static EXCHANGE_RATE getExRate(int id, SCENARIO scenario, ZonedDateTime date, BigDecimal rate_at_date, BigDecimal average_rate_for_month)
	{
		EXCHANGE_RATE exchange_rate = new EXCHANGE_RATE();
		exchange_rate.setId(id);
		exchange_rate.setCurrency(usd);
		exchange_rate.setScenario(scenario);
		exchange_rate.setDate(date);
		exchange_rate.setRate_at_date(rate_at_date);
		exchange_rate.setAverage_rate_for_month(average_rate_for_month);

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
		fact = getSC(1, "FACT", STORNO_SCENARIO_STATUS.ADDITION);
		usd = getCUR(1, "USD");
		C1001 = getEN(1, "C1001", "Компания-1");
		CP = getCP(1, "ООО \"Лизинговая компания\"");

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

		ExRate10_03_2017USD = getExRate(1, fact, getDate(10, 3, 2017), BigDecimal.valueOf(58.8318), BigDecimal.ZERO);
		ExRate31_03_2017USD = getExRate(2, fact, getDate(31, 3, 2017), BigDecimal.valueOf(56.3779), BigDecimal.valueOf(58.1091));
		ExRate30_04_2017USD = getExRate(3, fact, getDate(30, 4, 2017), BigDecimal.valueOf(56.9838082901554), BigDecimal.valueOf(56.4315074286036));
		ExRate31_05_2017USD = getExRate(4, fact, getDate(31, 5, 2017), BigDecimal.valueOf(56.5168010059989), BigDecimal.valueOf(57.171996848083));
		ExRate30_06_2017USD = getExRate(5, fact, getDate(30, 6, 2017), BigDecimal.valueOf(59.0855029786337), BigDecimal.valueOf(57.8311009199966));
		ExRate31_07_2017USD = getExRate(6, fact, getDate(31, 7, 2017), BigDecimal.valueOf(59.543597652502), BigDecimal.valueOf(59.6707093574817));
		ExRate31_08_2017USD = getExRate(7, fact, getDate(31, 8, 2017), BigDecimal.valueOf(58.7306), BigDecimal.valueOf(59.6497128133555));
		ExRate30_09_2017USD = getExRate(8, fact, getDate(30, 9, 2017), BigDecimal.valueOf(58.0168999895548), BigDecimal.valueOf(57.6953966972068));
		ExRate31_10_2017USD = getExRate(9, fact, getDate(31, 10, 2017), BigDecimal.valueOf(57.8716), BigDecimal.valueOf(57.7305008320361));
		ExRate30_11_2017USD = getExRate(10, fact, getDate(30, 11, 2017), BigDecimal.valueOf(58.3311), BigDecimal.valueOf(58.9212082863353));
		ExRate31_12_2017USD = getExRate(11, fact, getDate(31, 12, 2017), BigDecimal.valueOf(57.6002), BigDecimal.valueOf(58.5887999151509));
		ExRate31_01_2018USD = getExRate(12, fact, getDate(31, 1, 2018), BigDecimal.valueOf(56.2914), BigDecimal.valueOf(56.7874891077606));
		ExRate28_02_2018USD = getExRate(13, fact, getDate(28, 2, 2018), BigDecimal.valueOf(55.6717), BigDecimal.valueOf(56.8124108208847));
		ExRate31_03_2018USD = getExRate(14, fact, getDate(31, 3, 2018), BigDecimal.valueOf(57.2649), BigDecimal.valueOf(57.0343978412931));
		ExRate30_04_2018USD = getExRate(15, fact, getDate(30, 4, 2018), BigDecimal.valueOf(61.9997), BigDecimal.valueOf(60.4623078997034));
		ExRate31_05_2018USD = getExRate(16, fact, getDate(31, 5, 2018), BigDecimal.valueOf(62.5937), BigDecimal.valueOf(62.2090013772315));
		ExRate30_06_2018USD = getExRate(17, fact, getDate(30, 6, 2018), BigDecimal.valueOf(62.7565), BigDecimal.valueOf(62.7143124565438));
		ExRate31_07_2018USD = getExRate(18, fact, getDate(31, 7, 2018), BigDecimal.valueOf(62.7805), BigDecimal.valueOf(62.8828032372803));
		ExRate31_08_2018USD = getExRate(19, fact, getDate(31, 8, 2018), BigDecimal.valueOf(68.0821), BigDecimal.valueOf(66.1231037757643));
		ExRate30_09_2018USD = getExRate(20, fact, getDate(30, 9, 2018), BigDecimal.valueOf(65.5906), BigDecimal.valueOf(67.6597104818259));
		ExRate31_10_2018USD = getExRate(21, fact, getDate(31, 10, 2018), BigDecimal.valueOf(65.7742), BigDecimal.valueOf(65.8868068638933));
		ExRate30_11_2018USD = getExRate(22, fact, getDate(30, 11, 2018), BigDecimal.valueOf(66.6342), BigDecimal.valueOf(65.8868102499607));
		ExRate31_12_2018USD = getExRate(23, fact, getDate(31, 12, 2018), BigDecimal.valueOf(69.4706), BigDecimal.valueOf(65.8867929292929));
		ExRate31_01_2019USD = getExRate(24, fact, getDate(31, 1, 2019), BigDecimal.valueOf(67.5795), BigDecimal.valueOf(65.8868071622573));
		ExRate28_02_2019USD = getExRate(25, fact, getDate(28, 2, 2019), BigDecimal.valueOf(68.5500), BigDecimal.valueOf(65.8867934993621));
		ExRate31_03_2019USD = getExRate(26, fact, getDate(31, 3, 2019), BigDecimal.valueOf(68.3300), BigDecimal.valueOf(65.8867985728187));
		ExRate30_04_2019USD = getExRate(27, fact, getDate(30, 4, 2019), BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.886789061497));
		ExRate31_05_2019USD = getExRate(28, fact, getDate(31, 5, 2019), BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8868017917687));
		ExRate30_06_2019USD = getExRate(29, fact, getDate(30, 6, 2019), BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867890889642));
		ExRate31_07_2019USD = getExRate(30, fact, getDate(31, 7, 2019), BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867887476067));
		ExRate31_08_2019USD = getExRate(31, fact, getDate(31, 8, 2019), BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867919915907));
		ExRate30_09_2019USD = getExRate(32, fact, getDate(30, 9, 2019), BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867896047699));
		ExRate31_10_2019USD = getExRate(33, fact, getDate(31, 10, 2019), BigDecimal.valueOf(67.5699997265878), BigDecimal.valueOf(65.8867901653654));
		ExRate30_11_2019USD = getExRate(34, fact, getDate(30, 11, 2019), BigDecimal.valueOf(64.8114), BigDecimal.valueOf(63.3386));

		session.save(fact);
		session.save(usd);
		session.save(C1001);
		session.save(CP);
		periods.values().stream().forEach(value -> session.save(value));

		IFRS_ACCOUNT A0203010100_F2000 = new IFRS_ACCOUNT(0,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F2000", "Поступление", "-", "THP99", "RUB", "-", 	false, "Reg.LD.1=N5");

		IFRS_ACCOUNT A0208010000_F2000 = new IFRS_ACCOUNT(1,
				"A0208010000",
				"Долгосрочные авансы выданные",
				"F2000", "Поступление", "-", "THP99", "RUB", "-", true, "Reg.LD.1=N5");

		IFRS_ACCOUNT A0208010000_F2000_2 = new IFRS_ACCOUNT(2,
				"A0208010000",
				"Долгосрочные авансы выданные",
				"F2000", "Поступление", "-", "THP99", "RUB", "-", true, "Reg.LD.1=M5");

		IFRS_ACCOUNT A0203010100_F2000_2 = new IFRS_ACCOUNT(3,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F2000", "Поступление", "-", "THP99", "RUB", "-", 	false, "Reg.LD.1=M5");

		IFRS_ACCOUNT P0302990000_Y9900 = new IFRS_ACCOUNT(4,
				"P0302990000",
				"Прочие финансовые расходы",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", 	true, "Reg.LD.1=U5 + Reg.LD.1=V5");

		IFRS_ACCOUNT A0203010100_F2700 = new IFRS_ACCOUNT(5,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB", "-", 	false, "Reg.LD.1=U5");

		IFRS_ACCOUNT A0203010200_F2700 = new IFRS_ACCOUNT(6,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB", "-", 	false, "Reg.LD.1=V5");

		IFRS_ACCOUNT P0301990000_Y9900 = new IFRS_ACCOUNT(7,
				"P0301990000",
				"Прочие финансовые доходы",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", 	true, "Reg.LD.1=W5 + Reg.LD.1=X5");

		IFRS_ACCOUNT A0203010100_F2700_2 = new IFRS_ACCOUNT(8,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB", "-", 	false, "Reg.LD.1=W5");

		IFRS_ACCOUNT A0203010200_F2700_2 = new IFRS_ACCOUNT(9,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB", "-", 	false, "Reg.LD.1=X5");

		IFRS_ACCOUNT A0203010200_F2700_3 = new IFRS_ACCOUNT(10,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB", "-", 	false, "Reg.LD.2=M5");

		IFRS_ACCOUNT P0301020000_Y9900 = new IFRS_ACCOUNT(11,
				"P0301020000",
				"Процентные доходы по страховым депозитам",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", 	true, "Reg.LD.2=M5");

		IFRS_ACCOUNT A0203010100_F2700_3 = new IFRS_ACCOUNT(12,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F1600", "Курсовая разница", "-", "THP99", "RUB", "-", 	false, "Reg.LD.3=N5");

		IFRS_ACCOUNT A0203010100_F2700_4 = new IFRS_ACCOUNT(13,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F1600", "Курсовая разница", "-", "THP99", "RUB", "-", 	false, "Reg.LD.3=O5");

		IFRS_ACCOUNT A0203010200_F2700_4 = new IFRS_ACCOUNT(14,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F1600", "Курсовая разница", "-", "THP99", "RUB", "-", 	false, "Reg.LD.3=T5");

		IFRS_ACCOUNT A0203010200_F2700_5 = new IFRS_ACCOUNT(15,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F1600", "Курсовая разница", "-", "THP99", "RUB", "-", 	false, "Reg.LD.3=U5");

		IFRS_ACCOUNT P0301310000_Y9900 = new IFRS_ACCOUNT(16,
				"P0301310000",
				"Положительные курсовые разницы",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", 	false, "Reg.LD.3=V5");

		IFRS_ACCOUNT P0302310000_Y9900 = new IFRS_ACCOUNT(17,
				"P0302310000",
				"Отрицательные курсовые разницы",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", 	false, "Reg.LD.3=W5");

		IFRS_ACCOUNT A0107010000_F3000 = new IFRS_ACCOUNT(18,
				"A0107010000",
				"Краткосрочные авансы выданные",
				"F3000", "Выбытие", "-", "THP99", "RUB", "-", false, "Reg.LD.3=X5 + Reg.LD.3=Y5");

		IFRS_ACCOUNT A0203010100_F3000 = new IFRS_ACCOUNT(19,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F3000", "Выбытие", "-", "THP99", "RUB", "-", 	true, "Reg.LD.3=X5");

		IFRS_ACCOUNT A0203010200_F3000 = new IFRS_ACCOUNT(20,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F3000", "Выбытие", "-", "THP99", "RUB", "-", 	true, "Reg.LD.3=Y5");

		IFRS_ACCOUNT P0301310000_Y9900_2 = new IFRS_ACCOUNT(21,
				"P0301310000",
				"Положительные курсовые разницы",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", 	false, "Reg.LD.4_MA_AFL=B1");

		IFRS_ACCOUNT P0302310000_Y9900_2 = new IFRS_ACCOUNT(22,
				"P0302310000",
				"Отрицательные курсовые разницы",
				"Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", 	false, "Reg.LD.4_MA_AFL=C1");

		IFRS_ACCOUNT A0107010000_F1600 = new IFRS_ACCOUNT(23,
				"A0107010000",
				"Краткосрочные авансы выданные",
				"F1600", "Курсовая разница", "-", "THP99", "RUB", "-", false, "Reg.LD.4_MA_AFL=A1");

		IFRS_ACCOUNT A0215010100_F2006 = new IFRS_ACCOUNT(24,
				"A0215010100",
				"АПП воздушные суда и авиационные двигатели - ПСт",
				"F2006", "Ввод в эксплуатацию", "-", "THP99", "-", "-", false, "APP-1");

		IFRS_ACCOUNT A0215020100_F2006 = new IFRS_ACCOUNT(25,
				"A0215020100",
				"АПП земля - ПСт",
				"F2006", "Ввод в эксплуатацию", "-", "THP99", "-", "-", false, "APP-2");

		IFRS_ACCOUNT A0215030100_F2006 = new IFRS_ACCOUNT(26,
				"A0215030100",
				"АПП здания - ПСт",
				"F2006", "Ввод в эксплуатацию", "-", "THP99", "-", "-", false, "APP-3");

		IFRS_ACCOUNT A0215040100_F2006 = new IFRS_ACCOUNT(27,
				"A0215040100",
				"АПП машины и оборудование - ПСт",
				"F2006", "Ввод в эксплуатацию", "-", "THP99", "-", "-", false, "APP-4");

		IFRS_ACCOUNT A0215050100_F2006 = new IFRS_ACCOUNT(28,
				"A0215050100",
				"АПП прочие ОС - ПСт",
				"F2006", "Ввод в эксплуатацию", "-", "THP99", "-", "-", false, "APP-5");

		IFRS_ACCOUNT A0215060100_F2006 = new IFRS_ACCOUNT(29,
				"A0215060100",
				"АПП незавершенное строительство - первоначальная стоимость",
				"F2006", "Ввод в эксплуатацию", "-", "THP99", "-", "-", false, "APP-6");

		IFRS_ACCOUNT A0208010000_F3000 = new IFRS_ACCOUNT(30,
				"A0208010000",
				"Долгосрочные авансы выданные",
				"F3000", "Выбытие", "-", "THP99", "RUB", "-", true, "APP-7");

		IFRS_ACCOUNT A0208010000_F5000 = new IFRS_ACCOUNT(31,
				"A0208010000",
				"Долгосрочные авансы выданные",
				"F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB", "-", true, "Reg.LD.3=AE5-Reg.LD.3=AF5");

		IFRS_ACCOUNT A0107010000_F5000 = new IFRS_ACCOUNT(32,
				"A0107010000",
				"Краткосрочные авансы выданные",
				"F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB", "-", false, "Reg.LD.3=AE5-Reg.LD.3=AF5");

		IFRS_ACCOUNT A0203010100_F5000 = new IFRS_ACCOUNT(33,
				"A0203010100",
				"Долгосрочные депозиты по аренде ВС - основная сумма",
				"F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB", "-", true, "Reg.LD.3=AA5-Reg.LD.3=AC5");

		IFRS_ACCOUNT A0102010100_F5000 = new IFRS_ACCOUNT(34,
				"A0102010100",
				"Краткосрочные депозиты по аренде ВС - основная сумма",
				"F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB", "-", false, "Reg.LD.3=AA5-Reg.LD.3=AC5");

		IFRS_ACCOUNT A0203010200_F5000 = new IFRS_ACCOUNT(35,
				"A0203010200",
				"Долгосрочные депозиты по аренде ВС - проценты",
				"F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB", "-", true, "Reg.LD.3=AB5-Reg.LD.3=AD5");

		IFRS_ACCOUNT A0102010200_F5000 = new IFRS_ACCOUNT(36,
				"A0102010200",
				"Краткосрочные депозиты по аренде ВС - проценты",
				"F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB", "-", false, "Reg.LD.3=AB5-Reg.LD.3=AD5");

		session.save(A0203010100_F2000);
		session.save(A0208010000_F2000);
		session.save(A0208010000_F2000_2);
		session.save(A0203010100_F2000_2);
		session.save(P0302990000_Y9900);
		session.save(A0203010100_F2700);
		session.save(A0203010200_F2700);
		session.save(P0301990000_Y9900);
		session.save(A0203010100_F2700_2);
		session.save(A0203010200_F2700_2);
		session.save(A0203010200_F2700_3);
		session.save(P0301020000_Y9900);
		session.save(A0203010100_F2700_3);
		session.save(A0203010100_F2700_4);
		session.save(A0203010200_F2700_4);
		session.save(A0203010200_F2700_5);
		session.save(P0301310000_Y9900);
		session.save(P0302310000_Y9900);
		session.save(A0107010000_F3000);
		session.save(A0203010100_F3000);
		session.save(A0203010200_F3000);
		session.save(P0301310000_Y9900_2);
		session.save(P0302310000_Y9900_2);
		session.save(A0107010000_F1600);
		session.save(A0215010100_F2006);
		session.save(A0215020100_F2006);
		session.save(A0215030100_F2006);
		session.save(A0215040100_F2006);
		session.save(A0215050100_F2006);
		session.save(A0215060100_F2006);
		session.save(A0208010000_F3000);
		session.save(A0208010000_F5000);
		session.save(A0107010000_F5000);
		session.save(A0203010100_F5000);
		session.save(A0102010100_F5000);
		session.save(A0203010200_F5000);
		session.save(A0102010200_F5000);

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
		session.save(ExRate31_03_2017USD);
		session.save(ExRate30_04_2017USD);
		session.save(ExRate31_05_2017USD);
		session.save(ExRate30_06_2017USD);
		session.save(ExRate31_07_2017USD);
		session.save(ExRate31_08_2017USD);
		session.save(ExRate30_09_2017USD);
		session.save(ExRate31_10_2017USD);
		session.save(ExRate30_11_2017USD);
		session.save(ExRate31_12_2017USD);
		session.save(ExRate31_01_2018USD);
		session.save(ExRate28_02_2018USD);
		session.save(ExRate31_03_2018USD);
		session.save(ExRate30_04_2018USD);
		session.save(ExRate31_05_2018USD);
		session.save(ExRate30_06_2018USD);
		session.save(ExRate31_07_2018USD);
		session.save(ExRate31_08_2018USD);
		session.save(ExRate30_09_2018USD);
		session.save(ExRate31_10_2018USD);
		session.save(ExRate30_11_2018USD);
		session.save(ExRate31_12_2018USD);
		session.save(ExRate31_01_2019USD);
		session.save(ExRate28_02_2019USD);
		session.save(ExRate31_03_2019USD);
		session.save(ExRate30_04_2019USD);
		session.save(ExRate31_05_2019USD);
		session.save(ExRate30_06_2019USD);
		session.save(ExRate31_07_2019USD);
		session.save(ExRate31_08_2019USD);
		session.save(ExRate30_09_2019USD);
		session.save(ExRate31_10_2019USD);
		session.save(ExRate30_11_2019USD);

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
		ld1.setScenario(fact);
		ld1.setIs_created(STATUS_X.X);

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
		ed_ld1_30112019_20122019.setEnd_Date(getDate(03, 11, 2019));

		session.save(ld1);
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
