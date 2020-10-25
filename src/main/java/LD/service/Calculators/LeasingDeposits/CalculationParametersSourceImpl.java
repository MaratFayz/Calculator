package LD.service.Calculators.LeasingDeposits;

import LD.config.Security.Repository.UserRepository;
import LD.config.Security.model.User.User;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.IFRSAccount.IFRSAccount;
import LD.model.Scenario.Scenario;
import LD.repository.IFRSAccountRepository;
import LD.repository.PeriodsClosedRepository;
import LD.repository.ScenarioRepository;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Getter
@NoArgsConstructor
@Log4j2
@Component
@Scope("prototype")
@ToString
public class CalculationParametersSourceImpl implements CalculationParametersSource {

    private LocalDate copyDate;
    private Long scenarioFrom_id;
    private Long scenarioTo_id;

    private LocalDate firstOpenPeriodOfScenarioTo;
    private LocalDate firstOpenPeriodOfScenarioFrom;
    private List<IFRSAccount> allIfrsAccounts;
    private Scenario scenarioFrom;
    private Scenario scenarioTo;
    private LocalDate entriesCopyDateFromScenarioFromToScenarioTo;
    private User user;

    @Autowired
    private ScenarioRepository scenarioRepository;
    @Autowired
    private IFRSAccountRepository ifrsAccountRepository;
    @Autowired
    private PeriodsClosedRepository periodsClosedRepository;
    @Autowired
    private UserRepository userRepository;

    public CalculationParametersSourceImpl(LocalDate copyDate, Long scenarioFrom_id, Long scenarioTo_id) {
        this.copyDate = copyDate;
        this.scenarioFrom_id = scenarioFrom_id;
        this.scenarioTo_id = scenarioTo_id;
    }

    @PostConstruct
    private void prepareParameters() {
        log.info("PostConstruct");

        getCalculatingUser();

        checkIfCopyDateNonNullOrThrowException(copyDate);
        this.entriesCopyDateFromScenarioFromToScenarioTo = copyDate;

        getScenarioFromById(scenarioFrom_id);
        getScenarioToById(scenarioTo_id);

        getFirstOpenDateForScenarioFrom();
        getFirstOpenDateForScenarioTo();

        checkIfCombinationOfScenariosAppropriateOrThrowException();
        throwExceptionWhenFirstOpenPeriodOfScenarioFromIsAfterThanFirstOpenPeriodScenarioTo();
        throwExceptionIfCopyDateIsEqualOrGreaterThanFirstOpenPeriodOfScenarioFrom();

        getIfrsAccountsFromDatabase();
    }

