package LD;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application
{
	/*
	* Предположения программы:
	* 1. Заполнение таблицы LeasingDeposit.model.LeasingDeposit обязательно
	* 2.
	* 3. Заполнение LeasingDeposit.model.EndDate обязательно (для периода, в котором депозит появился, стоит первоначальная дата возврата; нельзя без даты)
	* 3.1. В LeasingDeposit.model.EndDate для депозитов, которые присутствуют только для факта, можно проставлять даты для плана (для расчетов с факта на бюджет)
	* 4. Дырок в периодах между транзакциями быть не может; либо транзакций нет всех, либо от периода поступления до некого периода присутствуют, затем нет.
	* 5. Таблицу LeasingDeposit.model.PeriodsClosed необходимо иметь хотя бы один закрытый период, а также значение незакрытого периода следующим.
	* */
	public static void main(String[] args)
	{
		SpringApplication.run(Application.class, args);
	}

}
