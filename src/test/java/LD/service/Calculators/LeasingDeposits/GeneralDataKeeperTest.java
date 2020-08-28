package LD.service.Calculators.LeasingDeposits;

import LD.config.Security.Repository.UserRepository;
import LD.config.Security.model.User.User;
import LD.model.Company.Company;
import LD.model.Counterpartner.Counterpartner;
import LD.model.Currency.Currency;
import LD.model.DepositRate.DepositRate;
import LD.model.Duration.Duration;
import LD.model.EndDate.EndDate;
import LD.model.EndDate.EndDateID;
import LD.model.Entry.Entry;
import LD.model.Enums.STATUS_X;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.ExchangeRate.ExchangeRate;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.Period.Period;
import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.PeriodsClosed.PeriodsClosedID;
import LD.model.Scenario.Scenario;
import LD.repository.*;
import LD.service.Calculators.LeasingDeposits.EntryCalculator;
import LD.service.Calculators.LeasingDeposits.GeneralDataKeeper;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static TestsForLeasingDeposit.Calculator.Builders.*;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

//перечень тестов:
//1. Проверить, будет ли работать программа с непроставленными данными по дате возврата для определенного периода
//2. Проверить, что депозит, который помечен как DELETED -> не имеет проводок со статусом <> DELETED
//3. Проверить, что депозит, который помечен как DELETED -> не появляется в расчете
//4. Проверить, что дисконтированная сумма считается от даты выдачи до даты закрытия на первоначальную дату
//5. Проверить, что депозит без даты возврата не считается.
//6. Валютные курсы корректно брались в зависимости от даты и валюты
//7. Ставки депозитные чтоб корректно брались в зависимсоти от срока и валюты, company
//8. Ввод данных обязателен для двух таблиц по одному депозиту (оссновные данные и дата конца - нужна хотя бы одна запись, иначе нет расчета).
//9. Проверить, что берется последняя проводка по последнему закрытому периоду перед первой дыркой.

@Disabled("Fix tests with mocking SecurityContext or refactoring GDK class")
@ExtendWith(MockitoExtension.class)
public class GeneralDataKeeperTest {

    static Scenario scenarioSourceAddition;
    static Scenario scenarioDestinationFull;
    static Scenario scenarioDestinationAddition;
    static Scenario scenarioSourceFull;
    static Currency usd;
    static Company C1001;
    static Counterpartner CP;
    static HashMap<ZonedDateTime, Period> periods = new HashMap<>();
    static Duration dur_0_12_M;
    static Duration dur_25_36_M;
    static DepositRate depRateC1001_0_12M;
    static DepositRate depRateC1001_25_36M;
    static List<ExchangeRate> ExR;

    List<LeasingDeposit> LeasingDeposits = new LinkedList<>();
    static LeasingDeposit leasingDeposit1;
    EntryCalculator calculatorBeforeTestForScenarioSource;
    EntryCalculator calculatorTestForScenarioSourceDestination;

    static GeneralDataKeeper GDK = new GeneralDataKeeper();
    @Mock
    DepositRatesRepository depositRatesRepository;
    @Mock
    ScenarioRepository scenarioRepository;
    @Mock
    private LeasingDepositRepository leasingDepositRepository;
    @Mock
    private IFRSAccountRepository ifrsAccountRepository;
    @Mock
    private ExchangeRateRepository exchangeRateRepository;
    @Mock
    private PeriodRepository periodRepository;
    @Mock
    private PeriodsClosedRepository periodsClosedRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    SecurityContextHolder securityContextHolder;
    @Mock
    SecurityContext securityContext;
    @Mock
    Authentication authentication;

    final String SCENARIO_SOURCE = "SCENARIO_SOURCE";
    final String SCENARIO_DESTINATION = "SCENARIO_DESTINATION";
    List<Entry> calculatedEntries = new ArrayList<>();
    ExecutorService threadExecutor;
    static User user = getAnyUser();

    @BeforeEach
    public void setUp() throws ExecutionException, InterruptedException {
        InitializeGeneraldata();
        create_LD_1_NormalTestLD();
    }

