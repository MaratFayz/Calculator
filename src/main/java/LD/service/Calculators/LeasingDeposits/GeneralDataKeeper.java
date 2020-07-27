package LD.service.Calculators.LeasingDeposits;

import LD.config.Security.Repository.UserRepository;
import LD.config.Security.model.User.User;
import LD.model.Enums.STATUS_X;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.ExchangeRate.ExchangeRate;
import LD.model.IFRSAccount.IFRSAccount;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.Period.Period;
import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.Scenario.Scenario;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import LD.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.*;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@Log4j2
@Component
@ToString
public class GeneralDataKeeper {

    private ZonedDateTime firstOpenPeriod_ScenarioTo;
    private ZonedDateTime firstOpenPeriod_ScenarioFrom;
    private List<Period> AllPeriods;
    private List<ExchangeRate> AllExRates;
    private List<IFRSAccount> AllIFRSAccounts;
    private List<LeasingDeposit> LeasingDeposits;
    private Scenario from;
    private Scenario to;
    private ZonedDateTime period_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo;
    private User user;

    @Autowired
    private LeasingDepositRepository leasingDepositRepository;
    @Autowired
    private ScenarioRepository scenarioRepository;
    @Autowired
    private IFRSAccountRepository ifrsAccountRepository;
    @Autowired
    private ExchangeRateRepository exchangeRateRepository;
    @Autowired
    private PeriodRepository periodRepository;
    @Autowired
    private PeriodsClosedRepository periodsClosedRepository;
    @Autowired
    private UserRepository userRepository;

    public void getDataFromDB(Long scenarioFrom, Long scenarioTo) {
        getDataFromDB(null, scenarioFrom, scenarioTo);
    }

    public void getDataFromDB(ZonedDateTime copyDate, Long scenarioFrom_id, Long scenarioTo_id) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        this.user = userRepository.findByUsername(username);

        this.period_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo = copyDate;

        this.from = scenarioRepository.findById(scenarioFrom_id)
                .orElseThrow();
        log.info("Результат запроса сценария-источника по значению {} => {}", scenarioFrom_id,
                this.from);

        this.to = scenarioRepository.findById(scenarioTo_id)
                .orElseThrow();
        log.info("Результат запроса сценария-получателя по значению {} => {}", scenarioTo_id,
                this.to);

        LeasingDeposits = leasingDepositRepository.findAll(specLDsForScenario(this.to));
        log.info("Результат запроса лизинговых депозитов по сценарию-получателю {} => {}",
                this.to.getName(), LeasingDeposits);

        this.firstOpenPeriod_ScenarioTo =
                periodsClosedRepository.findAll(specFirstClosedPeriod(this.to))
                        .get(0)
                        .getPeriodsClosedID()
                        .getPeriod()
                        .getDate();
        log.info("Результат запроса первого открытого периода для сценария {} => {}",
                this.to.getName(), this.firstOpenPeriod_ScenarioTo);

        if (!(this.from.equals(this.to) && this.from.getStatus()
                .equals(ScenarioStornoStatus.ADDITION))) {
            log.info("Сценарий-источник {} не равен сценарию-получателю {}", this.to.getName(),
                    this.from.getName());

            if (this.from.getStatus()
                    .equals(ScenarioStornoStatus.ADDITION) && (this.to.getStatus()
                    .equals(ScenarioStornoStatus.ADDITION))) {
                log.info("Запрещённая операция переноса ADDITION -> ADDITION");

                throw new IllegalArgumentException(
                        "Запрещённая операция переноса ADDITION -> ADDITION");
            }

            if (this.from.getStatus()
                    .equals(ScenarioStornoStatus.FULL) && (this.to.getStatus()
                    .equals(ScenarioStornoStatus.ADDITION))) {
                log.info("Запрещённая операция переноса FULL -> ADDITION");

                throw new IllegalArgumentException(
                        "Запрещённая операция переноса FULL -> ADDITION");
            }

            if (this.from.getStatus()
                    .equals(ScenarioStornoStatus.FULL) && this.to.getStatus()
                    .equals(ScenarioStornoStatus.FULL) &&
                    !this.from.equals(this.to)) {
                log.info("Запрещённая операция переноса FULL -> FULL");

                throw new IllegalArgumentException("Запрещённая операция переноса FULL -> FULL");
            }

            this.firstOpenPeriod_ScenarioFrom =
                    periodsClosedRepository.findAll(specFirstClosedPeriod(this.from))
                            .get(0)
                            .getPeriodsClosedID()
                            .getPeriod()
                            .getDate();
            log.info("Результат запроса первого открытого периода для сценария-источника {} => {}",
                    this.from.getName(), this.firstOpenPeriod_ScenarioFrom);

            if (!(this.firstOpenPeriod_ScenarioFrom.isBefore(this.firstOpenPeriod_ScenarioTo) ||
                    this.firstOpenPeriod_ScenarioFrom.isEqual(this.firstOpenPeriod_ScenarioTo))) {
                throw new IllegalArgumentException(
                        "Дата первого открытого периода сценария-источника всегда должна быть меньше ИЛИ равно первому открытому периоду сценария-получателя");
            }

            if (this.period_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo != null) {
                if (!this.period_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo.isBefore(
                        this.firstOpenPeriod_ScenarioFrom)) {
                    log.info(
                            "Дата начала копирования со сценария-источника всегда должна быть меньше любого первого открытого периода каждого из двух сценариев, либо равна null");

                    throw new IllegalArgumentException(
                            "Дата начала копирования со сценария-источника всегда должна быть меньше любого первого открытого периода каждого из двух сценариев, либо равна null");
                }
            }

            LeasingDeposits.addAll(leasingDepositRepository.findAll(specLDsForScenario(this.from)));
            log.info("Результат запроса ВСЕХ лизинговых депозитов: {}", LeasingDeposits);

        }

