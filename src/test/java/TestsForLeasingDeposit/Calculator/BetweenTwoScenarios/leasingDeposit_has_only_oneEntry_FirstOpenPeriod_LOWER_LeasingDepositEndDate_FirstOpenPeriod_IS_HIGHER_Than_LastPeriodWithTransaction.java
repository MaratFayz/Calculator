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
import LD.model.Enums.*;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static TestsForLeasingDeposit.Calculator.Builders.getAnyUser;
import static TestsForLeasingDeposit.Calculator.Builders.getDate;
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
public class leasingDeposit_has_only_oneEntry_FirstOpenPeriod_LOWER_LeasingDepositEndDate_FirstOpenPeriod_IS_HIGHER_Than_LastPeriodWithTransaction {

    Scenario fact, plan2020;
    Currency usd;
    Company C1001;
    Counterpartner CP;
    HashMap<ZonedDateTime, Period> periods = new HashMap<>();
    Duration dur_0_12_M;
    Duration dur_25_36_M;
    DepositRate depRateC1001_0_12M;
    DepositRate depRateC1001_25_36M;
    List<ExchangeRate> ExR;

    List<LeasingDeposit> LeasingDeposits = new LinkedList<>();
    LeasingDeposit leasingDeposit1;
    EntryCalculator lec;

    @Mock
    GeneralDataKeeper GDK;
    @Mock
    DepositRatesRepository depositRatesRepository;

    final String SCENARIO_LOAD = "FACT";
    final String SCENARIO_SAVE = "FACT";
    List<Entry> calculatedEntries = new ArrayList<>();
    ExecutorService threadExecutor;

    @BeforeEach
    public void setUp() throws ExecutionException, InterruptedException {
        InitializeGeneraldata();
        create_LD_1_NormalTestLD();

        Mockito.lenient().when(GDK.getLeasingDeposits()).thenReturn(List.of(leasingDeposit1));
        Mockito.lenient().when(GDK.getTo()).thenReturn(plan2020);
        Mockito.lenient().when(GDK.getFrom()).thenReturn(fact);
        Mockito.lenient().when(GDK.getAllExRates()).thenReturn(ExR);
        Mockito.lenient().when(GDK.getAllPeriods()).thenReturn(List.copyOf(periods.values()));

        threadExecutor = Executors.newFixedThreadPool(10);

        lec = new EntryCalculator(leasingDeposit1, GDK, depositRatesRepository);
        Specification<DepositRate> dr = lec.getDepRateForLD(leasingDeposit1, lec.getLDdurationMonths());
        Mockito.when(depositRatesRepository.findAll(Mockito.any(dr.getClass()))).thenReturn(List.of(depRateC1001_25_36M));
    }

    @Test
    public void test1_IllegalArgument_FirstOpenPeriod_and_PeriodLastTransaction_are_NOT_EQUAL_OR_NOT_FOP_greater_one_period_than_PLT() throws ExecutionException, InterruptedException {
        Mockito.when(GDK.getFirstOpenPeriod_ScenarioFrom()).thenReturn(getDate(31, 3, 2020));
        Mockito.when(GDK.getFirstOpenPeriod_ScenarioTo()).thenReturn(getDate(31, 3, 2020));

        Throwable e = Assertions.assertThrows(ExecutionException.class, () -> {
            Future<List<Entry>> entries = threadExecutor.submit(lec);
            calculatedEntries.addAll(entries.get());

            threadExecutor.shutdown();
        });

        assertEquals(e.getCause().getMessage(), "Транзакции лизингового депозита не соответствуют закрытому периоду: " +
                "период последней рассчитанной транзакции должен быть или равен первому открытому периоду или должен быть меньше строго на один период");
    }

