package TestsForLeasingDeposit.Calculator.BetweenTwoScenarios;

import LD.config.Security.model.User.User;
import LD.model.Company.Company;
import LD.model.Counterpartner.Counterpartner;
import LD.model.Currency.Currency;
import LD.model.DepositRate.DepositRate;
import LD.model.Duration.Duration;
import LD.model.EndDate.EndDate;
import LD.model.EndDate.EndDateID;
import LD.model.Entry.Entry;
import LD.model.Entry.EntryID;
import LD.model.Enums.EntryStatus;
import LD.model.Enums.LeasingDepositDuration;
import LD.model.Enums.STATUS_X;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.ExchangeRate.ExchangeRate;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.Period.Period;
import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.PeriodsClosed.PeriodsClosedID;
import LD.model.Scenario.Scenario;
import LD.repository.DepositRatesRepository;
import LD.service.Calculators.LeasingDeposits.EntryCalculator;
import LD.service.Calculators.LeasingDeposits.GeneralDataKeeper;
import TestsForLeasingDeposit.Calculator.Builders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static TestsForLeasingDeposit.Calculator.Builders.*;
import static org.junit.Assert.assertEquals;

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

@ExtendWith(MockitoExtension.class)
public class ScenarioSourceStatus_ADD_ScenarioDestinationStatus_FULL {

    static Scenario scenarioSource;
    static Scenario scenarioDestination;
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

    @Mock
    GeneralDataKeeper GDK;
    @Mock
    DepositRatesRepository depositRatesRepository;

    final String SCENARIO_SOURCE = "SCENARIO_SOURCE";
    final String SCENARIO_DESTINATION = "SCENARIO_DESTINATION";
    List<Entry> calculatedEntries = new ArrayList<>();
    ExecutorService threadExecutor;

    static final int constForExRates = 100;

    @BeforeEach
    public void setUp() throws ExecutionException, InterruptedException {
        InitializeGeneraldata();
        create_LD_1_NormalTestLD();

        //расчет на сценарии-источнике
        //копирование сентября и октября со сценария-источника
        //расчет сценария-получателя на ноябре
        //итого всего три транзакции на сценарии-получателе новые со стасом ACTUAL
        //также добавить сюда все транзакции до ноября = 32, которые будут сторнированы
        Mockito.lenient().when(GDK.getLeasingDeposits()).thenReturn(List.of(leasingDeposit1));
        Mockito.lenient().when(GDK.getFrom()).thenReturn(scenarioSource);
        Mockito.lenient().when(GDK.getTo()).thenReturn(scenarioSource);
        Mockito.lenient().when(GDK.getFirstOpenPeriod_ScenarioTo()).thenReturn(getDate(30, 9, 2018));
        Mockito.lenient().when(GDK.getAllExRates()).thenReturn(ExR);
        Mockito.lenient().when(GDK.getAllPeriods()).thenReturn(List.copyOf(periods.values()));

        threadExecutor = Executors.newFixedThreadPool(10);

        calculatorBeforeTestForScenarioSource = new EntryCalculator(leasingDeposit1, GDK, depositRatesRepository);
        Mockito.when(depositRatesRepository.findAll(Mockito.any(Specification.class))).thenReturn(List.of(depRateC1001_25_36M), List.of(depRateC1001_25_36M));

        Future<List<Entry>> entries = threadExecutor.submit(calculatorBeforeTestForScenarioSource);
        leasingDeposit1.getEntries().addAll(entries.get());

        //расчёт на сценарий-получатель
        Mockito.lenient().when(GDK.getTo()).thenReturn(scenarioDestination);
        Mockito.lenient().when(GDK.getPeriod_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo()).thenReturn(getDate(31, 8, 2018));
        Mockito.lenient().when(GDK.getFirstOpenPeriod_ScenarioFrom()).thenReturn(getDate(30, 9, 2018));
        Mockito.lenient().when(GDK.getFirstOpenPeriod_ScenarioTo()).thenReturn(getDate(31, 12, 2019));

        calculatorTestForScenarioSourceDestination = new EntryCalculator(leasingDeposit1, GDK, depositRatesRepository);

        entries = threadExecutor.submit(calculatorTestForScenarioSourceDestination);
        leasingDeposit1.getEntries().addAll(entries.get());

        threadExecutor.shutdown();
    }

