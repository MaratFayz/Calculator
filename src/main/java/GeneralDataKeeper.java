import lombok.Getter;
import lombok.Setter;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
public class GeneralDataKeeper
{
	private ZonedDateTime firstOpenPeriod;
	private CopyOnWriteArrayList<PERIOD> AllPeriods;
	private CopyOnWriteArrayList<SCENARIO> AllScenarios;
	private CopyOnWriteArrayList<EXCHANGE_RATE> AllExRates;
	private CopyOnWriteArrayList<IFRS_ACCOUNT> AllIFRSAccounts;
	private ConcurrentLinkedQueue<LD> all_LD_in_scenarioFrom;
	private ConcurrentLinkedQueue<LD> all_LD_in_scenarioTo;

	GeneralDataKeeper(SessionFactory sessionFactory, String scenarioFrom, String scenarioTo)
	{
		Session session = sessionFactory.openSession();
		CriteriaBuilder cb = session.getCriteriaBuilder();

		all_LD_in_scenarioFrom = getLDsForScenario(cb, session, scenarioFrom);
		if(!scenarioFrom.equals(scenarioTo)) all_LD_in_scenarioTo = getLDsForScenario(cb, session, scenarioTo);
		else all_LD_in_scenarioTo = new ConcurrentLinkedQueue<>();

		this.firstOpenPeriod = getFirstOpenPeriod(cb, session);
		this.AllPeriods = getAllPeriods(cb, session);
		this.AllScenarios = getAllScenarios(cb, session);
		this.AllExRates = getAllExRates(cb, session);
		this.AllIFRSAccounts = getAllIFRSAccounts(cb, session);

		session.close();
	}

	public static ZonedDateTime getFirstOpenPeriod(CriteriaBuilder cb, Session session)
	{
		CriteriaQuery<Object[]> cqOpenPer = cb.createQuery(Object[].class);
		Root<PERIOD> rootPer = cqOpenPer.from(PERIOD.class);
		Root<PERIODS_CLOSED> rootClosedPeriods = cqOpenPer.from(PERIODS_CLOSED.class);
		cqOpenPer.multiselect(rootClosedPeriods, rootPer).where(cb.equal(rootClosedPeriods.get("period"), rootPer.get("id")));
		List<Object[]> res = session.createQuery(cqOpenPer).getResultList();

		ZonedDateTime firstOpenPeriod = ZonedDateTime.parse("2999-12-31T00:00:00+00:00[UTC]");
		for(Object[] objects : res)
		{
			PERIODS_CLOSED pc = (PERIODS_CLOSED) objects[0];
			PERIOD p = (PERIOD) objects[1];

			if(pc.getISCLOSED() == null)
				if(p.getDate().isBefore(firstOpenPeriod)) firstOpenPeriod = p.getDate().withHour(0).withMinute(0).withSecond(0).withZoneSameLocal(ZoneId.of("UTC"));
		}

		return firstOpenPeriod;
	}

	public static ConcurrentLinkedQueue<LD> getLDsForScenario(CriteriaBuilder cb, Session session, String SCENARIO)
	{
		ConcurrentLinkedQueue<LD> allLD = new ConcurrentLinkedQueue<>();

		CriteriaQuery<LD> cqLD = cb.createQuery(LD.class);
		Root<LD> rootLD = cqLD.from(LD.class);
		cqLD.select(rootLD).where(cb.and(cb.equal(rootLD.get("is_created"), STATUS_X.X), cb.equal(rootLD.get("scenario").get("name"), SCENARIO)));
		Query<LD> resQLD = session.createQuery(cqLD);
		allLD.addAll(resQLD.getResultList());

		return allLD;
	}

	public static CopyOnWriteArrayList<PERIOD> getAllPeriods(CriteriaBuilder cb, Session session)
	{
		CopyOnWriteArrayList<PERIOD> allPeriods = new CopyOnWriteArrayList<>();

		CriteriaQuery<PERIOD> cqPeriods = cb.createQuery(PERIOD.class);
		Root<PERIOD> rootPeriods = cqPeriods.from(PERIOD.class);
		cqPeriods.select(rootPeriods);
		Query<PERIOD> resQPeriods = session.createQuery(cqPeriods);
		allPeriods = new CopyOnWriteArrayList<PERIOD>(resQPeriods.getResultList());

		return allPeriods;
	}

	public static CopyOnWriteArrayList<SCENARIO> getAllScenarios(CriteriaBuilder cb, Session session)
	{
		CopyOnWriteArrayList<SCENARIO> AllScenarios = new CopyOnWriteArrayList<>();

		CriteriaQuery<SCENARIO> cqScenarios = cb.createQuery(SCENARIO.class);
		Root<SCENARIO> rootScenarios = cqScenarios.from(SCENARIO.class);
		cqScenarios.select(rootScenarios);
		Query<SCENARIO> resQScenarios = session.createQuery(cqScenarios);
		AllScenarios = new CopyOnWriteArrayList<SCENARIO>(resQScenarios.getResultList());

		return AllScenarios;
	}

	public static CopyOnWriteArrayList<EXCHANGE_RATE> getAllExRates(CriteriaBuilder cb, Session session)
	{
		CopyOnWriteArrayList<EXCHANGE_RATE> AllExRates = new CopyOnWriteArrayList<>();

		CriteriaQuery<EXCHANGE_RATE> cqExRates = cb.createQuery(EXCHANGE_RATE.class);
		Root<EXCHANGE_RATE> rootExRates = cqExRates.from(EXCHANGE_RATE.class);
		cqExRates.select(rootExRates);
		Query<EXCHANGE_RATE> resQExRates = session.createQuery(cqExRates);
		AllExRates = new CopyOnWriteArrayList<EXCHANGE_RATE>(resQExRates.getResultList());

		return AllExRates;
	}

	public static CopyOnWriteArrayList<IFRS_ACCOUNT> getAllIFRSAccounts(CriteriaBuilder cb, Session session)
	{
		CopyOnWriteArrayList<IFRS_ACCOUNT> AllIFRSAccounts = new CopyOnWriteArrayList<>();

		CriteriaQuery<IFRS_ACCOUNT> cqIFRSAccounts = cb.createQuery(IFRS_ACCOUNT.class);
		Root<IFRS_ACCOUNT> rootIFRSAccounts = cqIFRSAccounts.from(IFRS_ACCOUNT.class);
		cqIFRSAccounts.select(rootIFRSAccounts);
		Query<IFRS_ACCOUNT> resQIFRSAccounts = session.createQuery(cqIFRSAccounts);
		AllIFRSAccounts = new CopyOnWriteArrayList<IFRS_ACCOUNT>(resQIFRSAccounts.getResultList());

		return AllIFRSAccounts;
	}
}