    @Test
    public void test1_1_IllegalOperationsBetweenScenarios_ADD_to_ADD_notEqual() {
        //ADD -> ADD для разных сценариев
        long scenarioSource = 1L;
        long scenarioDestination = 2L;

        Scenario scenarioSourceAddition = getSC("SCENARIO_S_A", ScenarioStornoStatus.ADDITION, user);
        Scenario scenarioDestinationAddition = getSC("SCENARIO_D_A", ScenarioStornoStatus.ADDITION, user);

        Mockito.lenient().when(scenarioRepository.findById(Mockito.eq(scenarioSource))).thenReturn(Optional.ofNullable(scenarioSourceAddition));
        Mockito.lenient().when(scenarioRepository.findById(Mockito.eq(scenarioDestination))).thenReturn(Optional.ofNullable(scenarioDestinationAddition));

        PeriodsClosedID pcIdSA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioSourceAddition)
                .build();

        PeriodsClosed pcSA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdSA)
                .build();

        PeriodsClosedID pcIdDA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioDestinationAddition)
                .build();

        PeriodsClosed pcDA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdDA)
                .build();

        Mockito.when(periodsClosedRepository.findAll(Mockito.any(Specification.class))).thenReturn(List.of(pcDA), List.of(pcSA));
        Mockito.when(securityContextHolder.getContext()).thenReturn(securityContext);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        Mockito.when(authentication.getName()).thenReturn(user.getUsername());
        Mockito.when(userRepository.findByUsername(eq(user.getUsername()))).thenReturn(user);

        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            GDK.getDataFromDB(scenarioSource, scenarioDestination);
        });

        assertEquals("Запрещённая операция переноса ADDITION -> ADDITION", thrown.getMessage());
    }

    @Test
    public void test1_2_LegalOperationsBetweenScenarios_ADD_to_ADD_Equal() {
        //ADD -> ADD для одного сценария
        long scenarioSource = 1L;
        long scenarioDestination = 1L;

        Scenario scenarioSourceAddition = getSC("SCENARIO_A", ScenarioStornoStatus.ADDITION, user);
        Scenario scenarioDestinationAddition = getSC("SCENARIO_A", ScenarioStornoStatus.ADDITION, user);

        Mockito.when(scenarioRepository.findById(scenarioSource)).thenReturn(Optional.ofNullable(scenarioSourceAddition));
        Mockito.when(scenarioRepository.findById(scenarioDestination)).thenReturn(Optional.ofNullable(scenarioDestinationAddition));

        PeriodsClosedID pcIdSA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioSourceAddition)
                .build();

        PeriodsClosed pcSA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdSA)
                .build();

        PeriodsClosedID pcIdDA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioDestinationAddition)
                .build();

        PeriodsClosed pcDA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdDA)
                .build();

        Mockito.when(periodsClosedRepository.findAll(Mockito.any(Specification.class))).thenReturn(List.of(pcDA), List.of(pcSA));

        ArrayList<LeasingDeposit> cv = new ArrayList();
        cv.add(leasingDeposit1);

        Mockito.when(leasingDepositRepository.findAll(Mockito.any(Specification.class))).thenReturn(cv);

        try {
            GDK.getDataFromDB(scenarioSource, scenarioDestination);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void test2_LegalOperationsBetweenScenarios_ADD_to_FULL() {
        long scenarioSource = 1L;
        long scenarioDestination = 2L;

        Scenario scenarioSourceAddition = getSC("SCENARIO_S_A", ScenarioStornoStatus.ADDITION, user);
        Scenario scenarioDestinationFull = getSC("SCENARIO_D_F", ScenarioStornoStatus.FULL, user);

        Mockito.when(scenarioRepository.findById(scenarioSource)).thenReturn(Optional.ofNullable(scenarioSourceAddition));
        Mockito.when(scenarioRepository.findById(scenarioDestination)).thenReturn(Optional.ofNullable(scenarioDestinationFull));

        PeriodsClosedID pcIdSA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioSourceAddition)
                .build();

        PeriodsClosed pcSA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdSA)
                .build();

        PeriodsClosedID pcIdDA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioDestinationFull)
                .build();

        PeriodsClosed pcDA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdDA)
                .build();

        Mockito.when(periodsClosedRepository.findAll(Mockito.any(Specification.class))).thenReturn(List.of(pcDA), List.of(pcSA));

        ArrayList<LeasingDeposit> cv = new ArrayList();
        cv.add(leasingDeposit1);

        Mockito.when(leasingDepositRepository.findAll(Mockito.any(Specification.class))).thenReturn(new ArrayList(), cv);

        try {
            GDK.getDataFromDB(scenarioSource, scenarioDestination);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void test3_IllegalOperationsBetweenScenarios_FULL_to_ADD() {
        long scenarioSource = 1L;
        long scenarioDestination = 2L;

        Scenario scenarioSourceFull = getSC("SCENARIO_S_F", ScenarioStornoStatus.FULL, user);
        Scenario scenarioDestinationAddition = getSC("SCENARIO_D_A", ScenarioStornoStatus.ADDITION, user);

        Mockito.when(scenarioRepository.findById(scenarioSource)).thenReturn(Optional.ofNullable(scenarioSourceFull));
        Mockito.when(scenarioRepository.findById(scenarioDestination)).thenReturn(Optional.ofNullable(scenarioDestinationAddition));

        PeriodsClosedID pcIdSA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioSourceFull)
                .build();

        PeriodsClosed pcSA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdSA)
                .build();

        PeriodsClosedID pcIdDA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioDestinationAddition)
                .build();

        PeriodsClosed pcDA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdDA)
                .build();

        Mockito.when(periodsClosedRepository.findAll(Mockito.any(Specification.class))).thenReturn(List.of(pcDA), List.of(pcSA));

        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            GDK.getDataFromDB(scenarioSource, scenarioDestination);
        });

        assertEquals("Запрещённая операция переноса FULL -> ADDITION", thrown.getMessage());
    }

    @Test
    public void test4_1_IllegalOperationsBetweenScenarios_FULL_to_FULL_notEquals() {
        //разные сценарии -> ошибка
        long scenarioSource = 1L;
        long scenarioDestination = 2L;

        Scenario scenarioSourceFull = getSC("SCENARIO_S_F", ScenarioStornoStatus.FULL, user);
        Scenario scenarioDestinationFull = getSC("SCENARIO_D_F", ScenarioStornoStatus.FULL, user);

        Mockito.when(scenarioRepository.findById(scenarioSource)).thenReturn(Optional.ofNullable(scenarioSourceFull));
        Mockito.when(scenarioRepository.findById(scenarioDestination)).thenReturn(Optional.ofNullable(scenarioDestinationFull));

        PeriodsClosedID pcIdSA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioSourceFull)
                .build();

        PeriodsClosed pcSA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdSA)
                .build();

        PeriodsClosedID pcIdDA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioDestinationFull)
                .build();

        PeriodsClosed pcDA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdDA)
                .build();

        Mockito.when(periodsClosedRepository.findAll(Mockito.any(Specification.class))).thenReturn(List.of(pcDA), List.of(pcSA));

        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            GDK.getDataFromDB(scenarioSource, scenarioDestination);
        });

        assertEquals("Запрещённая операция переноса FULL -> FULL", thrown.getMessage());
    }

    @Test
    public void test4_2_LegalOperationsBetweenScenarios_FULL_to_FULL_Equals() {
        //разные сценарии -> ошибка
        long scenarioSource = 1L;
        long scenarioDestination = 1L;

        Scenario scenarioSourceFull = getSC("SCENARIO_F", ScenarioStornoStatus.FULL, user);
        Scenario scenarioDestinationFull = getSC("SCENARIO_F", ScenarioStornoStatus.FULL, user);

        Mockito.when(scenarioRepository.findById(scenarioSource)).thenReturn(Optional.ofNullable(scenarioSourceFull));
        Mockito.when(scenarioRepository.findById(scenarioDestination)).thenReturn(Optional.ofNullable(scenarioDestinationFull));

        PeriodsClosedID pcIdSA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioSourceFull)
                .build();

        PeriodsClosed pcSA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdSA)
                .build();

        PeriodsClosedID pcIdDA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioDestinationFull)
                .build();

        PeriodsClosed pcDA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdDA)
                .build();

        Mockito.when(periodsClosedRepository.findAll(Mockito.any(Specification.class))).thenReturn(List.of(pcDA), List.of(pcSA));

        ArrayList<LeasingDeposit> cv = new ArrayList();
        cv.add(leasingDeposit1);

        Mockito.when(leasingDepositRepository.findAll(Mockito.any(Specification.class))).thenReturn(new ArrayList(), cv);

        try {
            GDK.getDataFromDB(scenarioSource, scenarioDestination);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void test5_copyDate_EQUALS_FirstOpenPeriodScenarioSource_AND_FirstOpenPeriodScenarioDestination() {
        ZonedDateTime copyDate = getDate(30, 11, 2019);

        long scenarioSource = 1L;
        long scenarioDestination = 2L;

        Scenario scenarioSourceAddition = getSC("SCENARIO_S_A", ScenarioStornoStatus.ADDITION, user);
        Scenario scenarioDestinationFull = getSC("SCENARIO_D_F", ScenarioStornoStatus.FULL, user);

        Mockito.when(scenarioRepository.findById(scenarioSource)).thenReturn(Optional.ofNullable(scenarioSourceAddition));
        Mockito.when(scenarioRepository.findById(scenarioDestination)).thenReturn(Optional.ofNullable(scenarioDestinationFull));

        PeriodsClosedID pcIdSA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioSourceAddition)
                .build();

        PeriodsClosed pcSA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdSA)
                .build();

        PeriodsClosedID pcIdDA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioDestinationFull)
                .build();

        PeriodsClosed pcDA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdDA)
                .build();

        Mockito.when(periodsClosedRepository.findAll(Mockito.any(Specification.class))).thenReturn(List.of(pcDA), List.of(pcSA));

        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            GDK.getDataFromDB(copyDate, scenarioSource, scenarioDestination);
        });

        assertEquals("Дата начала копирования со сценария-источника всегда должна быть меньше любого первого открытого периода каждого из двух сценариев, либо равна null", thrown.getMessage());
    }

    @Test
    public void test6_copyDate_GREATER_FirstOpenPeriodScenarioSource_AND_FirstOpenPeriodScenarioDestination() {
        ZonedDateTime copyDate = getDate(31, 12, 2019);

        long scenarioSource = 1L;
        long scenarioDestination = 2L;

        Scenario scenarioSourceAddition = getSC("SCENARIO_S_A", ScenarioStornoStatus.ADDITION, user);
        Scenario scenarioDestinationFull = getSC("SCENARIO_D_F", ScenarioStornoStatus.FULL, user);

        Mockito.when(scenarioRepository.findById(scenarioSource)).thenReturn(Optional.ofNullable(scenarioSourceAddition));
        Mockito.when(scenarioRepository.findById(scenarioDestination)).thenReturn(Optional.ofNullable(scenarioDestinationFull));

        PeriodsClosedID pcIdSA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioSourceAddition)
                .build();

        PeriodsClosed pcSA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdSA)
                .build();

        PeriodsClosedID pcIdDA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioDestinationFull)
                .build();

        PeriodsClosed pcDA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdDA)
                .build();

        Mockito.when(periodsClosedRepository.findAll(Mockito.any(Specification.class))).thenReturn(List.of(pcDA), List.of(pcSA));

        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            GDK.getDataFromDB(copyDate, scenarioSource, scenarioDestination);
        });

        assertEquals("Дата начала копирования со сценария-источника всегда должна быть меньше любого первого открытого периода каждого из двух сценариев, либо равна null", thrown.getMessage());
    }

    @Test
    public void test7_copyDate_LESS_FirstOpenPeriodScenarioSource_AND_FirstOpenPeriodScenarioDestination() {
        ZonedDateTime copyDate = getDate(31, 10, 2019);

        long scenarioSource = 1L;
        long scenarioDestination = 2L;

        Scenario scenarioSourceAddition = getSC("SCENARIO_S_A", ScenarioStornoStatus.ADDITION, user);
        Scenario scenarioDestinationFull = getSC("SCENARIO_D_F", ScenarioStornoStatus.FULL, user);

        Mockito.when(scenarioRepository.findById(scenarioSource)).thenReturn(Optional.ofNullable(scenarioSourceAddition));
        Mockito.when(scenarioRepository.findById(scenarioDestination)).thenReturn(Optional.ofNullable(scenarioDestinationFull));

        PeriodsClosedID pcIdSA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioSourceAddition)
                .build();

        PeriodsClosed pcSA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdSA)
                .build();

        PeriodsClosedID pcIdDA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioDestinationFull)
                .build();

        PeriodsClosed pcDA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdDA)
                .build();

        Mockito.when(periodsClosedRepository.findAll(Mockito.any(Specification.class))).thenReturn(List.of(pcDA), List.of(pcSA));

        ArrayList<LeasingDeposit> cv = new ArrayList();
        cv.add(leasingDeposit1);

        Mockito.when(leasingDepositRepository.findAll(Mockito.any(Specification.class))).thenReturn(new ArrayList(), cv);

        try {
            GDK.getDataFromDB(copyDate, scenarioSource, scenarioDestination);
        } catch (Exception e) {
            Assert.fail();
        }

    }

    @Test
    public void test8_FirstOpenPeriodScenarioSource_LESS_FirstOpenPeriodScenarioDestination() {
        long scenarioSource = 1L;
        long scenarioDestination = 2L;

        Scenario scenarioSourceAddition = getSC("SCENARIO_S_A", ScenarioStornoStatus.ADDITION, user);
        Scenario scenarioDestinationFull = getSC("SCENARIO_D_F", ScenarioStornoStatus.FULL, user);

        Mockito.when(scenarioRepository.findById(scenarioSource)).thenReturn(Optional.ofNullable(scenarioSourceAddition));
        Mockito.when(scenarioRepository.findById(scenarioDestination)).thenReturn(Optional.ofNullable(scenarioDestinationFull));

        PeriodsClosedID pcIdSA = PeriodsClosedID.builder()
                .period(getPer(31, 10, 2019))
                .scenario(scenarioSourceAddition)
                .build();

        PeriodsClosed pcSA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdSA)
                .build();

        PeriodsClosedID pcIdDA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioDestinationFull)
                .build();

        PeriodsClosed pcDA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdDA)
                .build();

        Mockito.when(periodsClosedRepository.findAll(Mockito.any(Specification.class))).thenReturn(List.of(pcDA), List.of(pcSA));

        ArrayList<LeasingDeposit> cv = new ArrayList();
        cv.add(leasingDeposit1);

        Mockito.when(leasingDepositRepository.findAll(Mockito.any(Specification.class))).thenReturn(new ArrayList(), cv);

        try {
            GDK.getDataFromDB(scenarioSource, scenarioDestination);
        } catch (Exception e) {
            Assert.fail();
        }

    }

    @Test
    public void test9_FirstOpenPeriodScenarioSource_EQUALS_FirstOpenPeriodScenarioDestination() {
        long scenarioSource = 1L;
        long scenarioDestination = 2L;

        Scenario scenarioSourceAddition = getSC("SCENARIO_S_A", ScenarioStornoStatus.ADDITION, user);
        Scenario scenarioDestinationFull = getSC("SCENARIO_D_F", ScenarioStornoStatus.FULL, user);

        Mockito.when(scenarioRepository.findById(scenarioSource)).thenReturn(Optional.ofNullable(scenarioSourceAddition));
        Mockito.when(scenarioRepository.findById(scenarioDestination)).thenReturn(Optional.ofNullable(scenarioDestinationFull));

        PeriodsClosedID pcIdSA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioSourceAddition)
                .build();

        PeriodsClosed pcSA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdSA)
                .build();

        PeriodsClosedID pcIdDA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioDestinationFull)
                .build();

        PeriodsClosed pcDA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdDA)
                .build();

        Mockito.when(periodsClosedRepository.findAll(Mockito.any(Specification.class))).thenReturn(List.of(pcDA), List.of(pcSA));

        ArrayList<LeasingDeposit> cv = new ArrayList();
        cv.add(leasingDeposit1);

        Mockito.when(leasingDepositRepository.findAll(Mockito.any(Specification.class))).thenReturn(new ArrayList(), cv);

        try {
            GDK.getDataFromDB(scenarioSource, scenarioDestination);
        } catch (Exception e) {
            Assert.fail();
        }

    }

    @Test
    public void test10_FirstOpenPeriodScenarioSource_GREATER_FirstOpenPeriodScenarioDestination() {
        long scenarioSource = 1L;
        long scenarioDestination = 2L;

        Scenario scenarioSourceAddition = getSC("SCENARIO_S_A", ScenarioStornoStatus.ADDITION, user);
        Scenario scenarioDestinationFull = getSC("SCENARIO_D_F", ScenarioStornoStatus.FULL, user);

        Mockito.when(scenarioRepository.findById(scenarioSource)).thenReturn(Optional.ofNullable(scenarioSourceAddition));
        Mockito.when(scenarioRepository.findById(scenarioDestination)).thenReturn(Optional.ofNullable(scenarioDestinationFull));

        PeriodsClosedID pcIdSA = PeriodsClosedID.builder()
                .period(getPer(31, 12, 2019))
                .scenario(scenarioSourceAddition)
                .build();

        PeriodsClosed pcSA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdSA)
                .build();

        PeriodsClosedID pcIdDA = PeriodsClosedID.builder()
                .period(getPer(30, 11, 2019))
                .scenario(scenarioDestinationFull)
                .build();

        PeriodsClosed pcDA = PeriodsClosed.builder()
                .ISCLOSED(null)
                .periodsClosedID(pcIdDA)
                .build();

        Mockito.when(periodsClosedRepository.findAll(Mockito.any(Specification.class))).thenReturn(List.of(pcDA), List.of(pcSA));

        ArrayList<LeasingDeposit> cv = new ArrayList();
        cv.add(leasingDeposit1);

        Mockito.when(leasingDepositRepository.findAll(Mockito.any(Specification.class))).thenReturn(new ArrayList(), cv);

        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            GDK.getDataFromDB(scenarioSource, scenarioDestination);
        });

        assertEquals("Дата первого открытого периода сценария-источника всегда должна быть меньше ИЛИ равно первому открытому периоду сценария-получателя", thrown.getMessage());
    }

    @Test
    public void getDataFromDB_shouldThrowException_whenCopyDateIsNull() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
            GeneralDataKeeper gdk = new GeneralDataKeeper();
            gdk.getDataFromDB(null, 1L, 2L);
        });

        assertEquals("Wrong! copyDate equals null!", exception.getMessage());
    }

    public static void InitializeGeneraldata() {
        scenarioSourceAddition = getSC("SCENARIO_S_A", ScenarioStornoStatus.ADDITION, user);
        scenarioSourceFull = getSC("SCENARIO_S_F", ScenarioStornoStatus.ADDITION, user);
        scenarioDestinationFull = getSC("SCENARIO_D_F", ScenarioStornoStatus.FULL, user);
        scenarioDestinationAddition = getSC("SCENARIO_D_A", ScenarioStornoStatus.ADDITION, user);

        usd = getCUR("USD");
        C1001 = getEN("C1001", "Компания-1");
        CP = getCP("ООО \"Лизинговая компания\"");

        long all = LocalDate.of(2010, 1, 31).datesUntil(LocalDate.of(2030, 12, 31), java.time.Period.ofMonths(1)).count();

        LocalDate.of(2010, 1, 31).datesUntil(LocalDate.of(2030, 12, 31), java.time.Period.ofMonths(1)).forEach((date) ->
                {
                    LocalDate newDate = date.withDayOfMonth(date.lengthOfMonth());

                    periods.put(getDate(newDate.getDayOfMonth(), newDate.getMonthValue(), newDate.getYear()), getPer(newDate.getDayOfMonth(), newDate.getMonthValue(), newDate.getYear()));
                }
        );

        dur_0_12_M = getDur("<= 12 мес.", 0, 12);
        dur_25_36_M = getDur("25-36 мес.", 25, 36);

        depRateC1001_0_12M = getDepRate(C1001, getDate(01, 01, 1970), getDate(31, 12, 2999), usd, dur_0_12_M, scenarioSourceAddition, BigDecimal.valueOf(2.0));
        depRateC1001_25_36M = getDepRate(C1001, getDate(01, 01, 1970), getDate(31, 12, 2999), usd, dur_25_36_M, scenarioSourceAddition, BigDecimal.valueOf(5.0));

        ExR = new ArrayList<>();
        ExR.add(getExRate(scenarioSourceAddition, getDate(10, 3, 2017), usd, BigDecimal.valueOf(58.8318), BigDecimal.ZERO));
        ExR.add(getExRate(scenarioSourceAddition, getDate(31, 3, 2017), usd, BigDecimal.valueOf(56.3779), BigDecimal.valueOf(58.1091)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(30, 4, 2017), usd, BigDecimal.valueOf(56.9838082901554), BigDecimal.valueOf(56.4315074286036)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(31, 5, 2017), usd, BigDecimal.valueOf(56.5168010059989), BigDecimal.valueOf(57.171996848083)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(30, 6, 2017), usd, BigDecimal.valueOf(59.0855029786337), BigDecimal.valueOf(57.8311009199966)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(31, 7, 2017), usd, BigDecimal.valueOf(59.543597652502), BigDecimal.valueOf(59.6707093574817)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(31, 8, 2017), usd, BigDecimal.valueOf(58.7306), BigDecimal.valueOf(59.6497128133555)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(30, 9, 2017), usd, BigDecimal.valueOf(58.0168999895548), BigDecimal.valueOf(57.6953966972068)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(31, 10, 2017), usd, BigDecimal.valueOf(57.8716), BigDecimal.valueOf(57.7305008320361)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(30, 11, 2017), usd, BigDecimal.valueOf(58.3311), BigDecimal.valueOf(58.9212082863353)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(31, 12, 2017), usd, BigDecimal.valueOf(57.6002), BigDecimal.valueOf(58.5887999151509)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(31, 1, 2018), usd, BigDecimal.valueOf(56.2914), BigDecimal.valueOf(56.7874891077606)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(28, 2, 2018), usd, BigDecimal.valueOf(55.6717), BigDecimal.valueOf(56.8124108208847)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(31, 3, 2018), usd, BigDecimal.valueOf(57.2649), BigDecimal.valueOf(57.0343978412931)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(30, 4, 2018), usd, BigDecimal.valueOf(61.9997), BigDecimal.valueOf(60.4623078997034)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(31, 5, 2018), usd, BigDecimal.valueOf(62.5937), BigDecimal.valueOf(62.2090013772315)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(30, 6, 2018), usd, BigDecimal.valueOf(62.7565), BigDecimal.valueOf(62.7143124565438)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(31, 7, 2018), usd, BigDecimal.valueOf(62.7805), BigDecimal.valueOf(62.8828032372803)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(31, 8, 2018), usd, BigDecimal.valueOf(68.0821), BigDecimal.valueOf(66.1231037757643)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(30, 9, 2018), usd, BigDecimal.valueOf(65.5906), BigDecimal.valueOf(67.6597104818259)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(31, 10, 2018), usd, BigDecimal.valueOf(65.7742), BigDecimal.valueOf(65.8868068638933)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(30, 11, 2018), usd, BigDecimal.valueOf(66.6342), BigDecimal.valueOf(65.8868102499607)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(31, 12, 2018), usd, BigDecimal.valueOf(69.4706), BigDecimal.valueOf(65.8867929292929)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(31, 1, 2019), usd, BigDecimal.valueOf(67.5795), BigDecimal.valueOf(65.8868071622573)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(28, 2, 2019), usd, BigDecimal.valueOf(68.5500), BigDecimal.valueOf(65.8867934993621)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(31, 3, 2019), usd, BigDecimal.valueOf(68.3300), BigDecimal.valueOf(65.8867985728187)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(30, 4, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.886789061497)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(31, 5, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8868017917687)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(30, 6, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867890889642)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(31, 7, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867887476067)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(31, 8, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867919915907)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(30, 9, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867896047699)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(31, 10, 2019), usd, BigDecimal.valueOf(67.5699997265878), BigDecimal.valueOf(65.8867901653654)));
        ExR.add(getExRate(scenarioSourceAddition, getDate(30, 11, 2019), usd, BigDecimal.valueOf(64.8114), BigDecimal.valueOf(63.3386)));
        ExR.add(getExRate(scenarioDestinationFull, getDate(30, 11, 2019), usd, BigDecimal.valueOf(64.8114), BigDecimal.valueOf(63.3386)));

        for (Period p : periods.values()) {
            PeriodsClosedID periodsClosedID_scenarioSource = PeriodsClosedID.builder()
                    .scenario(scenarioSourceAddition)
                    .period(p)
                    .build();

            PeriodsClosedID periodsClosedID_scenarioDestination = PeriodsClosedID.builder()
                    .scenario(scenarioDestinationFull)
                    .period(p)
                    .build();

            PeriodsClosed pc_scenarioSource = new PeriodsClosed();
            pc_scenarioSource.setPeriodsClosedID(periodsClosedID_scenarioSource);
            if (p.getDate().isBefore(getDate(30, 11, 2019))) pc_scenarioSource.setISCLOSED(STATUS_X.X);

            PeriodsClosed pc_scenarioDestination = new PeriodsClosed();
            pc_scenarioDestination.setPeriodsClosedID(periodsClosedID_scenarioDestination);
            if (p.getDate().isBefore(getDate(30, 11, 2019))) pc_scenarioDestination.setISCLOSED(STATUS_X.X);
        }

    }

    public static void create_LD_1_NormalTestLD() {
        //Депозит только для факта 1
        leasingDeposit1 = new LeasingDeposit();
        leasingDeposit1.setId(1L);
        leasingDeposit1.setCounterpartner(CP);
        leasingDeposit1.setCompany(C1001);
        leasingDeposit1.setCurrency(usd);
        leasingDeposit1.setDeposit_sum_not_disc(BigDecimal.valueOf(100000.00));
        leasingDeposit1.setStart_date(getDate(10, 3, 2017));
        leasingDeposit1.setScenario(scenarioSourceAddition);
        leasingDeposit1.setIs_created(STATUS_X.X);

        EndDateID endDateID_31032017_20102019 = EndDateID.builder()
                .leasingDeposit_id(leasingDeposit1.getId())
                .period(periods.get(getDate(31, 3, 2017)))
                .scenario(scenarioSourceAddition)
                .build();

        EndDate ed_ld1_31032017_20102019 = new EndDate();
        ed_ld1_31032017_20102019.setEndDateID(endDateID_31032017_20102019);
        ed_ld1_31032017_20102019.setEnd_Date(getDate(20, 10, 2019));


        EndDateID endDateID_31082017_20122019 = EndDateID.builder()
                .leasingDeposit_id(leasingDeposit1.getId())
                .period(periods.get(getDate(31, 8, 2017)))
                .scenario(scenarioSourceAddition)
                .build();

        EndDate ed_ld1_31082017_20122019 = new EndDate();
        ed_ld1_31082017_20122019.setEndDateID(endDateID_31082017_20122019);
        ed_ld1_31082017_20122019.setEnd_Date(getDate(20, 12, 2019));

        EndDateID endDateID_31102017_20112019 = EndDateID.builder()
                .leasingDeposit_id(leasingDeposit1.getId())
                .period(periods.get(getDate(31, 10, 2017)))
                .scenario(scenarioSourceAddition)
                .build();

        EndDate ed_ld1_31102017_20112019 = new EndDate();
        ed_ld1_31102017_20112019.setEndDateID(endDateID_31102017_20112019);
        ed_ld1_31102017_20112019.setEnd_Date(getDate(20, 11, 2019));

        EndDateID endDateID_30112019_03112019 = EndDateID.builder()
                .leasingDeposit_id(leasingDeposit1.getId())
                .period(periods.get(getDate(30, 11, 2019)))
                .scenario(scenarioSourceAddition)
                .build();

        EndDate ed_ld1_30112019_03112019 = new EndDate();
        ed_ld1_30112019_03112019.setEndDateID(endDateID_30112019_03112019);
        ed_ld1_30112019_03112019.setEnd_Date(getDate(03, 11, 2019));

        leasingDeposit1.setEnd_dates(Set.of(ed_ld1_31032017_20102019, ed_ld1_31082017_20122019, ed_ld1_31102017_20112019, ed_ld1_30112019_03112019));
        leasingDeposit1.setEntries(new HashSet<>());
    }

}