    @Test
    public void test2_NumberOfStornoTransactions() {
        assertEquals(18, calculatorTestForScenarioSourceDestination.getCalculatedStornoDeletedEntries().stream()
                .filter(entry -> entry.getEntryID().getScenario().equals(scenarioDestination))
                .filter(entry -> entry.getStatus() == EntryStatus.STORNO)
                .count());
    }

    @Test
    public void test3_NumberOfAllTransactionsStornoAndCalculated() {
        assertEquals(34, calculatorTestForScenarioSourceDestination.getCalculatedStornoDeletedEntries().size());
    }

    @Test
    public void test4_NumberOfCalculatedTransactions() {
        assertEquals(16, calculatorTestForScenarioSourceDestination.getCalculatedStornoDeletedEntries().stream()
                .filter(entry -> entry.getEntryID().getScenario().equals(scenarioDestination))
                .filter(entry -> entry.getStatus() == EntryStatus.ACTUAL)
                .count());
    }

    @Test
    public void test5_ActualEntryBasedOnLeasingDeposit() {
        //31.08.2018
        var LD1_31082018 = leasingDeposit1.getEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 8, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().equals(scenarioDestination))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
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

        assertEquals(BigDecimal.valueOf(6160), LD1_31082018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(390), LD1_31082018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6549), LD1_31082018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(362894), LD1_31082018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(25761), LD1_31082018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(388655), LD1_31082018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

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
        assertEquals(LeasingDepositDuration.LT, LD1_31082018.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_31082018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //30.09.2018
        var LD1_30092018 = leasingDeposit1.getEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 9, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().equals(scenarioDestination))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
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