    @Test
    public void test2_NoException_IF_FirstOpenPeriod_and_PeriodLastTransaction_are_EQUAL() throws ExecutionException, InterruptedException {
        Mockito.when(GDK.getFirstOpenPeriod_ScenarioFrom()).thenReturn(getDate(31, 3, 2017));
        Mockito.when(GDK.getFirstOpenPeriod_ScenarioTo()).thenReturn(getDate(31, 3, 2017));
        Mockito.when(GDK.getPeriod_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo()).thenReturn(null);

        Assertions.assertDoesNotThrow(() -> {
            Future<List<Entry>> entries = threadExecutor.submit(lec);
            calculatedEntries.addAll(entries.get());

            threadExecutor.shutdown();
        });
    }

    @Test
    public void test3_NoException_IF_FirstOpenPeriod_greater_one_period_than_PeriodLastTransaction() throws ExecutionException, InterruptedException {
        Mockito.when(GDK.getFirstOpenPeriod_ScenarioFrom()).thenReturn(getDate(30, 4, 2017));
        Mockito.when(GDK.getFirstOpenPeriod_ScenarioTo()).thenReturn(getDate(30, 4, 2017));

        Assertions.assertDoesNotThrow(() -> {
            Future<List<Entry>> entries = threadExecutor.submit(lec);
            calculatedEntries.addAll(entries.get());

            threadExecutor.shutdown();
        });

    }

