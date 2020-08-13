package TestsForLeasingDeposit.Calculator.InsideOneScenario;

import LD.config.Security.model.User.User;
import LD.model.Company.Company;
import LD.model.Counterpartner.Counterpartner;
import LD.model.Currency.Currency;
import LD.model.DepositRate.DepositRate;
import LD.model.Duration.Duration;
import LD.model.EndDate.EndDate;
import LD.model.EndDate.EndDateID;
import LD.model.Entry.Entry;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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

import static TestsForLeasingDeposit.Calculator.Builders.getAnyUser;
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

@ExtendWith(SpringExtension.class)
public class LeasingDeposit_has_no_Entries_FirstOpenPeriod_LOWER_LeasingDepositEndDate {

    static Scenario fact;
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
    EntryCalculator lec;

    @MockBean
    GeneralDataKeeper GDK;
    @MockBean
    DepositRatesRepository depositRatesRepository;

    final String SCENARIO_LOAD = "FACT";
    final String SCENARIO_SAVE = "FACT";
    List<Entry> calculatedEntries = new ArrayList<>();
    ExecutorService threadExecutor;

    @BeforeEach
    public void setUp() throws ExecutionException, InterruptedException {
        InitializeGeneraldata();
        create_LD_1_NormalTestLD();

        Mockito.when(GDK.getLeasingDeposits()).thenReturn(List.of(leasingDeposit1));
        Mockito.when(GDK.getTo()).thenReturn(fact);
        Mockito.when(GDK.getFrom()).thenReturn(fact);
        Mockito.when(GDK.getFirstOpenPeriod_ScenarioTo()).thenReturn(Builders.getDate(31, 8, 2019));
        Mockito.when(GDK.getAllExRates()).thenReturn(ExR);
        Mockito.when(GDK.getAllPeriods()).thenReturn(List.copyOf(periods.values()));

        threadExecutor = Executors.newFixedThreadPool(10);

        lec = new EntryCalculator(leasingDeposit1, GDK, depositRatesRepository);
        Specification<DepositRate> dr = lec.getDepRateForLD(leasingDeposit1, lec.getLDdurationMonths());
        Mockito.when(depositRatesRepository.findAll(Mockito.any(dr.getClass()))).thenReturn(List.of(depRateC1001_25_36M));

        LeasingDeposits = GDK.getLeasingDeposits();

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();
    }

    @Test
    public void test1_NominalValue() {
        //Если более года:
        assertEquals(BigDecimal.valueOf(100000.00).setScale(1), LeasingDeposits.get(0).getDeposit_sum_not_disc());
    }

    @Test
    public void test2_CalculatingDiscountedValue() {
        //Если более года:
        assertEquals(BigDecimal.valueOf(88027.34).setScale(2), lec.getDeposit_sum_discounted_on_firstEndDate().setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    public void test4_FirstPeriodWithOutTransaction() {
        assertEquals(Builders.getDate(31, 3, 2017), lec.getFirstPeriodWithoutTransactionUTC());
    }

    @Test
    public void test5_NumberOfNewTransactions() {
        assertEquals(30, lec.getCalculatedStornoDeletedEntries().size());
    }

    @Test
    public void test6_Reg_LD_1() {
        //31.03.2017
        var LD1_31032017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 3, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD1_31032017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD1_31032017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD1_31032017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5883180), LD1_31032017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 10, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31032017.getEnd_date_at_this_period());
        assertEquals(BigDecimal.ZERO, LD1_31032017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));


