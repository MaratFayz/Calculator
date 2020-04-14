import lombok.Getter;
import lombok.Setter;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.time.*;
import java.time.chrono.ChronoZonedDateTime;
import java.util.*;

@Getter
@Setter
public class GeneralDataKeeper
{
	private ZonedDateTime firstOpenPeriod;
	private List<PERIOD> AllPeriods;
	private List<EXCHANGE_RATE> AllExRates;
	private List<IFRS_ACCOUNT> AllIFRSAccounts;
	private LinkedList<LD> LDs;
	private static GeneralDataKeeper Instance;
	private SCENARIO from;
	private SCENARIO to;

	public static GeneralDataKeeper getInstance(SessionFactory sessionFactory, String scenarioFrom, String scenarioTo)
	{
		if(Instance == null)
			Instance = new GeneralDataKeeper(sessionFactory, scenarioFrom, scenarioTo);

		return Instance;
	}

	private GeneralDataKeeper(SessionFactory sessionFactory, String scenarioFrom, String scenarioTo)
	{
		Class<?> callerClass = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();

		if (GeneralDataKeeper.class != callerClass) {
			throw new IllegalAccessError("It's not legal to use constructor in singleton");
		}

		Session session = sessionFactory.openSession();
		CriteriaBuilder cb = session.getCriteriaBuilder();

		this.from = getScenarioWithName(cb, session, scenarioFrom);
		this.to = getScenarioWithName(cb, session, scenarioTo);

		LDs = getLDsForScenario(cb, session, this.from);
		if(!this.from.equals(this.to)) LDs.addAll(getLDsForScenario(cb, session, this.to));

		this.firstOpenPeriod = getFirstOpenPeriod(cb, session, this.to);
		this.AllPeriods = Collections.unmodifiableList(getAllPeriods(cb, session));
		this.AllExRates = Collections.unmodifiableList(getAllExRates(cb, session, this.to, this.from));
		this.AllIFRSAccounts = Collections.unmodifiableList(getAllIFRSAccounts(cb, session));

		session.close();
	}

	private SCENARIO getScenarioWithName(CriteriaBuilder cb, Session session, String scenarioToFind)
	{
		SCENARIO scenario;

		CriteriaQuery<SCENARIO> cqScenarios = cb.createQuery(SCENARIO.class);
		Root<SCENARIO> rootScenarios = cqScenarios.from(SCENARIO.class);
		cqScenarios.select(rootScenarios).where(cb.equal(rootScenarios.get("name"), scenarioToFind));
		Query<SCENARIO> resQScenarios = session.createQuery(cqScenarios);
		List<SCENARIO> resultScenarios = new ArrayList<SCENARIO>(resQScenarios.getResultList());

		if(resultScenarios.size() != 1)
			new IllegalArgumentException("There are no ONE SCENARIO for name = " + scenarioToFind);

		scenario = resultScenarios.get(0);

		return scenario;
	}

	private ZonedDateTime getFirstOpenPeriod(CriteriaBuilder cb, Session session, SCENARIO scenarioWhereFindFirstOpenPeriod)
	{
		CriteriaQuery<Object[]> cqOpenPer = cb.createQuery(Object[].class);
		Root<PERIOD> rootPer = cqOpenPer.from(PERIOD.class);
		Root<PERIODS_CLOSED> rootClosedPeriods = cqOpenPer.from(PERIODS_CLOSED.class);
		Root<SCENARIO> rootScenarios = cqOpenPer.from(SCENARIO.class);
		cqOpenPer.multiselect(rootClosedPeriods, rootPer, rootScenarios)
				.where(cb.equal(rootClosedPeriods.get("period"), rootPer.get("id")),
						cb.equal(rootClosedPeriods.get("scenario"), rootScenarios.get("id")));
		List<Object[]> res = session.createQuery(cqOpenPer).getResultList();

		if(res.size() == 0)
			new IllegalArgumentException("There are no pairs in scenario + period + closedPeriod");

		ZonedDateTime standard = ZonedDateTime.parse("2999-12-31T00:00:00+00:00[UTC]");
		ZonedDateTime firstOpenPeriod = standard;

		for(Object[] objects : res)
		{
			PERIODS_CLOSED pc = (PERIODS_CLOSED) objects[0];
			PERIOD p = (PERIOD) objects[1];
			SCENARIO sc = (SCENARIO) objects[2];

			if(pc.getISCLOSED() == null && sc.equals(scenarioWhereFindFirstOpenPeriod))
				if(p.getDate().isBefore(firstOpenPeriod)) firstOpenPeriod = ZonedDateTime.of(p.getDate().toLocalDate(), LocalTime.MIDNIGHT, ZoneId.of("UTC"));
		}

		if(standard.isEqual(firstOpenPeriod))
			new IllegalArgumentException("There are no open periods for scenario = " + scenarioWhereFindFirstOpenPeriod.getName());

		return firstOpenPeriod;
	}