    public void InitializeGeneraldata() {
        User user = getAnyUser();
        fact = Builders.getSC("FACT", ScenarioStornoStatus.ADDITION, user);
        plan2020 = Builders.getSC("PLAN2020", ScenarioStornoStatus.FULL, user);
        usd = Builders.getCUR("USD");
        C1001 = Builders.getEN("C1001", "Компания-1");
        CP = Builders.getCP("ООО \"Лизинговая компания\"");

        long all = LocalDate.of(2010, 1, 31).datesUntil(LocalDate.of(2030, 12, 31), java.time.Period.ofMonths(1)).count();

        LocalDate.of(2010, 1, 31).datesUntil(LocalDate.of(2030, 12, 31), java.time.Period.ofMonths(1)).forEach((date) ->
                {
                    LocalDate newDate = date.withDayOfMonth(date.lengthOfMonth());

                    periods.put(getDate(newDate.getDayOfMonth(), newDate.getMonthValue(), newDate.getYear()), Builders.getPer(newDate.getDayOfMonth(), newDate.getMonthValue(), newDate.getYear()));
                }
        );

        dur_0_12_M = Builders.getDur("<= 12 мес.", 0, 12);
        dur_25_36_M = Builders.getDur("25-36 мес.", 25, 36);

        depRateC1001_0_12M = Builders.getDepRate(C1001, getDate(01, 01, 1970), getDate(31, 12, 2999), usd, dur_0_12_M, fact, BigDecimal.valueOf(2.0));
        depRateC1001_25_36M = Builders.getDepRate(C1001, getDate(01, 01, 1970), getDate(31, 12, 2999), usd, dur_25_36_M, fact, BigDecimal.valueOf(5.0));

        ExR = new ArrayList<>();
        ExR.add(Builders.getExRate(fact, getDate(10, 3, 2017), usd, BigDecimal.valueOf(58.8318), BigDecimal.ZERO));
        ExR.add(Builders.getExRate(fact, getDate(31, 3, 2017), usd, BigDecimal.valueOf(56.3779), BigDecimal.valueOf(58.1091)));

        ExR.add(Builders.getExRate(plan2020, getDate(31, 3, 2017), usd, BigDecimal.valueOf(56.3779), BigDecimal.valueOf(58.1091)));

        ExR.add(Builders.getExRate(fact, getDate(30, 4, 2017), usd, BigDecimal.valueOf(56.9838082901554), BigDecimal.valueOf(56.4315074286036)));

        ExR.add(Builders.getExRate(plan2020, getDate(30, 4, 2017), usd, BigDecimal.valueOf(56.9838082901554), BigDecimal.valueOf(56.4315074286036)));

        ExR.add(Builders.getExRate(fact, getDate(31, 5, 2017), usd, BigDecimal.valueOf(56.5168010059989), BigDecimal.valueOf(57.171996848083)));
        ExR.add(Builders.getExRate(fact, getDate(30, 6, 2017), usd, BigDecimal.valueOf(59.0855029786337), BigDecimal.valueOf(57.8311009199966)));
        ExR.add(Builders.getExRate(fact, getDate(31, 7, 2017), usd, BigDecimal.valueOf(59.543597652502), BigDecimal.valueOf(59.6707093574817)));
        ExR.add(Builders.getExRate(fact, getDate(31, 8, 2017), usd, BigDecimal.valueOf(58.7306), BigDecimal.valueOf(59.6497128133555)));
        ExR.add(Builders.getExRate(fact, getDate(30, 9, 2017), usd, BigDecimal.valueOf(58.0168999895548), BigDecimal.valueOf(57.6953966972068)));
        ExR.add(Builders.getExRate(fact, getDate(31, 10, 2017), usd, BigDecimal.valueOf(57.8716), BigDecimal.valueOf(57.7305008320361)));
        ExR.add(Builders.getExRate(fact, getDate(30, 11, 2017), usd, BigDecimal.valueOf(58.3311), BigDecimal.valueOf(58.9212082863353)));
        ExR.add(Builders.getExRate(fact, getDate(31, 12, 2017), usd, BigDecimal.valueOf(57.6002), BigDecimal.valueOf(58.5887999151509)));
        ExR.add(Builders.getExRate(fact, getDate(31, 1, 2018), usd, BigDecimal.valueOf(56.2914), BigDecimal.valueOf(56.7874891077606)));
        ExR.add(Builders.getExRate(fact, getDate(28, 2, 2018), usd, BigDecimal.valueOf(55.6717), BigDecimal.valueOf(56.8124108208847)));
        ExR.add(Builders.getExRate(fact, getDate(31, 3, 2018), usd, BigDecimal.valueOf(57.2649), BigDecimal.valueOf(57.0343978412931)));
        ExR.add(Builders.getExRate(fact, getDate(30, 4, 2018), usd, BigDecimal.valueOf(61.9997), BigDecimal.valueOf(60.4623078997034)));
        ExR.add(Builders.getExRate(fact, getDate(31, 5, 2018), usd, BigDecimal.valueOf(62.5937), BigDecimal.valueOf(62.2090013772315)));
        ExR.add(Builders.getExRate(fact, getDate(30, 6, 2018), usd, BigDecimal.valueOf(62.7565), BigDecimal.valueOf(62.7143124565438)));
        ExR.add(Builders.getExRate(fact, getDate(31, 7, 2018), usd, BigDecimal.valueOf(62.7805), BigDecimal.valueOf(62.8828032372803)));
        ExR.add(Builders.getExRate(fact, getDate(31, 8, 2018), usd, BigDecimal.valueOf(68.0821), BigDecimal.valueOf(66.1231037757643)));
        ExR.add(Builders.getExRate(fact, getDate(30, 9, 2018), usd, BigDecimal.valueOf(65.5906), BigDecimal.valueOf(67.6597104818259)));
        ExR.add(Builders.getExRate(fact, getDate(31, 10, 2018), usd, BigDecimal.valueOf(65.7742), BigDecimal.valueOf(65.8868068638933)));
        ExR.add(Builders.getExRate(fact, getDate(30, 11, 2018), usd, BigDecimal.valueOf(66.6342), BigDecimal.valueOf(65.8868102499607)));
        ExR.add(Builders.getExRate(fact, getDate(31, 12, 2018), usd, BigDecimal.valueOf(69.4706), BigDecimal.valueOf(65.8867929292929)));
        ExR.add(Builders.getExRate(fact, getDate(31, 1, 2019), usd, BigDecimal.valueOf(67.5795), BigDecimal.valueOf(65.8868071622573)));
        ExR.add(Builders.getExRate(fact, getDate(28, 2, 2019), usd, BigDecimal.valueOf(68.5500), BigDecimal.valueOf(65.8867934993621)));
        ExR.add(Builders.getExRate(fact, getDate(31, 3, 2019), usd, BigDecimal.valueOf(68.3300), BigDecimal.valueOf(65.8867985728187)));
        ExR.add(Builders.getExRate(fact, getDate(30, 4, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.886789061497)));
        ExR.add(Builders.getExRate(fact, getDate(31, 5, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8868017917687)));
        ExR.add(Builders.getExRate(fact, getDate(30, 6, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867890889642)));
        ExR.add(Builders.getExRate(fact, getDate(31, 7, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867887476067)));
        ExR.add(Builders.getExRate(fact, getDate(31, 8, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867919915907)));
        ExR.add(Builders.getExRate(fact, getDate(30, 9, 2019), usd, BigDecimal.valueOf(67.5700), BigDecimal.valueOf(65.8867896047699)));
        ExR.add(Builders.getExRate(fact, getDate(31, 10, 2019), usd, BigDecimal.valueOf(67.5699997265878), BigDecimal.valueOf(65.8867901653654)));
        ExR.add(Builders.getExRate(fact, getDate(30, 11, 2019), usd, BigDecimal.valueOf(64.8114), BigDecimal.valueOf(63.3386)));

        Long i = 1L;
        for (Period p : periods.values()) {
            PeriodsClosedID periodsClosedID = PeriodsClosedID.builder()
                    .scenario(fact)
                    .period(p)
                    .build();

            PeriodsClosed pc = new PeriodsClosed();
            pc.setPeriodsClosedID(periodsClosedID);
            if (p.getDate().isBefore(getDate(31, 3, 2020))) pc.setISCLOSED(STATUS_X.X);
            i++;
        }

    }

