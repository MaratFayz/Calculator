package LD.service.Calculators.LeasingDeposits;

import LD.config.Security.Repository.UserRepository;
import LD.config.Security.model.User.User;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.IFRSAccount.IFRSAccount;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.Scenario.Scenario;
import LD.repository.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Log4j2
@Component
@ToString
public class CalculationParametersSourceImpl implements CalculationParametersSource {

    private LocalDate firstOpenPeriod_ScenarioTo;
    private LocalDate firstOpenPeriod_ScenarioFrom;
    private List<IFRSAccount> AllIFRSAccounts;
    private List<LeasingDeposit> LeasingDeposits;
    private Scenario scenarioFrom;
    private Scenario scenarioTo;
    private LocalDate period_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo;
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

    public void prepareParameters(LocalDate copyDate, Long scenarioFrom_id, Long scenarioTo_id) {
        getCalculatingUser();

        checkIfCopyDateNonNullOrThrowException(copyDate);

        this.period_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo = copyDate;

        getScenarioFromById(scenarioFrom_id);
        getScenarioToById(scenarioTo_id);

        this.firstOpenPeriod_ScenarioTo =
                periodsClosedRepository.findAll(specFirstClosedPeriod(this.scenarioTo))
                        .get(0)
                        .getPeriodsClosedID()
                        .getPeriod()
                        .getDate();

        log.info("Результат запроса первого открытого периода для сценария {} => {}",
                this.scenarioTo.getName(), this.firstOpenPeriod_ScenarioTo);

        if (!(this.scenarioFrom.equals(this.scenarioTo) && this.scenarioFrom.getStatus().equals(ScenarioStornoStatus.ADDITION))) {
            log.info("Сценарий-источник {} не равен сценарию-получателю {}", this.scenarioTo.getName(),
                    this.scenarioFrom.getName());

            if (this.scenarioFrom.getStatus().equals(ScenarioStornoStatus.ADDITION) &&
                    (this.scenarioTo.getStatus().equals(ScenarioStornoStatus.ADDITION))) {
                log.info("Запрещённая операция переноса ADDITION -> ADDITION");
                throw new IllegalArgumentException(
                        "Запрещённая операция переноса ADDITION -> ADDITION");
            }

            if (this.scenarioFrom.getStatus()
                    .equals(ScenarioStornoStatus.FULL) && (this.scenarioTo.getStatus()
                    .equals(ScenarioStornoStatus.ADDITION))) {
                log.info("Запрещённая операция переноса FULL -> ADDITION");

                throw new IllegalArgumentException(
                        "Запрещённая операция переноса FULL -> ADDITION");
            }

            if (this.scenarioFrom.getStatus().equals(ScenarioStornoStatus.FULL) &&
                    this.scenarioTo.getStatus().equals(ScenarioStornoStatus.FULL) &&
                    !this.scenarioFrom.equals(this.scenarioTo)) {
                log.info("Запрещённая операция переноса FULL -> FULL");

                throw new IllegalArgumentException("Запрещённая операция переноса FULL -> FULL");
            }

            this.firstOpenPeriod_ScenarioFrom =
                    periodsClosedRepository.findAll(specFirstClosedPeriod(this.scenarioFrom))
                            .get(0)
                            .getPeriodsClosedID()
                            .getPeriod()
                            .getDate();

            log.info("Результат запроса первого открытого периода для сценария-источника {} => {}",
                    this.scenarioFrom.getName(), this.firstOpenPeriod_ScenarioFrom);

            if (!(this.firstOpenPeriod_ScenarioFrom.isBefore(this.firstOpenPeriod_ScenarioTo) ||
                    this.firstOpenPeriod_ScenarioFrom.isEqual(this.firstOpenPeriod_ScenarioTo))) {
                throw new IllegalArgumentException(
                        "Дата первого открытого периода сценария-источника всегда должна быть меньше ИЛИ равно первому открытому периоду сценария-получателя");
            }

            if (this.period_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo != null) {
                if (!this.period_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo.isBefore(this.firstOpenPeriod_ScenarioFrom)) {
                    log.info("Дата начала копирования со сценария-источника всегда должна быть меньше любого первого " +
                            "открытого периода каждого из двух сценариев, либо равна null");

                    throw new IllegalArgumentException(
                            "Дата начала копирования со сценария-источника всегда должна быть меньше любого первого открытого периода каждого из двух сценариев, либо равна null");
                }
            }
        }

        this.AllIFRSAccounts = Collections.unmodifiableList(ifrsAccountRepository.findAll());
        log.info("Результат запроса (штук) всех счетов МСФО => {}", this.AllIFRSAccounts.size());
    }

    private void getScenarioToById(Long scenarioTo_id) {
        this.scenarioTo = scenarioRepository.findById(scenarioTo_id).orElseThrow();
        log.info("Результат запроса сценария-получателя по значению {} => {}", scenarioTo_id,
                this.scenarioTo);
    }

    private void getScenarioFromById(Long scenarioFrom_id) {
        this.scenarioFrom = scenarioRepository.findById(scenarioFrom_id).orElseThrow();
        log.info("Результат запроса сценария-источника по значению {} => {}", scenarioFrom_id,
                this.scenarioFrom);
    }

    private void checkIfCopyDateNonNullOrThrowException(LocalDate copyDate) {
        Objects.requireNonNull(copyDate, () -> {
            throw new IllegalArgumentException("Wrong! copyDate equals null!");
        });
    }

    private void getCalculatingUser() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        String username = authentication.getName();
        this.user = userRepository.findByUsername(username);
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
}