	private LinkedList<LD> getLDsForScenario(CriteriaBuilder cb, Session session, SCENARIO scenarioWhereFindLDs)
	{
		LinkedList<LD> allLD = new LinkedList<>();

		CriteriaQuery<LD> cqLD = cb.createQuery(LD.class);
		Root<LD> rootLD = cqLD.from(LD.class);
		cqLD.select(rootLD).where(cb.and(cb.equal(rootLD.get("is_created"), STATUS_X.X), cb.equal(rootLD.get("scenario"), scenarioWhereFindLDs)));
		Query<LD> resQLD = session.createQuery(cqLD);
		allLD.addAll(resQLD.getResultList());

		return allLD;
	}

	private List<PERIOD> getAllPeriods(CriteriaBuilder cb, Session session)
	{
		List<PERIOD> allPeriods = new ArrayList<>();

		CriteriaQuery<PERIOD> cqPeriods = cb.createQuery(PERIOD.class);
		Root<PERIOD> rootPeriods = cqPeriods.from(PERIOD.class);
		cqPeriods.select(rootPeriods);
		Query<PERIOD> resQPeriods = session.createQuery(cqPeriods);
		allPeriods = new ArrayList<PERIOD>(resQPeriods.getResultList());

		//check periods
		TreeSet<ZonedDateTime> datesInPeriods = allPeriods.stream().map(period -> period.getDate()).collect(TreeSet::new,
				(ts, date) -> ts.add(date), (ts1, ts2) -> ts1.addAll(ts2));

		TreeSet<ZonedDateTime> reqPeriods = allEndDatesFromEarliestLDStartDateTillFirstOpenPeriod();

		reqPeriods.add(this.firstOpenPeriod);

		reqPeriods.removeAll(datesInPeriods);

		if(reqPeriods.size() > 0)
			new IllegalArgumentException("There is no ALL dates in Periods");

		return allPeriods;
	}

	private TreeSet<ZonedDateTime> allEndDatesFromEarliestLDStartDateTillFirstOpenPeriod()
	{
		Optional<ZonedDateTime> TheEarliestDateInLDs = this.LDs.stream().map(ld -> ld.getStart_date()).min(ChronoZonedDateTime::compareTo);
		TreeSet<ZonedDateTime> allEndDatesFromEarliestLDStartDateTillFirstOpenPeriod = new TreeSet<>();

		if(!TheEarliestDateInLDs.isPresent())
			new IllegalArgumentException("There is no ONE date for leasing_deposits");

		ZonedDateTime theMinDateInLDs_withlastDayOfMonth = TheEarliestDateInLDs.get().withDayOfMonth(TheEarliestDateInLDs.get().toLocalDate().lengthOfMonth());

		theMinDateInLDs_withlastDayOfMonth.toLocalDate().datesUntil(this.firstOpenPeriod.toLocalDate(), Period.ofMonths(1))
				.map(localDate -> ZonedDateTime.of(localDate, LocalTime.MIN, ZoneId.of("UTC")))
				.forEach(date -> allEndDatesFromEarliestLDStartDateTillFirstOpenPeriod.add(date));

		return allEndDatesFromEarliestLDStartDateTillFirstOpenPeriod;
	}

	private List<EXCHANGE_RATE> getAllExRates(CriteriaBuilder cb, Session session, SCENARIO from, SCENARIO to)
	{
		List<EXCHANGE_RATE> AllExRates = new ArrayList<>();

		CriteriaQuery<EXCHANGE_RATE> cqExRates = cb.createQuery(EXCHANGE_RATE.class);
		Root<EXCHANGE_RATE> rootExRates = cqExRates.from(EXCHANGE_RATE.class);
		cqExRates.select(rootExRates);
		Query<EXCHANGE_RATE> resQExRates = session.createQuery(cqExRates);
		AllExRates = new ArrayList<EXCHANGE_RATE>(resQExRates.getResultList());

		return AllExRates;
	}

	private List<IFRS_ACCOUNT> getAllIFRSAccounts(CriteriaBuilder cb, Session session)
	{
		List<IFRS_ACCOUNT> AllIFRSAccounts = new ArrayList<>();

		CriteriaQuery<IFRS_ACCOUNT> cqIFRSAccounts = cb.createQuery(IFRS_ACCOUNT.class);
		Root<IFRS_ACCOUNT> rootIFRSAccounts = cqIFRSAccounts.from(IFRS_ACCOUNT.class);
		cqIFRSAccounts.select(rootIFRSAccounts);
		Query<IFRS_ACCOUNT> resQIFRSAccounts = session.createQuery(cqIFRSAccounts);
		AllIFRSAccounts = new ArrayList<IFRS_ACCOUNT>(resQIFRSAccounts.getResultList());

		return AllIFRSAccounts;
	}
}