        assertEquals(BigDecimal.valueOf(6549), LD1_30092018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(379), LD1_30092018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6928), LD1_30092018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(388655), LD1_30092018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(25613581), LD1_30092018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(26002237), LD1_30092018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        assertEquals(BigDecimal.valueOf(87663), LD1_30092018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5968303), LD1_30092018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5749889801L), LD1_30092018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5743921498L), LD1_30092018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(445880), LD1_30092018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(454393305), LD1_30092018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(428333843), LD1_30092018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-6172255341L), LD1_30092018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.LT, LD1_30092018.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_30092018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.10.2018
        var LD1_31102018 = leasingDeposit1.getEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 10, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().equals(scenarioDestination))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
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

        assertEquals(BigDecimal.valueOf(6928), LD1_31102018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(393), LD1_31102018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(7321), LD1_31102018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(26002237), LD1_31102018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(25879132), LD1_31102018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(51881369), LD1_31102018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        assertEquals(BigDecimal.valueOf(87663), LD1_31102018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5749889801L), LD1_31102018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5765984786L), LD1_31102018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(16094986), LD1_31102018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(454393305), LD1_31102018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(481500136), LD1_31102018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(1227699), LD1_31102018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-17322685), LD1_31102018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.LT, LD1_31102018.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_31102018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //30.11.2018
        var LD1_30112018 = leasingDeposit1.getEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 11, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().equals(scenarioDestination))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
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

        assertEquals(BigDecimal.valueOf(7321), LD1_30112018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(382), LD1_30112018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(7702), LD1_30112018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(51881369), LD1_30112018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(25146635), LD1_30112018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(77028004), LD1_30112018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        assertEquals(BigDecimal.valueOf(87663), LD1_30112018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5765984786L), LD1_30112018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5841375304L), LD1_30112018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(75390517), LD1_30112018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(481500136), LD1_30112018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(513227659), LD1_30112018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6580889), LD1_30112018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-81971406), LD1_30112018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_30112018.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(5841375304L), LD1_30112018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(513227659), LD1_30112018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_30112018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.12.2018
        var LD1_31122018 = leasingDeposit1.getEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 12, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().equals(scenarioDestination))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
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

        assertEquals(BigDecimal.valueOf(7702), LD1_31122018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(396), LD1_31122018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(8098), LD1_31122018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(77028004), LD1_31122018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(26091007), LD1_31122018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(103119010), LD1_31122018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        assertEquals(BigDecimal.valueOf(87663), LD1_31122018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5841375304L), LD1_31122018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6090023460L), LD1_31122018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(248648156), LD1_31122018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(513227659), LD1_31122018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(562584260), LD1_31122018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(23265594), LD1_31122018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-271913750), LD1_31122018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_31122018.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(6090023460L), LD1_31122018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(562584260), LD1_31122018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5841375304L), LD1_31122018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(513227659), LD1_31122018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31122018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31122018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.01.2019
        var LD1_31012019 = leasingDeposit1.getEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 1, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().equals(scenarioDestination))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
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

        assertEquals(BigDecimal.valueOf(8098), LD1_31012019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(398), LD1_31012019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(8496), LD1_31012019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(103119010), LD1_31012019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(26199353), LD1_31012019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(129318364), LD1_31012019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        assertEquals(BigDecimal.valueOf(87663), LD1_31012019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6090023460L), LD1_31012019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5924243380L), LD1_31012019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-165780080), LD1_31012019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(562584260), LD1_31012019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(574142265), LD1_31012019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-14641349), LD1_31012019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(180421429), LD1_31012019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_31012019.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(5924243380L), LD1_31012019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(574142265), LD1_31012019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6090023460L), LD1_31012019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(562584260), LD1_31012019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31012019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31012019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //28.02.2019
        var LD1_28022019 = leasingDeposit1.getEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(28, 2, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().equals(scenarioDestination))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
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

        assertEquals(BigDecimal.valueOf(8496), LD1_28022019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(361), LD1_28022019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(8856), LD1_28022019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(129318364), LD1_28022019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(23757422), LD1_28022019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(153075786), LD1_28022019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        assertEquals(BigDecimal.valueOf(87663), LD1_28022019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5924243380L), LD1_28022019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6009320659L), LD1_28022019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(85077279), LD1_28022019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(574142265), LD1_28022019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(607105166), LD1_28022019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(9205479), LD1_28022019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-94282758), LD1_28022019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_28022019.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(6009320659L), LD1_28022019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(607105166), LD1_28022019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5924243380L), LD1_28022019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(574142265), LD1_28022019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_28022019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_28022019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.03.2019
        var LD1_31032019 = leasingDeposit1.getEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 3, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().equals(scenarioDestination))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
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

        assertEquals(BigDecimal.valueOf(8856), LD1_31032019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(401), LD1_31032019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(9257), LD1_31032019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(153075786), LD1_31032019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(26406791), LD1_31032019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(179482577), LD1_31032019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        assertEquals(BigDecimal.valueOf(87663), LD1_31032019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6009320659L), LD1_31032019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5990034755L), LD1_31032019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-19285904), LD1_31032019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(607105166), LD1_31032019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(632542767), LD1_31032019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-969190), LD1_31032019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(20255095), LD1_31032019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_31032019.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(5990034755L), LD1_31032019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(632542767), LD1_31032019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6009320659L), LD1_31032019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(607105166), LD1_31032019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31032019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31032019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //30.04.2019
        var LD1_30042019 = leasingDeposit1.getEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 4, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().equals(scenarioDestination))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
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

        assertEquals(BigDecimal.valueOf(9257), LD1_30042019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(389), LD1_30042019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(9647), LD1_30042019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(179482577), LD1_30042019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(25659354), LD1_30042019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(205141932), LD1_30042019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        assertEquals(BigDecimal.valueOf(87663), LD1_30042019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5990034755L), LD1_30042019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410561L), LD1_30042019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-66624194), LD1_30042019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(632542767), LD1_30042019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(651822181), LD1_30042019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-6379941), LD1_30042019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(73004135), LD1_30042019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_30042019.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(5923410561L), LD1_30042019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(651822181), LD1_30042019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5990034755L), LD1_30042019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(632542767), LD1_30042019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_30042019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_30042019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.05.2019
        var LD1_31052019 = leasingDeposit1.getEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 5, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().equals(scenarioDestination))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
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

        assertEquals(BigDecimal.valueOf(9647), LD1_31052019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(404), LD1_31052019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(10051), LD1_31052019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(205141932), LD1_31052019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(26622993), LD1_31052019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(231764925), LD1_31052019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        assertEquals(BigDecimal.valueOf(87663), LD1_31052019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410561L), LD1_31052019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410569L), LD1_31052019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(9), LD1_31052019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(651822181), LD1_31052019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(679125308), LD1_31052019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(680134), LD1_31052019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-680142), LD1_31052019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_31052019.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(5923410569L), LD1_31052019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(679125308), LD1_31052019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410561L), LD1_31052019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(651822181), LD1_31052019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31052019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31052019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //30.06.2019
        var LD1_30062019 = leasingDeposit1.getEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 6, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().equals(scenarioDestination))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
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

        assertEquals(BigDecimal.valueOf(10051), LD1_30062019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(393), LD1_30062019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(10443), LD1_30062019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(231764925), LD1_30062019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(25869435), LD1_30062019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(257634360), LD1_30062019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        assertEquals(BigDecimal.valueOf(87663), LD1_30062019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410569L), LD1_30062019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410613L), LD1_30062019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(44), LD1_30062019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(679125308), LD1_30062019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(705655635), LD1_30062019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(660892), LD1_30062019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-660936), LD1_30062019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_30062019.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(5923410613L), LD1_30062019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(705655635), LD1_30062019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410569L), LD1_30062019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(679125308), LD1_30062019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_30062019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_30062019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.07.2019
        var LD1_31072019 = leasingDeposit1.getEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 7, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().equals(scenarioDestination))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
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

        assertEquals(BigDecimal.valueOf(10443), LD1_31072019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(407), LD1_31072019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(10851), LD1_31072019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(257634360), LD1_31072019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(26840958), LD1_31072019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(284475319), LD1_31072019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        assertEquals(BigDecimal.valueOf(87663), LD1_31072019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410613L), LD1_31072019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410561L), LD1_31072019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-53), LD1_31072019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(705655635), LD1_31072019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(733182293), LD1_31072019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(685700), LD1_31072019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-685648), LD1_31072019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_31072019.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(5923410561L), LD1_31072019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(733182293), LD1_31072019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410613L), LD1_31072019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(705655635), LD1_31072019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31072019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31072019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.08.2019
        var LD1_31082019 = leasingDeposit1.getEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 8, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().equals(scenarioDestination))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
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

        assertEquals(BigDecimal.valueOf(10851), LD1_31082019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(409), LD1_31082019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(11260), LD1_31082019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(284475319), LD1_31082019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(26952415), LD1_31082019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(311427733), LD1_31082019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        assertEquals(BigDecimal.valueOf(87663), LD1_31082019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410561L), LD1_31082019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410561L), LD1_31082019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(0), LD1_31082019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(733182293), LD1_31082019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(760823261), LD1_31082019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(688552), LD1_31082019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-688552), LD1_31082019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_31082019.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(5923410561L), LD1_31082019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(760823261), LD1_31082019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410561L), LD1_31082019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(733182293), LD1_31082019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31082019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31082019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //30.09.2019
        var LD_ScenarioDestination_30092019 = leasingDeposit1.getEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 9, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().equals(scenarioDestination))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD_ScenarioDestination_30092019.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD_ScenarioDestination_30092019.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30092019.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30092019.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD_ScenarioDestination_30092019.getEnd_date_at_this_period());
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30092019.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30092019.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30092019.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30092019.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30092019.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30092019.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30092019.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30092019.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30092019.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

        assertEquals(BigDecimal.valueOf(11260), LD_ScenarioDestination_30092019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(397), LD_ScenarioDestination_30092019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(11657), LD_ScenarioDestination_30092019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(311427733), LD_ScenarioDestination_30092019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(26189537), LD_ScenarioDestination_30092019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(337617270), LD_ScenarioDestination_30092019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        assertEquals(BigDecimal.valueOf(87663), LD_ScenarioDestination_30092019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410561L), LD_ScenarioDestination_30092019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410569L), LD_ScenarioDestination_30092019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(9), LD_ScenarioDestination_30092019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30092019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(760823261), LD_ScenarioDestination_30092019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(787681863), LD_ScenarioDestination_30092019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(669065), LD_ScenarioDestination_30092019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30092019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-669074), LD_ScenarioDestination_30092019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30092019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30092019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30092019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD_ScenarioDestination_30092019.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(5923410569L), LD_ScenarioDestination_30092019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(787681863), LD_ScenarioDestination_30092019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410561L), LD_ScenarioDestination_30092019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(760823261), LD_ScenarioDestination_30092019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD_ScenarioDestination_30092019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD_ScenarioDestination_30092019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.10.2019
        var LD_ScenarioDestination_31102019 = leasingDeposit1.getEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 10, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().equals(scenarioDestination))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD_ScenarioDestination_31102019.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD_ScenarioDestination_31102019.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_31102019.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_31102019.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD_ScenarioDestination_31102019.getEnd_date_at_this_period());
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_31102019.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_31102019.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_31102019.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_31102019.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_31102019.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_31102019.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_31102019.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_31102019.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_31102019.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

        assertEquals(BigDecimal.valueOf(11657), LD_ScenarioDestination_31102019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(412), LD_ScenarioDestination_31102019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(12070), LD_ScenarioDestination_31102019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(337617270), LD_ScenarioDestination_31102019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(27173082), LD_ScenarioDestination_31102019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(364790352), LD_ScenarioDestination_31102019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        assertEquals(BigDecimal.valueOf(87663), LD_ScenarioDestination_31102019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410569L), LD_ScenarioDestination_31102019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410569L), LD_ScenarioDestination_31102019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_31102019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_31102019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(787681863), LD_ScenarioDestination_31102019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(815549135), LD_ScenarioDestination_31102019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(694191), LD_ScenarioDestination_31102019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_31102019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-694191), LD_ScenarioDestination_31102019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_31102019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_31102019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_31102019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD_ScenarioDestination_31102019.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(5923410569L), LD_ScenarioDestination_31102019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(815549135), LD_ScenarioDestination_31102019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410569L), LD_ScenarioDestination_31102019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(787681863), LD_ScenarioDestination_31102019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD_ScenarioDestination_31102019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD_ScenarioDestination_31102019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //30.11.2019
        var LD_ScenarioDestination_30112019 = leasingDeposit1.getEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 11, 2019)))
                .filter(tr -> tr.getEntryID().getScenario().equals(scenarioDestination))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD_ScenarioDestination_30112019.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD_ScenarioDestination_30112019.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30112019.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30112019.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 11, 3, 0, 0, 0, 0, ZoneId.of("UTC")), LD_ScenarioDestination_30112019.getEnd_date_at_this_period());
        assertEquals(BigDecimal.valueOf(-12137), LD_ScenarioDestination_30112019.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-714056), LD_ScenarioDestination_30112019.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(11733), LD_ScenarioDestination_30112019.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(13464041), LD_ScenarioDestination_30112019.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(1855376), LD_ScenarioDestination_30112019.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30112019.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30112019.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(13475774), LD_ScenarioDestination_30112019.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(1855376), LD_ScenarioDestination_30112019.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

        assertEquals(BigDecimal.valueOf(12097), LD_ScenarioDestination_30112019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(40), LD_ScenarioDestination_30112019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(12137), LD_ScenarioDestination_30112019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(365620251), LD_ScenarioDestination_30112019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(2539464), LD_ScenarioDestination_30112019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(368159715), LD_ScenarioDestination_30112019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        assertEquals(BigDecimal.valueOf(87863), LD_ScenarioDestination_30112019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5936886343L), LD_ScenarioDestination_30112019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5694508173L), LD_ScenarioDestination_30112019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30112019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-242378170), LD_ScenarioDestination_30112019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(817404511), LD_ScenarioDestination_30112019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(786631827), LD_ScenarioDestination_30112019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30112019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-33312148), LD_ScenarioDestination_30112019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30112019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(275690319), LD_ScenarioDestination_30112019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5694508173L), LD_ScenarioDestination_30112019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(786631827), LD_ScenarioDestination_30112019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD_ScenarioDestination_30112019.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30112019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD_ScenarioDestination_30112019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923410569L), LD_ScenarioDestination_30112019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(815549135), LD_ScenarioDestination_30112019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD_ScenarioDestination_30112019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD_ScenarioDestination_30112019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));
    }

    public static void InitializeGeneraldata() {
        User user = getAnyUser();
        scenarioSource = getSC("SCENARIO_SOURCE", ScenarioStornoStatus.ADDITION, user);
        scenarioDestination = getSC("SCENARIO_DESTINATION", ScenarioStornoStatus.FULL, user);

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

        depRateC1001_0_12M = getDepRate(C1001, getDate(01, 01, 1970), getDate(31, 12, 2999), usd, dur_0_12_M, scenarioSource, BigDecimal.valueOf(2.0));
        depRateC1001_25_36M = getDepRate(C1001, getDate(01, 01, 1970), getDate(31, 12, 2999), usd, dur_25_36_M, scenarioSource, BigDecimal.valueOf(5.0));

        ExR = new ArrayList<>();
        ExR.add(getExRate(scenarioSource, getDate(10, 3, 2017), usd, BigDecimal.valueOf(58.8318), BigDecimal.ZERO));
        ExR.add(getExRate(scenarioSource, getDate(31, 3, 2017), usd, BigDecimal.valueOf(56.3779), BigDecimal.valueOf(58.1091)));
        ExR.add(getExRate(scenarioSource, getDate(30, 4, 2017), usd, BigDecimal.valueOf(56.9838082901554), BigDecimal.valueOf(56.4315074286036)));
        ExR.add(getExRate(scenarioSource, getDate(31, 5, 2017), usd, BigDecimal.valueOf(56.5168010059989), BigDecimal.valueOf(57.171996848083)));
        ExR.add(getExRate(scenarioSource, getDate(30, 6, 2017), usd, BigDecimal.valueOf(59.0855029786337), BigDecimal.valueOf(57.8311009199966)));
        ExR.add(getExRate(scenarioSource, getDate(31, 7, 2017), usd, BigDecimal.valueOf(59.543597652502), BigDecimal.valueOf(59.6707093574817)));
        ExR.add(getExRate(scenarioSource, getDate(31, 8, 2017), usd, BigDecimal.valueOf(58.7306), BigDecimal.valueOf(59.6497128133555)));
        ExR.add(getExRate(scenarioSource, getDate(30, 9, 2017), usd, BigDecimal.valueOf(58.0168999895548), BigDecimal.valueOf(57.6953966972068)));
        ExR.add(getExRate(scenarioSource, getDate(31, 10, 2017), usd, BigDecimal.valueOf(57.8716), BigDecimal.valueOf(57.7305008320361)));
        ExR.add(getExRate(scenarioSource, getDate(30, 11, 2017), usd, BigDecimal.valueOf(58.3311), BigDecimal.valueOf(58.9212082863353)));
        ExR.add(getExRate(scenarioSource, getDate(31, 12, 2017), usd, BigDecimal.valueOf(57.6002), BigDecimal.valueOf(58.5887999151509)));
        ExR.add(getExRate(scenarioSource, getDate(31, 1, 2018), usd, BigDecimal.valueOf(56.2914), BigDecimal.valueOf(56.7874891077606)));
        ExR.add(getExRate(scenarioSource, getDate(28, 2, 2018), usd, BigDecimal.valueOf(55.6717), BigDecimal.valueOf(56.8124108208847)));
        ExR.add(getExRate(scenarioSource, getDate(31, 3, 2018), usd, BigDecimal.valueOf(57.2649), BigDecimal.valueOf(57.0343978412931)));
        ExR.add(getExRate(scenarioSource, getDate(30, 4, 2018), usd, BigDecimal.valueOf(61.9997), BigDecimal.valueOf(60.4623078997034)));
        ExR.add(getExRate(scenarioSource, getDate(31, 5, 2018), usd, BigDecimal.valueOf(62.5937), BigDecimal.valueOf(62.2090013772315)));
        ExR.add(getExRate(scenarioSource, getDate(30, 6, 2018), usd, BigDecimal.valueOf(62.7565), BigDecimal.valueOf(62.7143124565438)));
        ExR.add(getExRate(scenarioSource, getDate(31, 7, 2018), usd, BigDecimal.valueOf(62.7805), BigDecimal.valueOf(62.8828032372803)));
        ExR.add(getExRate(scenarioSource, getDate(31, 8, 2018), usd, BigDecimal.valueOf(68.0821), BigDecimal.valueOf(66.1231037757643)));
        ExR.add(getExRate(scenarioSource, getDate(30, 9, 2018), usd, BigDecimal.valueOf(65.5906), BigDecimal.valueOf(67.6597104818259)));
        ExR.add(getExRate(scenarioSource, getDate(31, 10, 2018), usd, BigDecimal.valueOf(65.7742), BigDecimal.valueOf(65.8868068638933)));
        ExR.add(getExRate(scenarioSource, getDate(30, 11, 2018), usd, BigDecimal.valueOf(66.6342), BigDecimal.valueOf(65.8868102499607)));
        ExR.add(getExRate(scenarioSource, getDate(31, 12, 2018), usd, BigDecimal.valueOf(69.4706), BigDecimal.valueOf(65.8867929292929)));
        ExR.add(getExRate(scenarioSource, getDate(31, 1, 2019), usd, BigDecimal.valueOf(67.5795), BigDecimal.valueOf(65.8868071622573)));
        ExR.add(getExRate(scenarioSource, getDate(28, 2, 2019), usd, BigDecimal.valueOf(68.5500), BigDecimal.valueOf(65.8867934993621)));
        ExR.add(getExRate(scenarioSource, getDate(31, 3, 2019), usd, BigDecimal.valueOf(68.3300), BigDecimal.valueOf(65.8867985728187)));
        ExR.add(getExRate(scenarioSource, getDate(30, 4, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.886789061497)));
        ExR.add(getExRate(scenarioSource, getDate(31, 5, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8868017917687)));
        ExR.add(getExRate(scenarioSource, getDate(30, 6, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867890889642)));
        ExR.add(getExRate(scenarioSource, getDate(31, 7, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867887476067)));
        ExR.add(getExRate(scenarioSource, getDate(31, 8, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867919915907)));
        ExR.add(getExRate(scenarioSource, getDate(30, 9, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867896047699)));
        ExR.add(getExRate(scenarioSource, getDate(31, 10, 2019), usd, BigDecimal.valueOf(67.5699997265878), BigDecimal.valueOf(65.8867901653654)));
        ExR.add(getExRate(scenarioSource, getDate(30, 11, 2019), usd, BigDecimal.valueOf(64.8114), BigDecimal.valueOf(63.3386)));

        ExR.add(getExRate(scenarioDestination, getDate(30, 9, 2018), usd, BigDecimal.valueOf(65590.5998), BigDecimal.valueOf(67659.7105)));
        ExR.add(getExRate(scenarioDestination, getDate(31, 10, 2018), usd, BigDecimal.valueOf(65774.1998), BigDecimal.valueOf(65886.8069)));
        ExR.add(getExRate(scenarioDestination, getDate(30, 11, 2018), usd, BigDecimal.valueOf(66634.2005), BigDecimal.valueOf(65886.8102)));
        ExR.add(getExRate(scenarioDestination, getDate(31, 12, 2018), usd, BigDecimal.valueOf(69470.5995), BigDecimal.valueOf(65886.7929)));
        ExR.add(getExRate(scenarioDestination, getDate(31, 1, 2019), usd, BigDecimal.valueOf(67579.4998), BigDecimal.valueOf(65886.8072)));
        ExR.add(getExRate(scenarioDestination, getDate(28, 2, 2019), usd, BigDecimal.valueOf(68550.0001), BigDecimal.valueOf(65886.7935)));
        ExR.add(getExRate(scenarioDestination, getDate(31, 3, 2019), usd, BigDecimal.valueOf(68330.0004), BigDecimal.valueOf(65886.7986)));
        ExR.add(getExRate(scenarioDestination, getDate(30, 4, 2019), usd, BigDecimal.valueOf(67569.9996), BigDecimal.valueOf(65886.7891)));
        ExR.add(getExRate(scenarioDestination, getDate(31, 5, 2019), usd, BigDecimal.valueOf(67569.9997), BigDecimal.valueOf(65886.8018)));
        ExR.add(getExRate(scenarioDestination, getDate(30, 6, 2019), usd, BigDecimal.valueOf(67570.0002), BigDecimal.valueOf(65886.7891)));
        ExR.add(getExRate(scenarioDestination, getDate(31, 7, 2019), usd, BigDecimal.valueOf(67569.9996), BigDecimal.valueOf(65886.7887)));
        ExR.add(getExRate(scenarioDestination, getDate(31, 8, 2019), usd, BigDecimal.valueOf(67569.9996), BigDecimal.valueOf(65886.7920)));
        ExR.add(getExRate(scenarioDestination, getDate(30, 9, 2019), usd, BigDecimal.valueOf(67569.9997), BigDecimal.valueOf(65886.7896)));
        ExR.add(getExRate(scenarioDestination, getDate(31, 10, 2019), usd, BigDecimal.valueOf(67569.9997), BigDecimal.valueOf(65886.7902)));
        ExR.add(getExRate(scenarioDestination, getDate(30, 11, 2019), usd, BigDecimal.valueOf(64811.4000), BigDecimal.valueOf(63338.6000)));

        for (Period p : periods.values()) {
            PeriodsClosedID periodsClosedID_scenarioSource = PeriodsClosedID.builder()
                    .scenario(scenarioSource)
                    .period(p)
                    .build();

            PeriodsClosedID periodsClosedID_scenarioDestination = PeriodsClosedID.builder()
                    .scenario(scenarioDestination)
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
        leasingDeposit1.setScenario(scenarioSource);
        leasingDeposit1.setIs_created(STATUS_X.X);

        EndDateID endDateID_31032017_20102019 = EndDateID.builder()
                .leasingDeposit_id(leasingDeposit1.getId())
                .period(periods.get(getDate(31, 3, 2017)))
                .scenario(scenarioSource)
                .build();

        EndDate ed_ld1_31032017_20102019 = new EndDate();
        ed_ld1_31032017_20102019.setEndDateID(endDateID_31032017_20102019);
        ed_ld1_31032017_20102019.setEnd_Date(getDate(20, 10, 2019));

        EndDateID endDateID_31082017_20122019 = EndDateID.builder()
                .leasingDeposit_id(leasingDeposit1.getId())
                .period(periods.get(getDate(31, 8, 2017)))
                .scenario(scenarioSource)
                .build();

        EndDate ed_ld1_31082017_20122019 = new EndDate();
        ed_ld1_31082017_20122019.setEndDateID(endDateID_31082017_20122019);
        ed_ld1_31082017_20122019.setEnd_Date(getDate(20, 12, 2019));

        EndDateID endDateID_31102017_20112019 = EndDateID.builder()
                .leasingDeposit_id(leasingDeposit1.getId())
                .period(periods.get(getDate(31, 10, 2017)))
                .scenario(scenarioSource)
                .build();

        EndDate ed_ld1_31102017_20112019 = new EndDate();
        ed_ld1_31102017_20112019.setEndDateID(endDateID_31102017_20112019);
        ed_ld1_31102017_20112019.setEnd_Date(getDate(20, 11, 2019));

        EndDateID endDateID_30112019_03112019 = EndDateID.builder()
                .leasingDeposit_id(leasingDeposit1.getId())
                .period(periods.get(getDate(30, 11, 2019)))
                .scenario(scenarioDestination)
                .build();

        EndDate ed_ld1_30112019_03112019 = new EndDate();
        ed_ld1_30112019_03112019.setEndDateID(endDateID_30112019_03112019);
        ed_ld1_30112019_03112019.setEnd_Date(getDate(03, 11, 2019));

        leasingDeposit1.setEnd_dates(Set.of(ed_ld1_31032017_20102019, ed_ld1_31082017_20122019, ed_ld1_31102017_20112019, ed_ld1_30112019_03112019));
        leasingDeposit1.setEntries(new HashSet<>());

        LocalDate.of(2017, 3, 31).datesUntil(LocalDate.of(2018, 9, 1), java.time.Period.ofMonths(1)).forEach(date ->
        {
            EntryID entryID_destinationSource = EntryID.builder()
                    .leasingDeposit_id(leasingDeposit1.getId())
                    .CALCULATION_TIME(ZonedDateTime.now())
                    .scenario(scenarioDestination)
                    .period(periods.get(getDate(date.lengthOfMonth(), date.getMonthValue(), date.getYear())))
                    .build();

            Entry entry_destinationScenario = Entry.builder()
                    .entryID(entryID_destinationSource)
                    .status(EntryStatus.ACTUAL)
                    .DISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q(BigDecimal.ZERO)
                    .DISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P(BigDecimal.ZERO)
                    .ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N(BigDecimal.ZERO)
                    .build();

            leasingDeposit1.getEntries().add(entry_destinationScenario);
        });
    }
}