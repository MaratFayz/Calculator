import junit.framework.TestCase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

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
	ResultSet table_LDeposits;
	ResultSet table_LDWIthEndDatesAndPeriods;
	ResultSet table_LDtransactions;
	ArrayList<LD> alLD = new ArrayList<>();
	ResultSet CommonParameter_CurrencyExchange;
	ResultSet CommonParameter_DepositRate;
	final String SCENARIO = "FACT";
	LocalDate EndDate = LocalDate.of(2020, 3, 31);
	LinkedList<HashMap<String, String>> LeasingDeposits = new LinkedList<>();
	LinkedList<HashMap<String, String>> LDWIthEndDatesAndPeriods = new LinkedList<>();
	LinkedList<HashMap<String, String>> LDtransactions = new LinkedList<>();
	LinkedList<HashMap<String, String>> CurrencyExchange = new LinkedList<>();

	public void setUp()
	{
		//Депозит только для факта 1
		HashMap<String, String> dataLD = new HashMap<>();
		dataLD.put("ID", "LDTEST0001");
		dataLD.put("ID_ENTITY", "C1001");
		dataLD.put("ID_COUNTERPARTY", "1");
		dataLD.put("ID_CURRENCY", "2");
		dataLD.put("START_DATE", "2017-03-10"); // //20.10.2019
		dataLD.put("DEPOSIT_SUM_NOT_DISC", "100000");
		dataLD.put("TO_DELETED", "null");
		dataLD.put("ID_SCENARIO", "FACT"); //5%
		LeasingDeposits.add(dataLD);

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

		//Депозит только для факта 1
		dataLD = new HashMap<>();
		dataLD.put("ID_LD", "LDTEST0001");
		dataLD.put("ID_PERIOD", "032017"); //31.03.2017
		dataLD.put("END_DATE", "2019-10-20");
		LDWIthEndDatesAndPeriods.add(dataLD);

		dataLD = new HashMap<>();
		dataLD.put("ID_LD", "LDTEST0001");
		dataLD.put("ID_PERIOD", "082017"); //31.08.2017
		dataLD.put("END_DATE", "2019-12-20");
		LDWIthEndDatesAndPeriods.add(dataLD);

		dataLD = new HashMap<>();
		dataLD.put("ID_LD", "LDTEST0001");
		dataLD.put("ID_PERIOD", "102017"); //31.10.2017
		dataLD.put("END_DATE", "2019-11-20");
		LDWIthEndDatesAndPeriods.add(dataLD);

		dataLD = new HashMap<>();
		dataLD.put("ID_LD", "LDTEST0001");
		dataLD.put("ID_PERIOD", "112019"); //30.11.2019
		dataLD.put("END_DATE", "2019-11-03");
		LDWIthEndDatesAndPeriods.add(dataLD);

		dataLD = new HashMap<>();
		dataLD.put("ID_LD", "LDTEST0003");
		dataLD.put("ID_PERIOD", "032017");
		dataLD.put("END_DATE", "2017-10-20");
		LDWIthEndDatesAndPeriods.add(dataLD);

		table_LDWIthEndDatesAndPeriods = new ResultSet_Sim.ResultSetT(LDWIthEndDatesAndPeriods);
		table_LDtransactions = new ResultSet_Sim.ResultSetT(LDtransactions);

		dataLD = new HashMap<>();
		dataLD.put("ID_CURRENCY", "USD");
		dataLD.put("DATE", "2017-03-10");
		dataLD.put("ID_SCENARIO", "FACT");
		dataLD.put("RATE", "XXX");
		CurrencyExchange.add(dataLD);

		CommonParameter_CurrencyExchange = new ResultSet_Sim.ResultSetT(CurrencyExchange);

		LDArrayBuilder ldb = new LDArrayBuilder(table_LDeposits, table_LDWIthEndDatesAndPeriods, table_LDtransactions);
		alLD = ldb.getLDArray(CommonParameter_CurrencyExchange, CommonParameter_DepositRate, EndDate);
		alLD.stream().forEach(LD::calculate);
	}

	public void test1_NominalValue()
	{
		//Если более года:
		assertEquals(100000.0, alLD.get(0).getNominalValueAtStartDay());

		//Если удаленный:
		assertEquals(100000.0, alLD.get(1).getNominalValueAtStartDay());

		//Если менее года:
		assertEquals(100000.0, alLD.get(2).getNominalValueAtStartDay());
	}

	public void test2_CalculatingDiscountedValue()
	{
		//Если более года:
		assertEquals(88027.3358353828, alLD.get(0).getDiscountedValueAtStartDay());

		//Если удаленный:
		assertEquals(100000.0, alLD.get(1).getDiscountedValueAtStartDay());

		//Если менее года:
		assertEquals(100000.0, alLD.get(2).getDiscountedValueAtStartDay());
	}


}