    private void getCalculatingUser() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        String username = authentication.getName();
        this.user = userRepository.findByUsername(username);
    }

    private void checkIfCopyDateNonNullOrThrowException(LocalDate copyDate) {
        requireNonNull(copyDate, () -> {
            throw new IllegalArgumentException("Wrong! copyDate equals null!");
        });
    }

    private void getScenarioFromById(Long scenarioFrom_id) {
        this.scenarioFrom = scenarioRepository.findById(scenarioFrom_id).orElseThrow();
        log.info("Результат запроса сценария-источника по значению {} => {}", scenarioFrom_id,
                this.scenarioFrom);
    }

    private void getScenarioToById(Long scenarioTo_id) {
        this.scenarioTo = scenarioRepository.findById(scenarioTo_id).orElseThrow();
        log.info("Результат запроса сценария-получателя по значению {} => {}", scenarioTo_id,
                this.scenarioTo);
    }

    private void getFirstOpenDateForScenarioFrom() {
        this.firstOpenPeriodOfScenarioFrom = periodsClosedRepository.findFirstOpenPeriodDateByScenario(this.scenarioFrom);

        log.info("Результат запроса первого открытого периода для сценария {} => {}",
                this.scenarioFrom.getName(), this.firstOpenPeriodOfScenarioFrom);
    }

    private void getFirstOpenDateForScenarioTo() {
        this.firstOpenPeriodOfScenarioTo = periodsClosedRepository.findFirstOpenPeriodDateByScenario(this.scenarioTo);

        log.info("Результат запроса первого открытого периода для сценария {} => {}",
                this.scenarioTo.getName(), this.firstOpenPeriodOfScenarioTo);
    }

    private void checkIfCombinationOfScenariosAppropriateOrThrowException() {
        if (isScenariosEqual()) {
            if (isScenarioFromAddition() & isScenarioToAddition()) {
                log.info("Сценарий-источник {} равен сценарию-получателю {} со статусом {}", this.scenarioTo.getName(),
                        this.scenarioFrom.getName(), this.scenarioFrom.getStatus());
            } else if (isScenarioFromFull() & isScenarioToFull()) {
                throw new IllegalArgumentException(
                        "Запрещённая операция расчёта между одним сценарием со статусом: FULL -> FULL");
            }
        } else if (isScenariosDiffer()) {
            log.info("Сценарий-источник {} не равен сценарию-получателю {}", this.scenarioFrom.getName(),
                    this.scenarioTo.getName());

            if (isScenarioFromAddition() && isScenarioToAddition()) {
                throw new IllegalArgumentException(
                        "Запрещённая операция расчёта между разными сценариями со статусами: ADDITION -> ADDITION");
            } else if (isScenarioFromAddition() && isScenarioToFull()) {
                log.info("Разрешённая операция расчёта сценария ADDITION -> FULL");
            } else if (isScenarioFromFull() && isScenarioToAddition()) {
                throw new IllegalArgumentException(
                        "Запрещённая операция расчёта между разными сценариями со статусами: FULL -> ADDITION");
            } else if (isScenarioFromFull() && isScenarioToFull()) {
                throw new IllegalArgumentException(
                        "Запрещённая операция расчёта между разными сценариями со статусами: FULL -> FULL");
            }
        } else {
            throw new IllegalStateException("Сценарии и не равны и не различаются!");
        }
    }

    private boolean isScenariosEqual() {
        return this.scenarioFrom.equals(this.scenarioTo);
    }

    private boolean isScenarioFromAddition() {
        return this.scenarioFrom.getStatus().equals(ScenarioStornoStatus.ADDITION);
    }

    private boolean isScenarioToAddition() {
        return this.scenarioTo.getStatus().equals(ScenarioStornoStatus.ADDITION);
    }

    private boolean isScenarioToFull() {
        return this.scenarioTo.getStatus().equals(ScenarioStornoStatus.FULL);
    }

    private boolean isScenarioFromFull() {
        return this.scenarioFrom.getStatus().equals(ScenarioStornoStatus.FULL);
    }

    private boolean isScenariosDiffer() {
        return !isScenariosEqual();
    }

    private void throwExceptionWhenFirstOpenPeriodOfScenarioFromIsAfterThanFirstOpenPeriodScenarioTo() {
        if (this.firstOpenPeriodOfScenarioFrom.isAfter(this.firstOpenPeriodOfScenarioTo)) {
            throw new IllegalArgumentException(
                    "Дата первого открытого периода сценария-источника всегда должна быть меньше ИЛИ " +
                            "равно первому открытому периоду сценария-получателя");
        }
    }

    private void throwExceptionIfCopyDateIsEqualOrGreaterThanFirstOpenPeriodOfScenarioFrom() {
        if (!this.entriesCopyDateFromScenarioFromToScenarioTo.isBefore(this.firstOpenPeriodOfScenarioFrom)) {
            throw new IllegalArgumentException(
                    "Дата начала копирования со сценария-источника всегда " +
                            "должна быть меньше любого первого открытого периода " +
                            "каждого из двух сценариев, либо равна LocalDate.MIN");
        }
    }

    private void getIfrsAccountsFromDatabase() {
        this.allIfrsAccounts = Collections.unmodifiableList(ifrsAccountRepository.findAll());
        log.info("Результат запроса (штук) всех счетов МСФО => {}", this.allIfrsAccounts.size());
    }
}