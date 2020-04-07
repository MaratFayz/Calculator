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
import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.TimeZone;

public class main
{
	/*
	* Предположения программы:
	* 1. Заполнение таблицы LD обязательно
	* 2.
	* 3. Заполнение END_DATES обязательно (для периода, в котором депозит появился, стоит первоначальная дата возврата; нельзя без даты)
	* 3.1. В END_DATES для депозитов, которые присутствуют только для факта, можно проставлять даты для плана (для расчетов с факта на бюджет)
	* 4. Дырок в периодах между транзакциями быть не может; либо транзакций нет всех, либо от периода поступления до некого периода присутствуют, затем нет.
	* 5. Таблицу PERIODS_CLOSED необходимо иметь хотя бы один закрытый период, а также значение незакрытого периода следующим.
	* */
	public static void main(String[] args)
	{
		Scanner scanner = new Scanner(System.in);
		System.out.print("Введите через пробел сценарий расчета : ");
		String answer = scanner.nextLine();
		String[] splittedAnswer = answer.split("\\s+?");

		if(splittedAnswer.length == 2)
		{

			final String SCENARIO_LOAD = answer.split("\\s+?")[0];
			final String SCENARIO_SAVE = answer.split("\\s+?")[1];


			StandardServiceRegistry registry = new StandardServiceRegistryBuilder().configure("hibernate.cfg.xml").build();
			Metadata metadata = new MetadataSources(registry).getMetadataBuilder().build();
			SessionFactory sessionFactory = metadata.getSessionFactoryBuilder().build();
			Session session = sessionFactory.openSession();
			Transaction transaction = session.beginTransaction();

/*		Course course = session.get(Course.class, 1);

		System.out.println(course.getName());*/

			transaction.commit();
			session.close();
			sessionFactory.close();
		}
		else
		{
			System.out.println("Ошибка! Необходимо ввести через пробел команду <СЦЕНАРИЙ-1> <СЦЕНАРИЙ-2>. Например, FACT FACT / FACT PLAN");
		}
	}



}