		if (this.LeasingDeposits.size() == 0) {
			throw new IllegalStateException("Отсутствуют депозиты для расчета");
		}

        this.AllPeriods = Collections.unmodifiableList(countAllPeriods());
        log.info("Результат запроса всех периодов => {}", this.AllPeriods);

        this.AllExRates = Collections.unmodifiableList(
                exchangeRateRepository.findAll(specAllExRatesForScenario(this.to, this.from)));
        log.info("Результат запроса всех курсов по сценариям {} и {} => {}", this.from, this.to,
                this.AllExRates);

        this.AllIFRSAccounts = Collections.unmodifiableList(ifrsAccountRepository.findAll());
        log.info("Результат запроса всех счетов МСФО => {}", this.AllIFRSAccounts);
    }

    public static Specification<PeriodsClosed> specFirstClosedPeriod(
            Scenario scenarioWhereFindFirstOpenPeriod) {
        Specification<PeriodsClosed> spPerCl = (rootPC, qPC, cbPC) -> {

            qPC.orderBy(cbPC.asc(rootPC.get("periodsClosedID")
                    .get("period")
                    .get("date")));

            return cbPC.and(
                    cbPC.equal(rootPC.get("periodsClosedID")
                            .get("scenario"), scenarioWhereFindFirstOpenPeriod),
                    cbPC.isNull(rootPC.get("ISCLOSED"))
            );
        };

        return spPerCl;
    }

    public static Specification<LeasingDeposit> specLDsForScenario(Scenario scenarioWhereFindLDs) {
        Specification<LeasingDeposit> spLD = (rootLD, qLD, cbLD) ->
                cbLD.and(cbLD.equal(rootLD.get("is_created"), STATUS_X.X),
                        cbLD.equal(rootLD.get("scenario"), scenarioWhereFindLDs));

        return spLD;
    }

    private List<Period> countAllPeriods() {
        List<Period> allPeriods = new ArrayList<>();
        allPeriods = periodRepository.findAll();

        //check periods
        TreeSet<ZonedDateTime> datesInPeriods = allPeriods.stream()
                .map(period -> period.getDate())
                .collect(TreeSet::new,
                        (ts, date) -> ts.add(date), (ts1, ts2) -> ts1.addAll(ts2));

        TreeSet<ZonedDateTime> reqPeriods = allEndDatesFromEarliestLDStartDateTillFirstOpenPeriod();

        reqPeriods.add(this.firstOpenPeriod_ScenarioTo);

        reqPeriods.removeAll(datesInPeriods);

		if (reqPeriods.size() > 0) {
			new IllegalArgumentException("There is no ALL dates in Periods");
		}

        return allPeriods;
    }

    private TreeSet<ZonedDateTime> allEndDatesFromEarliestLDStartDateTillFirstOpenPeriod() {
        Optional<ZonedDateTime> TheEarliestDateInLDs = this.LeasingDeposits.stream()
                .map(ld -> ld.getStart_date())
                .min(ChronoZonedDateTime::compareTo);
        TreeSet<ZonedDateTime> allEndDatesFromEarliestLDStartDateTillFirstOpenPeriod =
                new TreeSet<>();

		if (!TheEarliestDateInLDs.isPresent()) {
			new IllegalArgumentException("There is no ONE date for leasing_deposits");
		}

        ZonedDateTime theMinDateInLDs_withlastDayOfMonth = TheEarliestDateInLDs.get()
                .withDayOfMonth(TheEarliestDateInLDs.get()
                        .toLocalDate()
                        .lengthOfMonth());

        theMinDateInLDs_withlastDayOfMonth.toLocalDate()
                .datesUntil(this.firstOpenPeriod_ScenarioTo.toLocalDate(),
                        java.time.Period.ofMonths(1))
                .map(localDate -> ZonedDateTime.of(localDate, LocalTime.MIN, ZoneId.of("UTC")))
                .forEach(date -> allEndDatesFromEarliestLDStartDateTillFirstOpenPeriod.add(date));

        return allEndDatesFromEarliestLDStartDateTillFirstOpenPeriod;
    }

    public static Specification<ExchangeRate> specAllExRatesForScenario(Scenario from,
                                                                        Scenario to) {
        return new Specification<ExchangeRate>() {
            @Override
            public Predicate toPredicate(Root<ExchangeRate> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                return criteriaBuilder.and(
                        criteriaBuilder.or(
                                criteriaBuilder.equal(root.get("exchangeRateID")
                                        .get("scenario"), from),
                                criteriaBuilder.equal(root.get("exchangeRateID")
                                        .get("scenario"), to)
                        ));
            }
        };
    }

    public static Specification<Scenario> ScenarioForName(String neededScenario) {
        return new Specification<Scenario>() {
            @Override
            public Predicate toPredicate(Root<Scenario> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                return criteriaBuilder.equal(root.get("name"), neededScenario);
            }
        };
    }


}