    public void create_LD_1_NormalTestLD() {
        //Депозит только для факта 1
        leasingDeposit1 = new LeasingDeposit();
        leasingDeposit1.setId(1L);
        leasingDeposit1.setCounterpartner(CP);
        leasingDeposit1.setCompany(C1001);
        leasingDeposit1.setCurrency(usd);
        leasingDeposit1.setDeposit_sum_not_disc(BigDecimal.valueOf(100000.00));
        leasingDeposit1.setStart_date(getDate(10, 3, 2017));
        leasingDeposit1.setScenario(fact);
        leasingDeposit1.setIs_created(STATUS_X.X);

        EndDateID endDateID_31032017_20102019 = EndDateID.builder()
                .leasingDeposit_id(leasingDeposit1.getId())
                .period(periods.get(getDate(31, 3, 2017)))
                .scenario(fact)
                .build();

        EndDate ed_ld1_31032017_20102019 = new EndDate();
        ed_ld1_31032017_20102019.setEndDateID(endDateID_31032017_20102019);
        ed_ld1_31032017_20102019.setEnd_Date(getDate(20, 10, 2019));


        EndDateID endDateID_31082017_20122019 = EndDateID.builder()
                .leasingDeposit_id(leasingDeposit1.getId())
                .period(periods.get(getDate(31, 8, 2017)))
                .scenario(fact)
                .build();

        EndDate ed_ld1_31082017_20122019 = new EndDate();
        ed_ld1_31082017_20122019.setEndDateID(endDateID_31082017_20122019);
        ed_ld1_31082017_20122019.setEnd_Date(getDate(20, 12, 2019));

        EndDateID endDateID_31102017_20112019 = EndDateID.builder()
                .leasingDeposit_id(leasingDeposit1.getId())
                .period(periods.get(getDate(31, 10, 2017)))
                .scenario(fact)
                .build();

        EndDate ed_ld1_31102017_20112019 = new EndDate();
        ed_ld1_31102017_20112019.setEndDateID(endDateID_31102017_20112019);
        ed_ld1_31102017_20112019.setEnd_Date(getDate(20, 11, 2019));

        EndDateID endDateID_30112019_03112019 = EndDateID.builder()
                .leasingDeposit_id(leasingDeposit1.getId())
                .period(periods.get(getDate(30, 11, 2019)))
                .scenario(fact)
                .build();

        EndDate ed_ld1_30112019_03112019 = new EndDate();
        ed_ld1_30112019_03112019.setEndDateID(endDateID_30112019_03112019);
        ed_ld1_30112019_03112019.setEnd_Date(getDate(03, 11, 2019));

        leasingDeposit1.setEnd_dates(Set.of(ed_ld1_31032017_20102019, ed_ld1_31082017_20122019, ed_ld1_31102017_20112019, ed_ld1_30112019_03112019));

        //транзакция за период 31.03.2017
        EntryID entryID = EntryID.builder()
                .leasingDeposit_id(leasingDeposit1.getId())
                .CALCULATION_TIME(ZonedDateTime.now())
                .scenario(fact)
                .period(periods.get(getDate(31, 3, 2017)))
                .build();

        Entry entry = Entry.builder()
                .entryID(entryID)
                .end_date_at_this_period(getDate(20, 10, 2019))
                .status(EntryStatus.ACTUAL)
                .Status_EntryMadeDuringOrAfterClosedPeriod(EntryPeriodCreation.AFTER_CLOSING_PERIOD)
                .DISCONT_AT_START_DATE_cur_REG_LD_1_K(BigDecimal.valueOf(-11973))
                .DISCONT_AT_START_DATE_RUB_REG_LD_1_L(BigDecimal.valueOf(-704373))
                .DISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M(BigDecimal.valueOf(-704373))
                .deposit_sum_not_disc_RUB_REG_LD_1_N(BigDecimal.valueOf(5883180))
                .DISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P(BigDecimal.ZERO)
                .DISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q(BigDecimal.ZERO)
                .DISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(BigDecimal.ZERO)
                .REVAL_CORR_DISC_rub_REG_LD_1_S(BigDecimal.ZERO)
                .CORR_ACC_AMORT_DISC_rub_REG_LD_1_T(BigDecimal.ZERO)
                .CORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X(BigDecimal.ZERO)
                .CORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U(BigDecimal.ZERO)
                .CORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V(BigDecimal.ZERO)
                .CORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W(BigDecimal.ZERO)
                .ACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H(BigDecimal.ZERO)
                .AMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I(BigDecimal.valueOf(247))
                .ACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(BigDecimal.valueOf(247))
                .ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(BigDecimal.ZERO)
                .AMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M(BigDecimal.valueOf(14379))
                .ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N(BigDecimal.valueOf(14379))
                .discountedSum_at_current_end_date_cur_REG_LD_3_G(BigDecimal.valueOf(88027))
                .INCOMING_LD_BODY_RUB_REG_LD_3_L(BigDecimal.valueOf(5178807))
                .OUTCOMING_LD_BODY_REG_LD_3_M(BigDecimal.valueOf(4962796))
                .REVAL_LD_BODY_PLUS_REG_LD_3_N(BigDecimal.ZERO)
                .REVAL_LD_BODY_MINUS_REG_LD_3_O(BigDecimal.valueOf(-216010))
                .ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R(BigDecimal.ZERO)
                .ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S(BigDecimal.valueOf(13951))
                .REVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T(BigDecimal.ZERO)
                .REVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U(BigDecimal.valueOf(-428))
                .SUM_PLUS_FOREX_DIFF_REG_LD_3_V(BigDecimal.ZERO)
                .SUM_MINUS_FOREX_DIFF_REG_LD_3_W(BigDecimal.valueOf(216439))
                .DISPOSAL_BODY_RUB_REG_LD_3_X(BigDecimal.ZERO)
                .DISPOSAL_DISCONT_RUB_REG_LD_3_Y(BigDecimal.ZERO)
                .LDTERM_REG_LD_3_Z(LeasingDepositDuration.LT)
                .TERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA(BigDecimal.ZERO)
                .TERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB(BigDecimal.ZERO)
                .TERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC(BigDecimal.ZERO)
                .TERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD(BigDecimal.ZERO)
                .ADVANCE_CURRENTPERIOD_REG_LD_3_AE(BigDecimal.ZERO)
                .ADVANCE_PREVPERIOD_REG_LD_3_AF(BigDecimal.ZERO)
                .build();

        leasingDeposit1.setEntries(Set.of(entry));

    }

}