        //30.04.2017
        var LD1_30042017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 4, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD1_30042017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD1_30042017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 10, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_30042017.getEnd_date_at_this_period());
        assertEquals(BigDecimal.ZERO, LD1_30042017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

        //31.05.2017
        var LD1_31052017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 5, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD1_31052017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD1_31052017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 10, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31052017.getEnd_date_at_this_period());
        assertEquals(BigDecimal.ZERO, LD1_31052017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

        //30.06.2017
        var LD1_30062017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 6, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD1_30062017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD1_30062017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 10, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_30062017.getEnd_date_at_this_period());
        assertEquals(BigDecimal.ZERO, LD1_30062017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

        //31.07.2017
        var LD1_31072017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 7, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD1_31072017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD1_31072017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 10, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31072017.getEnd_date_at_this_period());
        assertEquals(BigDecimal.ZERO, LD1_31072017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

        //31.08.2017
        var LD1_31082017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 8, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD1_31082017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD1_31082017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 12, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31082017.getEnd_date_at_this_period());
        assertEquals(BigDecimal.valueOf(-12688), LD1_31082017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-746430), LD1_31082017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-42056), LD1_31082017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-509), LD1_31082017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-821), LD1_31082017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-42565), LD1_31082017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-821), LD1_31082017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

        //30.09.2017
        var LD1_30092017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 9, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD1_30092017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD1_30092017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 12, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_30092017.getEnd_date_at_this_period());
        assertEquals(BigDecimal.ZERO, LD1_30092017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

        //31.10.2017
        var LD1_31102017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 10, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD1_31102017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD1_31102017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31102017.getEnd_date_at_this_period());
        assertEquals(BigDecimal.valueOf(-12337), LD1_31102017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-725789), LD1_31102017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(20641), LD1_31102017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-286), LD1_31102017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(563), LD1_31102017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(20355), LD1_31102017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(563), LD1_31102017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

        //30.11.2017
        var LD1_30112017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 11, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD1_30112017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD1_30112017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_30112017.getEnd_date_at_this_period());
        assertEquals(BigDecimal.ZERO, LD1_30112017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

        //31.12.2017
        var LD1_31122017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 12, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD1_31122017.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD1_31122017.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31122017.getEnd_date_at_this_period());
        assertEquals(BigDecimal.ZERO, LD1_31122017.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

        //31.01.2018
        var LD1_31012018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 1, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD1_31012018.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD1_31012018.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31012018.getEnd_date_at_this_period());
        assertEquals(BigDecimal.ZERO, LD1_31012018.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

        //28.02.2018
        var LD1_28022018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(28, 2, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD1_28022018.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD1_28022018.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_28022018.getEnd_date_at_this_period());
        assertEquals(BigDecimal.ZERO, LD1_28022018.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

        //31.03.2018
        var LD1_31032018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 3, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD1_31032018.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD1_31032018.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31032018.getEnd_date_at_this_period());
        assertEquals(BigDecimal.ZERO, LD1_31032018.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

        //30.04.2018
        var LD1_30042018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 4, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD1_30042018.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD1_30042018.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_30042018.getEnd_date_at_this_period());
        assertEquals(BigDecimal.ZERO, LD1_30042018.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

        //31.05.2018
        var LD1_31052018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 5, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD1_31052018.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD1_31052018.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31052018.getEnd_date_at_this_period());
        assertEquals(BigDecimal.ZERO, LD1_31052018.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

        //30.06.2018
        var LD1_30062018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 6, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD1_30062018.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD1_30062018.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_30062018.getEnd_date_at_this_period());
        assertEquals(BigDecimal.ZERO, LD1_30062018.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

        //31.07.2018
        var LD1_31072018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 7, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(-11973), LD1_31072018.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-704373), LD1_31072018.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getDeposit_sum_not_disc_RUB_REG_LD_1_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(ZonedDateTime.of(2019, 11, 20, 0, 0, 0, 0, ZoneId.of("UTC")), LD1_31072018.getEnd_date_at_this_period());
        assertEquals(BigDecimal.ZERO, LD1_31072018.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getREVAL_CORR_DISC_rub_REG_LD_1_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().setScale(0, RoundingMode.HALF_UP));

        //31.08.2018
        var LD1_31082018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 8, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
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

        //30.09.2018
        var LD1_30092018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 9, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
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

        //31.10.2018
        var LD1_31102018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 10, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
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

        //30.11.2018
        var LD1_30112018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 11, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
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

        //31.12.2018
        var LD1_31122018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 12, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
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

        //31.01.2019
        var LD1_31012019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 1, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
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

        //28.02.2019
        var LD1_28022019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(28, 2, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
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

        //31.03.2019
        var LD1_31032019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 3, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
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

        //30.04.2019
        var LD1_30042019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 4, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
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

        //31.05.2019
        var LD1_31052019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 5, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
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

        //30.06.2019
        var LD1_30062019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 6, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
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

        //31.07.2019
        var LD1_31072019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 7, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
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

        //31.08.2019
        var LD1_31082019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 8, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
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

    }

    @Test
    public void test7_Reg_LD_2() {
        //31.03.2017
        var LD1_31032017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 3, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.ZERO, LD1_31032017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(247), LD1_31032017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(247), LD1_31032017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(14379), LD1_31032017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(14379), LD1_31032017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //30.04.2017
        var LD1_30042017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 4, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(247), LD1_30042017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(355), LD1_30042017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(602), LD1_30042017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(14379), LD1_30042017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(20017), LD1_30042017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(34396), LD1_30042017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //31.05.2017
        var LD1_31052017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 5, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(602), LD1_31052017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(368), LD1_31052017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(970), LD1_31052017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(34396), LD1_31052017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(21041), LD1_31052017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(55436), LD1_31052017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //30.06.2017
        var LD1_30062017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 6, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(970), LD1_30062017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(358), LD1_30062017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(1328), LD1_30062017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(55436), LD1_30062017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(20681), LD1_30062017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(76117), LD1_30062017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //31.07.2017
        var LD1_31072017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 7, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(1328), LD1_31072017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(371), LD1_31072017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(1699), LD1_31072017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(76117), LD1_31072017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(22140), LD1_31072017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(98258), LD1_31072017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //31.08.2017
        var LD1_31082017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 8, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(1685), LD1_31082017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(370), LD1_31082017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(2055), LD1_31082017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(97460), LD1_31082017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(22044), LD1_31082017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(119504), LD1_31082017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //30.09.2017
        var LD1_30092017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 9, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(2055), LD1_30092017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(359), LD1_30092017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(2414), LD1_30092017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(119504), LD1_30092017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(20718), LD1_30092017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(140222), LD1_30092017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //31.10.2017
        var LD1_31102017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 10, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(2423), LD1_31102017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(374), LD1_31102017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(2797), LD1_31102017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(140785), LD1_31102017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(21596), LD1_31102017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(162381), LD1_31102017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //30.11.2017
        var LD1_30112017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 11, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(2797), LD1_30112017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(363), LD1_30112017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(3161), LD1_30112017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(162381), LD1_30112017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(21417), LD1_30112017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(183798), LD1_30112017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //31.12.2017
        var LD1_31122017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 12, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(3161), LD1_31122017.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(377), LD1_31122017.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(3538), LD1_31122017.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(183798), LD1_31122017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(22096), LD1_31122017.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(205894), LD1_31122017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //31.01.2018
        var LD1_31012018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 1, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(3538), LD1_31012018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(379), LD1_31012018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(3917), LD1_31012018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(205894), LD1_31012018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(21506), LD1_31012018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(227400), LD1_31012018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //28.02.2018
        var LD1_28022018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(28, 2, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(3917), LD1_28022018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(343), LD1_28022018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(4260), LD1_28022018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(227400), LD1_28022018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(19510), LD1_28022018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(246910), LD1_28022018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //31.03.2018
        var LD1_31032018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 3, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(4260), LD1_31032018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(382), LD1_31032018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(4642), LD1_31032018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(246910), LD1_31032018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(21770), LD1_31032018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(268680), LD1_31032018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //30.04.2018
        var LD1_30042018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 4, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(4642), LD1_30042018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(371), LD1_30042018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5013), LD1_30042018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(268680), LD1_30042018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(22426), LD1_30042018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(291106), LD1_30042018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //31.05.2018
        var LD1_31052018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 5, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(5013), LD1_31052018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(385), LD1_31052018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5398), LD1_31052018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(291106), LD1_31052018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(23940), LD1_31052018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(315046), LD1_31052018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //30.06.2018
        var LD1_30062018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 6, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(5398), LD1_30062018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(374), LD1_30062018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5772), LD1_30062018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(315046), LD1_30062018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(23451), LD1_30062018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(338497), LD1_30062018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //31.07.2018
        var LD1_31072018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 7, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(5772), LD1_31072018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(388), LD1_31072018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6160), LD1_31072018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(338497), LD1_31072018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(24397), LD1_31072018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(362894), LD1_31072018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //31.08.2018
        var LD1_31082018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 8, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(6160), LD1_31082018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(390), LD1_31082018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6549), LD1_31082018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(362894), LD1_31082018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(25761), LD1_31082018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(388655), LD1_31082018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //30.09.2018
        var LD1_30092018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 9, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(6549), LD1_30092018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(379), LD1_30092018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6928), LD1_30092018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(388655), LD1_30092018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(25614), LD1_30092018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(414269), LD1_30092018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //31.10.2018
        var LD1_31102018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 10, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(6928), LD1_31102018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(393), LD1_31102018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(7321), LD1_31102018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(414269), LD1_31102018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(25879), LD1_31102018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(440148), LD1_31102018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //30.11.2018
        var LD1_30112018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 11, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(7321), LD1_30112018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(382), LD1_30112018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(7702), LD1_30112018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(440148), LD1_30112018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(25147), LD1_30112018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(465295), LD1_30112018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //31.12.2018
        var LD1_31122018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 12, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(7702), LD1_31122018.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(396), LD1_31122018.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(8098), LD1_31122018.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(465295), LD1_31122018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(26091), LD1_31122018.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(491386), LD1_31122018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //31.01.2019
        var LD1_31012019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 1, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(8098), LD1_31012019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(398), LD1_31012019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(8496), LD1_31012019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(491386), LD1_31012019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(26199), LD1_31012019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(517585), LD1_31012019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //28.02.2019
        var LD1_28022019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(28, 2, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(8496), LD1_28022019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(361), LD1_28022019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(8856), LD1_28022019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(517585), LD1_28022019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(23757), LD1_28022019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(541342), LD1_28022019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //31.03.2019
        var LD1_31032019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 3, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(8856), LD1_31032019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(401), LD1_31032019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(9257), LD1_31032019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(541342), LD1_31032019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(26407), LD1_31032019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(567749), LD1_31032019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //30.04.2019
        var LD1_30042019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 4, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(9257), LD1_30042019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(389), LD1_30042019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(9647), LD1_30042019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(567749), LD1_30042019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(25659), LD1_30042019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(593409), LD1_30042019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //31.05.2019
        var LD1_31052019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 5, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(9647), LD1_31052019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(404), LD1_31052019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(10051), LD1_31052019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(593409), LD1_31052019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(26623), LD1_31052019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(620032), LD1_31052019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //30.06.2019
        var LD1_30062019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 6, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(10051), LD1_30062019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(393), LD1_30062019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(10443), LD1_30062019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(620032), LD1_30062019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(25869), LD1_30062019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(645901), LD1_30062019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //31.07.2019
        var LD1_31072019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 7, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(10443), LD1_31072019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(407), LD1_31072019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(10851), LD1_31072019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(645901), LD1_31072019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(26841), LD1_31072019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(672742), LD1_31072019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

        //31.08.2019
        var LD1_31082019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 8, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(10851), LD1_31082019.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(409), LD1_31082019.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(11260), LD1_31082019.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(672742), LD1_31082019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(26952), LD1_31082019.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(699694), LD1_31082019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(0, RoundingMode.HALF_UP));

    }

    @Test
    public void test8_Reg_LD_3() {
        //31.03.2017
        var LD1_31032017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 3, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(88027), LD1_31032017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31032017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(4962796), LD1_31032017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-216010), LD1_31032017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(13951), LD1_31032017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-428), LD1_31032017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(216439), LD1_31032017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.LT, LD1_31032017.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_31032017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //30.04.2017
        var LD1_30042017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 4, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(88027), LD1_30042017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(4962796), LD1_30042017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5016133), LD1_30042017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(53336), LD1_30042017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(13951), LD1_30042017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(34313), LD1_30042017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(346), LD1_30042017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-53682), LD1_30042017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.LT, LD1_30042017.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_30042017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.05.2017
        var LD1_31052017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 5, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(88027), LD1_31052017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5016133), LD1_31052017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(4975023), LD1_31052017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-41109), LD1_31052017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(34313), LD1_31052017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(54832), LD1_31052017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-522), LD1_31052017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(41632), LD1_31052017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.LT, LD1_31052017.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_31052017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //30.06.2017
        var LD1_30062017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 6, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(88027), LD1_30062017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(4975023), LD1_30062017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5201139), LD1_30062017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(226116), LD1_30062017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(54832), LD1_30062017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(78453), LD1_30062017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(2941), LD1_30062017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-229057), LD1_30062017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.LT, LD1_30062017.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_30062017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.07.2017
        var LD1_31072017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 7, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(88027), LD1_31072017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5201139), LD1_31072017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5241464), LD1_31072017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(40325), LD1_31072017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(78453), LD1_31072017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(101155), LD1_31072017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(561), LD1_31072017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-40886), LD1_31072017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.LT, LD1_31072017.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_31072017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.08.2017
        var LD1_31082017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 8, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87312), LD1_31082017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5198899), LD1_31082017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5127914), LD1_31082017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-70985), LD1_31082017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(100333), LD1_31082017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(120667), LD1_31082017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-1710), LD1_31082017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(72694), LD1_31082017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.LT, LD1_31082017.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_31082017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //30.09.2017
        var LD1_30092017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 9, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87312), LD1_30092017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5127914), LD1_30092017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5065600), LD1_30092017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-62315), LD1_30092017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(120667), LD1_30092017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(140035), LD1_30092017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-1351), LD1_30092017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(63666), LD1_30092017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.LT, LD1_30092017.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_30092017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.10.2017
        var LD1_31102017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 10, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_31102017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5085954), LD1_31102017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5073217), LD1_31102017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-12737), LD1_31102017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(140597), LD1_31102017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(161894), LD1_31102017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-299), LD1_31102017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(13037), LD1_31102017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.LT, LD1_31102017.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_31102017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //30.11.2017
        var LD1_30112017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 11, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_30112017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5073217), LD1_30112017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5113498), LD1_30112017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(40281), LD1_30112017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(161894), LD1_30112017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(184382), LD1_30112017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(1071), LD1_30112017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-41352), LD1_30112017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.LT, LD1_30112017.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_30112017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.12.2017
        var LD1_31122017 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 12, 2017).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_31122017.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5113498), LD1_31122017.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5049425), LD1_31122017.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-64073), LD1_31122017.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(184382), LD1_31122017.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(203795), LD1_31122017.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-2683), LD1_31122017.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(66756), LD1_31122017.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.LT, LD1_31122017.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_31122017.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122017.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.01.2018
        var LD1_31012018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 1, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_31012018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5049425), LD1_31012018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(4934691), LD1_31012018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-114734), LD1_31012018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(203795), LD1_31012018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(220482), LD1_31012018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-4819), LD1_31012018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(119552), LD1_31012018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.LT, LD1_31012018.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_31012018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //28.02.2018
        var LD1_28022018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(28, 2, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_28022018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(4934691), LD1_28022018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(4880366), LD1_28022018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-54325), LD1_28022018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(220482), LD1_28022018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(237173), LD1_28022018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-2819), LD1_28022018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(57144), LD1_28022018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.LT, LD1_28022018.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_28022018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.03.2018
        var LD1_31032018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 3, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_31032018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(4880366), LD1_31032018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5020031), LD1_31032018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(139665), LD1_31032018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(237173), LD1_31032018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(265819), LD1_31032018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6875), LD1_31032018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-146541), LD1_31032018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.LT, LD1_31032018.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_31032018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //30.04.2018
        var LD1_30042018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 4, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_30042018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5020031), LD1_30042018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5435100), LD1_30042018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(415068), LD1_30042018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(265819), LD1_30042018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(310793), LD1_30042018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(22549), LD1_30042018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-437617), LD1_30042018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.LT, LD1_30042018.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_30042018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.05.2018
        var LD1_31052018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 5, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_31052018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5435100), LD1_31052018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5487172), LD1_31052018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(52072), LD1_31052018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(310793), LD1_31052018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(337859), LD1_31052018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(3126), LD1_31052018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-55198), LD1_31052018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.LT, LD1_31052018.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_31052018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //30.06.2018
        var LD1_30062018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 6, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_30062018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5487172), LD1_30062018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5501443), LD1_30062018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(14272), LD1_30062018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(337859), LD1_30062018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(362204), LD1_30062018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(895), LD1_30062018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-15166), LD1_30062018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.LT, LD1_30062018.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_30062018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.07.2018
        var LD1_31072018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 7, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_31072018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5501443), LD1_31072018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5503547), LD1_31072018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(2104), LD1_31072018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(362204), LD1_31072018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(386700), LD1_31072018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(99), LD1_31072018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-2203), LD1_31072018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.LT, LD1_31072018.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.ZERO, LD1_31072018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.08.2018
        var LD1_31082018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 8, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

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
        var LD1_30092018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 9, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_30092018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5968303), LD1_30092018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5749890), LD1_30092018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-218413), LD1_30092018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(445880), LD1_30092018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(454393), LD1_30092018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-17101), LD1_30092018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30092018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(235514), LD1_30092018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
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
        var LD1_31102018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 10, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_31102018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5749890), LD1_31102018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5765985), LD1_31102018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(16095), LD1_31102018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(454393), LD1_31102018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(481500), LD1_31102018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(1228), LD1_31102018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31102018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-17323), LD1_31102018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
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
        var LD1_30112018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 11, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_30112018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5765985), LD1_30112018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5841375), LD1_30112018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(75390), LD1_30112018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(481500), LD1_30112018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(513228), LD1_30112018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6581), LD1_30112018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-81971), LD1_30112018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_30112018.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(5841375), LD1_30112018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(513228), LD1_30112018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_30112018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30112018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.12.2018
        var LD1_31122018 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 12, 2018).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_31122018.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5841375), LD1_31122018.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6090024), LD1_31122018.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(248648), LD1_31122018.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122018.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(513228), LD1_31122018.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(562584), LD1_31122018.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(23266), LD1_31122018.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122018.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-271914), LD1_31122018.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122018.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122018.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31122018.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_31122018.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(6090024), LD1_31122018.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(562584), LD1_31122018.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5841375), LD1_31122018.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(513228), LD1_31122018.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31122018.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31122018.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.01.2019
        var LD1_31012019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 1, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_31012019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6090024), LD1_31012019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5924243), LD1_31012019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-165780), LD1_31012019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(562584), LD1_31012019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(574142), LD1_31012019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-14641), LD1_31012019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(180421), LD1_31012019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31012019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_31012019.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(5924243), LD1_31012019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(574142), LD1_31012019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6090024), LD1_31012019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(562584), LD1_31012019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31012019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31012019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //28.02.2019
        var LD1_28022019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(28, 2, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_28022019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5924243), LD1_28022019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6009321), LD1_28022019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(85077), LD1_28022019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(574142), LD1_28022019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(607105), LD1_28022019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(9205), LD1_28022019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-94283), LD1_28022019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_28022019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_28022019.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(6009321), LD1_28022019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(607105), LD1_28022019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5924243), LD1_28022019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(574142), LD1_28022019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_28022019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_28022019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.03.2019
        var LD1_31032019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 3, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_31032019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6009321), LD1_31032019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5990035), LD1_31032019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-19286), LD1_31032019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(607105), LD1_31032019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(632543), LD1_31032019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-969), LD1_31032019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(20255), LD1_31032019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31032019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_31032019.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(5990035), LD1_31032019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(632543), LD1_31032019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(6009321), LD1_31032019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(607105), LD1_31032019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31032019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31032019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //30.04.2019
        var LD1_30042019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 4, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_30042019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5990035), LD1_30042019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923411), LD1_30042019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-66624), LD1_30042019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(632543), LD1_30042019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(651822), LD1_30042019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-6380), LD1_30042019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(73004), LD1_30042019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30042019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_30042019.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(5923411), LD1_30042019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(651822), LD1_30042019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5990035), LD1_30042019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(632543), LD1_30042019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_30042019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_30042019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.05.2019
        var LD1_31052019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 5, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_31052019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923411), LD1_31052019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923411), LD1_31052019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(651822), LD1_31052019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(679125), LD1_31052019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(680), LD1_31052019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-680), LD1_31052019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31052019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_31052019.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(5923411), LD1_31052019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(679125), LD1_31052019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923411), LD1_31052019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(651822), LD1_31052019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31052019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31052019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //30.06.2019
        var LD1_30062019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(30, 6, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_30062019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923411), LD1_30062019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923411), LD1_30062019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(679125), LD1_30062019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(705656), LD1_30062019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(661), LD1_30062019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-661), LD1_30062019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_30062019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_30062019.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(5923411), LD1_30062019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(705656), LD1_30062019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923411), LD1_30062019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(679125), LD1_30062019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_30062019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_30062019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.07.2019
        var LD1_31072019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 7, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_31072019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923411), LD1_31072019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923411), LD1_31072019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(705656), LD1_31072019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(733182), LD1_31072019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(686), LD1_31072019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-686), LD1_31072019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31072019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_31072019.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(5923411), LD1_31072019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(733182), LD1_31072019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923411), LD1_31072019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(705656), LD1_31072019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31072019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31072019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

        //31.08.2019
        var LD1_31082019 = lec.getCalculatedStornoDeletedEntries().stream()
                .filter(tr -> tr.getEntryID().getPeriod().getDate().isEqual(Builders.getDate(31, 8, 2019).withZoneSameInstant(ZoneId.of("Europe/Moscow"))))
                .filter(tr -> tr.getEntryID().getScenario().getName().equals("FACT"))
                .filter(tr -> tr.getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()).get(0);

        assertEquals(BigDecimal.valueOf(87663), LD1_31082019.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923411), LD1_31082019.getINCOMING_LD_BODY_RUB_REG_LD_3_L().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923411), LD1_31082019.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082019.getREVAL_LD_BODY_PLUS_REG_LD_3_N().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082019.getREVAL_LD_BODY_MINUS_REG_LD_3_O().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(733182), LD1_31082019.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(760823), LD1_31082019.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(689), LD1_31082019.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082019.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(-689), LD1_31082019.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082019.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082019.getDISPOSAL_BODY_RUB_REG_LD_3_X().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO, LD1_31082019.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y().setScale(0, RoundingMode.HALF_UP));
        assertEquals(LeasingDepositDuration.ST, LD1_31082019.getLDTERM_REG_LD_3_Z());
        assertEquals(BigDecimal.valueOf(5923411), LD1_31082019.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(760823), LD1_31082019.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5923411), LD1_31082019.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(733182), LD1_31082019.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31082019.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(5178807), LD1_31082019.getADVANCE_PREVPERIOD_REG_LD_3_AF().setScale(0, RoundingMode.HALF_UP));

    }

    public static void InitializeGeneraldata() {
        User user = getAnyUser();
        fact = Builders.getSC("FACT", ScenarioStornoStatus.ADDITION, user);
        usd = Builders.getCUR("USD");
        C1001 = Builders.getEN("C1001", "Компания-1");
        CP = Builders.getCP("ООО \"Лизинговая компания\"");

        long all = LocalDate.of(2010, 1, 31).datesUntil(LocalDate.of(2030, 12, 31), java.time.Period.ofMonths(1)).count();

        LocalDate.of(2010, 1, 31).datesUntil(LocalDate.of(2030, 12, 31), java.time.Period.ofMonths(1)).forEach((date) ->
                {
                    LocalDate newDate = date.withDayOfMonth(date.lengthOfMonth());

                    periods.put(Builders.getDate(newDate.getDayOfMonth(), newDate.getMonthValue(), newDate.getYear()), Builders.getPer(newDate.getDayOfMonth(), newDate.getMonthValue(), newDate.getYear()));
                }
        );

        dur_0_12_M = Builders.getDur("<= 12 мес.", 0, 12);
        dur_25_36_M = Builders.getDur("25-36 мес.", 25, 36);

        depRateC1001_0_12M = Builders.getDepRate(C1001, Builders.getDate(01, 01, 1970), Builders.getDate(31, 12, 2999), usd, dur_0_12_M, fact, BigDecimal.valueOf(2.0));
        depRateC1001_25_36M = Builders.getDepRate(C1001, Builders.getDate(01, 01, 1970), Builders.getDate(31, 12, 2999), usd, dur_25_36_M, fact, BigDecimal.valueOf(5.0));

        ExR = new ArrayList<>();
        ExR.add(Builders.getExRate(fact, Builders.getDate(10, 3, 2017), usd, BigDecimal.valueOf(58.8318), BigDecimal.ZERO));
        ExR.add(Builders.getExRate(fact, Builders.getDate(31, 3, 2017), usd, BigDecimal.valueOf(56.3779), BigDecimal.valueOf(58.1091)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(30, 4, 2017), usd, BigDecimal.valueOf(56.9838082901554), BigDecimal.valueOf(56.4315074286036)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(31, 5, 2017), usd, BigDecimal.valueOf(56.5168010059989), BigDecimal.valueOf(57.171996848083)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(30, 6, 2017), usd, BigDecimal.valueOf(59.0855029786337), BigDecimal.valueOf(57.8311009199966)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(31, 7, 2017), usd, BigDecimal.valueOf(59.543597652502), BigDecimal.valueOf(59.6707093574817)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(31, 8, 2017), usd, BigDecimal.valueOf(58.7306), BigDecimal.valueOf(59.6497128133555)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(30, 9, 2017), usd, BigDecimal.valueOf(58.0168999895548), BigDecimal.valueOf(57.6953966972068)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(31, 10, 2017), usd, BigDecimal.valueOf(57.8716), BigDecimal.valueOf(57.7305008320361)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(30, 11, 2017), usd, BigDecimal.valueOf(58.3311), BigDecimal.valueOf(58.9212082863353)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(31, 12, 2017), usd, BigDecimal.valueOf(57.6002), BigDecimal.valueOf(58.5887999151509)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(31, 1, 2018), usd, BigDecimal.valueOf(56.2914), BigDecimal.valueOf(56.7874891077606)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(28, 2, 2018), usd, BigDecimal.valueOf(55.6717), BigDecimal.valueOf(56.8124108208847)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(31, 3, 2018), usd, BigDecimal.valueOf(57.2649), BigDecimal.valueOf(57.0343978412931)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(30, 4, 2018), usd, BigDecimal.valueOf(61.9997), BigDecimal.valueOf(60.4623078997034)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(31, 5, 2018), usd, BigDecimal.valueOf(62.5937), BigDecimal.valueOf(62.2090013772315)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(30, 6, 2018), usd, BigDecimal.valueOf(62.7565), BigDecimal.valueOf(62.7143124565438)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(31, 7, 2018), usd, BigDecimal.valueOf(62.7805), BigDecimal.valueOf(62.8828032372803)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(31, 8, 2018), usd, BigDecimal.valueOf(68.0821), BigDecimal.valueOf(66.1231037757643)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(30, 9, 2018), usd, BigDecimal.valueOf(65.5906), BigDecimal.valueOf(67.6597104818259)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(31, 10, 2018), usd, BigDecimal.valueOf(65.7742), BigDecimal.valueOf(65.8868068638933)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(30, 11, 2018), usd, BigDecimal.valueOf(66.6342), BigDecimal.valueOf(65.8868102499607)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(31, 12, 2018), usd, BigDecimal.valueOf(69.4706), BigDecimal.valueOf(65.8867929292929)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(31, 1, 2019), usd, BigDecimal.valueOf(67.5795), BigDecimal.valueOf(65.8868071622573)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(28, 2, 2019), usd, BigDecimal.valueOf(68.5500), BigDecimal.valueOf(65.8867934993621)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(31, 3, 2019), usd, BigDecimal.valueOf(68.3300), BigDecimal.valueOf(65.8867985728187)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(30, 4, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.886789061497)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(31, 5, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8868017917687)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(30, 6, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867890889642)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(31, 7, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867887476067)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(31, 8, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867919915907)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(30, 9, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867896047699)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(31, 10, 2019), usd, BigDecimal.valueOf(67.5699997265878), BigDecimal.valueOf(65.8867901653654)));
        ExR.add(Builders.getExRate(fact, Builders.getDate(30, 11, 2019), usd, BigDecimal.valueOf(64.8114), BigDecimal.valueOf(63.3386)));

        Long i = 1L;
        for (Period p : periods.values()) {
            PeriodsClosedID periodsClosedID = PeriodsClosedID.builder()
                    .scenario(fact)
                    .period(p)
                    .build();

            PeriodsClosed pc = new PeriodsClosed();
            pc.setPeriodsClosedID(periodsClosedID);
            if (p.getDate().isBefore(Builders.getDate(31, 3, 2020))) pc.setISCLOSED(STATUS_X.X);
            i++;
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
        leasingDeposit1.setStart_date(Builders.getDate(10, 3, 2017));
        leasingDeposit1.setScenario(fact);
        leasingDeposit1.setIs_created(STATUS_X.X);

        EndDateID endDateID_31032017_20102019 = EndDateID.builder()
                .leasingDeposit_id(leasingDeposit1.getId())
                .period(periods.get(Builders.getDate(31, 3, 2017)))
                .scenario(fact)
                .build();

        EndDate ed_ld1_31032017_20102019 = new EndDate();
        ed_ld1_31032017_20102019.setEndDateID(endDateID_31032017_20102019);
        ed_ld1_31032017_20102019.setEnd_Date(Builders.getDate(20, 10, 2019));


        EndDateID endDateID_31082017_20122019 = EndDateID.builder()
                .leasingDeposit_id(leasingDeposit1.getId())
                .period(periods.get(Builders.getDate(31, 8, 2017)))
                .scenario(fact)
                .build();

        EndDate ed_ld1_31082017_20122019 = new EndDate();
        ed_ld1_31082017_20122019.setEndDateID(endDateID_31082017_20122019);
        ed_ld1_31082017_20122019.setEnd_Date(Builders.getDate(20, 12, 2019));

        EndDateID endDateID_31102017_20112019 = EndDateID.builder()
                .leasingDeposit_id(leasingDeposit1.getId())
                .period(periods.get(Builders.getDate(31, 10, 2017)))
                .scenario(fact)
                .build();

        EndDate ed_ld1_31102017_20112019 = new EndDate();
        ed_ld1_31102017_20112019.setEndDateID(endDateID_31102017_20112019);
        ed_ld1_31102017_20112019.setEnd_Date(Builders.getDate(20, 11, 2019));

        EndDateID endDateID_30112019_03112019 = EndDateID.builder()
                .leasingDeposit_id(leasingDeposit1.getId())
                .period(periods.get(Builders.getDate(30, 11, 2019)))
                .scenario(fact)
                .build();

        EndDate ed_ld1_30112019_03112019 = new EndDate();
        ed_ld1_30112019_03112019.setEndDateID(endDateID_30112019_03112019);
        ed_ld1_30112019_03112019.setEnd_Date(Builders.getDate(03, 11, 2019));

        leasingDeposit1.setEnd_dates(Set.of(ed_ld1_31032017_20102019, ed_ld1_31082017_20122019, ed_ld1_31102017_20112019, ed_ld1_30112019_03112019));
    }

}

