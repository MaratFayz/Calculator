package Utils;

import LD.config.DateFormat;
import LD.config.Security.model.User.User;
import LD.model.Company.Company;
import LD.model.Counterpartner.Counterpartner;
import LD.model.Currency.Currency;
import LD.model.DepositRate.DepositRate;
import LD.model.DepositRate.DepositRateID;
import LD.model.Duration.Duration;
import LD.model.EndDate.EndDate;
import LD.model.EndDate.EndDateID;
import LD.model.Entry.Entry;
import LD.model.Entry.EntryID;
import LD.model.EntryIFRSAcc.EntryIFRSAcc;
import LD.model.EntryIFRSAcc.EntryIFRSAccID;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.ExchangeRate.ExchangeRate;
import LD.model.ExchangeRate.ExchangeRateID;
import LD.model.IFRSAccount.IFRSAccount;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.Period.Period;
import LD.model.Scenario.Scenario;
import Utils.Entities.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Data
public class TestEntitiesKeeper {

    TestDataKeeper testDataKeeper;
    User user;
    Company company;
    List<Scenario> scenarios = new ArrayList<>();
    List<Currency> currencies = new ArrayList<>();
    Counterpartner counterpartner;
    LocalDate periods_start;
    LocalDate periods_end;
    LocalDate firstOpenPeriodScenarioFrom;
    LocalDate firstOpenPeriodScenarioTo;
    LocalDate periodInScenarioFromForCopyingEntriesToScenarioTo;
    List<Period> periods = new ArrayList<>();
    List<Duration> durations = new ArrayList<>();
    List<DepositRate> depositRates = new ArrayList<>();
    List<LeasingDeposit> leasingDeposits = new ArrayList<>();
    List<EndDate> endDates = new ArrayList<>();
    List<ExchangeRate> exRates = new ArrayList<>();
    List<Entry> entries_into_leasingDeposit = new ArrayList<>();
    List<Entry> entries_expected = new ArrayList<>();
    List<Entry> entriesForIfrsSumDaoTest = new ArrayList<>();
    Entry entryForEntryIfrsCalculation;
    List<EntryIFRSAcc> entriesIfrsExcepted = new ArrayList<>();
    List<EntryIFRSAcc> entriesIfrsForIfrsSumDaoTests = new ArrayList<>();
    List<IFRSAccount> ifrsAccounts = new ArrayList<>();

    public static TestEntitiesKeeper transformDataKeeperIntoEntitiesKeeper(TestDataKeeper testDataKeeper) {
        return new TestEntitiesKeeper(testDataKeeper);
    }

    private TestEntitiesKeeper(TestDataKeeper testDataKeeper) {
        this.testDataKeeper = testDataKeeper;
        transformDataIntoEntities();
    }

    private void transformDataIntoEntities() {
        createUser();
        createCompany();
        createScenarios();
        createCurrencies();
        createCounterpartner();
        createPeriods();
        createFirstOpenPeriodForScenarioFrom();
        createFirstOpenPeriodForScenarioTo();
        createPeriodInScenarioFromForCopyingEntriesToScenarioTo();
        createDurations();
        createDepositRates();
        createEndDates();
        createEntriesIntoLeasingDeposits();
        createLeasingDeposits();
        addEntriesIntoLeasingDeposits();
        addEndDatesIntoLeasingDeposits();
        createExpectedEntries();
        pasteLeasingDepositIntoExpectedEntries();
        createExchangeRates();
        createEntryForCalculationEntryIfrs();
        createEntriesForEntryIfrsSumDaoTests();
        createIfrsAccounts();
        createEntriesIfrsAccounts();
        createEntriesIfrsAccountsForDaoTests();
    }

    private void createUser() {
        user = toUser(testDataKeeper.getUser());
    }

    private User toUser(UserTestData testUserToUser) {
        User user = User.builder().id(testUserToUser.getId())
                .username(testUserToUser.getUsername())
                .password(testUserToUser.getPassword())
                .build();

        user.setLastChange(DateFormat.parsingZonedDateTime(testUserToUser.getLastChange()));

        return user;
    }

    private void createCompany() {
        if (nonNull(testDataKeeper.getCompany())) {
            company = toCompany(testDataKeeper.getCompany());
        }
    }

    private Company toCompany(CompanyTestData testCompanyToCompany) {
        Company company = Company.builder().id(testCompanyToCompany.getId())
                .name(testCompanyToCompany.getName())
                .code(testCompanyToCompany.getCode())
                .build();

        company.setLastChange(DateFormat.parsingZonedDateTime(testCompanyToCompany.getLastChange()));
        company.setUser(this.user.getId().equals(testCompanyToCompany.getUserCode()) ? user : null);
        return company;
    }

    private void createScenarios() {
        if (nonNull(testDataKeeper.getScenarios())) {
            testDataKeeper.getScenarios().forEach(sc -> scenarios.add(toScenario(sc)));
        }
    }

    private Scenario toScenario(ScenarioTestData testScenarioToScenario) {
        Scenario sc = Scenario.builder().id(testScenarioToScenario.getId())
                .name(testScenarioToScenario.getName())
                .status(ScenarioStornoStatus.valueOf(testScenarioToScenario.getStatus()))
                .build();

        sc.setLastChange(DateFormat.parsingZonedDateTime(testScenarioToScenario.getLastChange()));
        sc.setUser(this.user.getId().equals(testScenarioToScenario.getUserCode()) ? user : null);

        return sc;
    }

    private void createCurrencies() {
        if (nonNull(testDataKeeper.getCurrencies())) {
            testDataKeeper.getCurrencies().forEach(cu -> currencies.add(toCurrency(cu)));
        }
    }

    private Currency toCurrency(CurrencyTestData testCurrencyToCurrency) {
        Currency cur = Currency.builder().id(testCurrencyToCurrency.getId())
                .name(testCurrencyToCurrency.getName())
                .short_name(testCurrencyToCurrency.getShort_name())
                .build();

        cur.setLastChange(DateFormat.parsingZonedDateTime(testCurrencyToCurrency.getLastChange()));
        cur.setUser(this.user.getId().equals(testCurrencyToCurrency.getUserCode()) ? user : null);
        return cur;
    }

    private void createCounterpartner() {
        if (nonNull(testDataKeeper.getCounterpartner())) {
            counterpartner = toCounterpartner(testDataKeeper.getCounterpartner());
        }
    }

    private Counterpartner toCounterpartner(CounterpartnerTestData testCPtoCP) {
        Counterpartner c = Counterpartner.builder().id(testCPtoCP.getId())
                .name(testCPtoCP.getName())
                .build();

        c.setLastChange(DateFormat.parsingZonedDateTime(testCPtoCP.getLastChange()));
        c.setUser(this.user.getId().equals(testCPtoCP.getUserCode()) ? user : null);
        return c;
    }

    private void createPeriods() {
        if (nonNull(testDataKeeper.getPeriods_start()) && nonNull(testDataKeeper.getPeriods_end())) {
            periods_start = toLocalDate(testDataKeeper.getPeriods_start());
            periods_end = toLocalDate(testDataKeeper.getPeriods_end());

            if (periods_start.plusDays(1).isBefore(periods_end) || periods_start.plusDays(1).isEqual(periods_end)) {
                periods_start.datesUntil(periods_end, java.time.Period.ofMonths(1)).collect(Collectors.toList()).forEach(date -> {
                    LocalDate d = date.plusMonths(1).withDayOfMonth(1).minusDays(1);

                    Period period = Period.builder().date(d).build();
                    periods.add(period);
                });
            } else {
                throw new IllegalStateException("Error: periods_start is greater than periods_end!");
            }
        }

    }

    private void createFirstOpenPeriodForScenarioFrom() {
        if (nonNull(testDataKeeper.getFirstOpenPeriodScenarioFrom())) {
            firstOpenPeriodScenarioFrom = DateFormat.parsingDate(testDataKeeper.getFirstOpenPeriodScenarioFrom());
        }
    }

    private void createFirstOpenPeriodForScenarioTo() {
        if (nonNull(testDataKeeper.getFirstOpenPeriodScenarioTo())) {
            firstOpenPeriodScenarioTo = DateFormat.parsingDate(testDataKeeper.getFirstOpenPeriodScenarioTo());
        }
    }

    private void createPeriodInScenarioFromForCopyingEntriesToScenarioTo() {
        if (nonNull(testDataKeeper.getPeriodInScenarioFromForCopyingEntriesToScenarioTo())) {
            periodInScenarioFromForCopyingEntriesToScenarioTo =
                    DateFormat.parsingDate(testDataKeeper.getPeriodInScenarioFromForCopyingEntriesToScenarioTo());
        }
    }

    private LocalDate toLocalDate(String dateToTransform) {
        return DateFormat.parsingDate(dateToTransform);
    }

    private void createDurations() {
        if (nonNull(testDataKeeper.getDurations())) {
            testDataKeeper.getDurations().forEach(d -> durations.add(toDuration(d)));
        }
    }

    private Duration toDuration(DurationTestData testDurationToDuration) {
        Duration d = Duration.builder().id(testDurationToDuration.getId())
                .name(testDurationToDuration.getName())
                .MIN_MONTH(testDurationToDuration.getMin_MONTH())
                .MAX_MONTH(testDurationToDuration.getMax_MONTH())
                .build();

        d.setLastChange(DateFormat.parsingZonedDateTime(testDurationToDuration.getLastChange()));
        d.setUser(this.user.getId().equals(testDurationToDuration.getUserCode()) ? user : null);
        return d;
    }

    private void createDepositRates() {
        if (nonNull(testDataKeeper.getDepositRates())) {
            testDataKeeper.getDepositRates().forEach(dr -> depositRates.add(toDepositRates(dr)));
        }
    }

    private DepositRate toDepositRates(DepositRateTestData testDepositRateToDepositRate) {
        LocalDate end_period = DateFormat.parsingDate(testDepositRateToDepositRate.getEnd_PERIOD());
        LocalDate start_period = DateFormat.parsingDate(testDepositRateToDepositRate.getStart_PERIOD());

        if (start_period.isAfter(end_period)) {
            throw new IllegalStateException("ОШИБКА! Дата начала действия ставки депозита позже, чем дата начала");
        }

        DepositRateID depositRatesID = DepositRateID.builder()
                .company(this.company.getId().equals(testDepositRateToDepositRate.getCompanyCode()) ? company : null)
                .currency(this.currencies.stream().filter(c -> c.getId().equals(testDepositRateToDepositRate.getCurrencyCode())).collect(Collectors.toList()).get(0))
                .duration(this.durations.stream().filter(d -> d.getId().equals(testDepositRateToDepositRate.getDurationCode())).collect(Collectors.toList()).get(0))
                .scenario(this.scenarios.stream().filter(s -> s.getId().equals(testDepositRateToDepositRate.getScenarioCode())).collect(Collectors.toList()).get(0))
                .START_PERIOD(start_period)
                .END_PERIOD(end_period)
                .build();

        DepositRate dr = DepositRate.builder().depositRateID(depositRatesID)
                .RATE(testDepositRateToDepositRate.getRate())
                .build();

        dr.setLastChange(DateFormat.parsingZonedDateTime(testDepositRateToDepositRate.getLastChange()));
        dr.setUser(this.user.getId().equals(testDepositRateToDepositRate.getUserCode()) ? user : null);
        return dr;
    }

    private void createEndDates() {
        if (nonNull(testDataKeeper.getEnd_dates())) {
            testDataKeeper.getEnd_dates().forEach(ed -> endDates.add(toEndDates(ed)));
        }
    }

    private EndDate toEndDates(EndDateTestData testEndDateToEndDate) {
        EndDateID endDateID = EndDateID.builder()
                .leasingDeposit_id(testEndDateToEndDate.getLeasingDepositCode())
                .period(this.periods.stream().filter(p -> p.getDate().isEqual(DateFormat.parsingDate(testEndDateToEndDate.getPeriod()))).collect(Collectors.toList()).get(0))
                .scenario(this.scenarios.stream().filter(s -> s.getId().equals(testEndDateToEndDate.getScenarioCode())).collect(Collectors.toList()).get(0))
                .build();

        EndDate endDate = EndDate.builder()
                .endDate(DateFormat.parsingDate(testEndDateToEndDate.getEnd_Date()))
                .endDateID(endDateID)
                .build();

        endDate.setLastChange(DateFormat.parsingZonedDateTime(testEndDateToEndDate.getLastChange()));
        endDate.setUser(this.user.getId().equals(testEndDateToEndDate.getUserCode()) ? user : null);
        return endDate;
    }

    private void createEntriesIntoLeasingDeposits() {
        if (nonNull(testDataKeeper.getEntries_into_leasingDeposit())) {
            testDataKeeper.getEntries_into_leasingDeposit().forEach(e -> entries_into_leasingDeposit.add(toEntry(e)));
        }
    }

    private void createLeasingDeposits() {
        if (nonNull(testDataKeeper.getLeasingDeposits())) {
            testDataKeeper.getLeasingDeposits().forEach(ld -> leasingDeposits.add(toLeasingDeposits(ld)));
        }
    }

    private LeasingDeposit toLeasingDeposits(LeasingDepositTestData testLeasingDepositToLeasingDeposit) {
        LeasingDeposit ld = LeasingDeposit.builder().id(testLeasingDepositToLeasingDeposit.getId())
                .company(this.company.getId().equals(testLeasingDepositToLeasingDeposit.getCompanyCode()) ? company : null)
                .counterpartner(this.counterpartner.getId().equals(testLeasingDepositToLeasingDeposit.getCounterpartnerCode()) ? counterpartner : null)
                .currency(this.currencies.stream().filter(c -> c.getId().equals(testLeasingDepositToLeasingDeposit.getCurrencyCode())).collect(Collectors.toList()).get(0))
                .start_date(DateFormat.parsingDate(testLeasingDepositToLeasingDeposit.getStart_date()))
                .deposit_sum_not_disc(testLeasingDepositToLeasingDeposit.getDeposit_sum_not_disc())
                .scenario(this.scenarios.stream().filter(s -> s.getId().equals(testLeasingDepositToLeasingDeposit.getScenarioCode())).collect(Collectors.toList()).get(0))
                .is_created(testLeasingDepositToLeasingDeposit.getIs_created())
                .is_deleted(testLeasingDepositToLeasingDeposit.getIs_deleted())
                .end_dates(this.endDates.stream().filter(ed -> ed.getEndDateID().getLeasingDeposit_id().equals(testLeasingDepositToLeasingDeposit.getId())).collect(Collectors.toSet()))
                .entries(new HashSet<>())
                .build();

        ld.setLastChange(DateFormat.parsingZonedDateTime(testLeasingDepositToLeasingDeposit.getLastChange()));
        ld.setUser(this.user.getId().equals(testLeasingDepositToLeasingDeposit.getUserCode()) ? user : null);
        return ld;
    }

    private void addEntriesIntoLeasingDeposits() {
        leasingDeposits.forEach(ld -> {
            entries_into_leasingDeposit.stream().filter(e -> e.getEntryID().getLeasingDeposit_id() == ld.getId())
                    .forEach(e -> {
                        ld.getEntries().add(e);
                        e.setLeasingDeposit(ld);
                    });
        });
    }

    private void addEndDatesIntoLeasingDeposits() {
        leasingDeposits.forEach(ld -> {
            endDates.stream().filter(e -> e.getEndDateID().getLeasingDeposit_id() == ld.getId())
                    .forEach(e -> ld.getEnd_dates().add(e));
        });
    }

    private void createExpectedEntries() {
        if (nonNull(testDataKeeper.getEntries_expected())) {
            testDataKeeper.getEntries_expected().forEach(e -> entries_expected.add(toEntry(e)));
        }
    }

    private Entry toEntry(EntryTestData testEntryToEntry) {
        if (isNull(testEntryToEntry)) {
            return null;
        }

        EntryID entryID = EntryID.builder()
                .CALCULATION_TIME(DateFormat.parsingZonedDateTime(testEntryToEntry.getCalculation_time()))
                .scenario(this.scenarios.stream().filter(s -> s.getId().equals(testEntryToEntry.getScenarioCode())).collect(Collectors.toList()).get(0))
                .leasingDeposit_id(testEntryToEntry.getLeasingDepositCode())
                .period(this.periods.stream().filter(p -> p.getDate().isEqual(DateFormat.parsingDate(testEntryToEntry.getPeriod()))).collect(Collectors.toList()).get(0))
                .build();

        return Entry.builder()
                .entryID(entryID)
                .leasingDeposit(this.leasingDeposits.stream().filter(s -> s.getId().equals(testEntryToEntry.getLeasingDepositCode())).collect(Collectors.toList()).get(0))
                .status(testEntryToEntry.getStatus())
                .end_date_at_this_period(DateFormat.parsingDate(testEntryToEntry.getEnd_date_at_this_period()))
                .Status_EntryMadeDuringOrAfterClosedPeriod(testEntryToEntry.getStatus_EntryMadeDuringOrAfterClosedPeriod())
                .percentRateForPeriodForLD(testEntryToEntry.getPercentRateForPeriodForLD())
                .DISCONT_AT_START_DATE_cur_REG_LD_1_K(testEntryToEntry.getDiscont_at_start_date_cur_reg_ld_1_k())
                .DISCONT_AT_START_DATE_RUB_REG_LD_1_L(testEntryToEntry.getDiscont_at_start_date_rub_reg_ld_1_l())
                .DISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M(testEntryToEntry.getDiscont_at_start_date_rub_forifrsacc_reg_ld_1_m())
                .deposit_sum_not_disc_RUB_REG_LD_1_N(testEntryToEntry.getDeposit_sum_not_disc_rub_reg_ld_1_n())
                .DISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P(testEntryToEntry.getDiscont_sum_at_new_end_date_cur_reg_ld_1_p())
                .DISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q(testEntryToEntry.getDisc_sum_at_new_end_date_rub_reg_ld_1_q())
                .DISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(testEntryToEntry.getDisc_diff_betw_disconts_rub_reg_ld_1_r())
                .REVAL_CORR_DISC_rub_REG_LD_1_S(testEntryToEntry.getReval_corr_disc_rub_reg_ld_1_s())
                .CORR_ACC_AMORT_DISC_rub_REG_LD_1_T(testEntryToEntry.getCorr_acc_amort_disc_rub_reg_ld_1_t())
                .CORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U(testEntryToEntry.getCorr_new_date_higher_discont_rub_reg_ld_1_u())
                .CORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V(testEntryToEntry.getCorr_new_date_higher_corr_acc_amort_disc_rub_reg_ld_1_v())
                .CORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W(testEntryToEntry.getCorr_new_date_less_discont_rub_reg_ld_1_w())
                .CORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X(testEntryToEntry.getCorr_new_date_less_corr_acc_amort_disc_rub_reg_ld_1_x())
                .ACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H(testEntryToEntry.getAccum_amort_discont_start_period_cur_reg_ld_2_h())
                .AMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I(testEntryToEntry.getAmort_discont_current_period_cur_reg_ld_2_i())
                .ACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(testEntryToEntry.getAccum_amort_discont_end_period_cur_reg_ld_2_j())
                .ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(testEntryToEntry.getAccum_amort_discont_start_period_rub_reg_ld_2_k())
                .AMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M(testEntryToEntry.getAmort_discont_current_period_rub_reg_ld_2_m())
                .ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N(testEntryToEntry.getAccum_amort_discont_end_period_rub_reg_ld_2_n())
                .discountedSum_at_current_end_date_cur_REG_LD_3_G(testEntryToEntry.getDiscountedsum_at_current_end_date_cur_reg_ld_3_g())
                .INCOMING_LD_BODY_RUB_REG_LD_3_L(testEntryToEntry.getIncoming_ld_body_rub_reg_ld_3_l())
                .OUTCOMING_LD_BODY_REG_LD_3_M(testEntryToEntry.getOutcoming_ld_body_reg_ld_3_m())
                .REVAL_LD_BODY_PLUS_REG_LD_3_N(testEntryToEntry.getReval_ld_body_plus_reg_ld_3_n())
                .REVAL_LD_BODY_MINUS_REG_LD_3_O(testEntryToEntry.getReval_ld_body_minus_reg_ld_3_o())
                .ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R(testEntryToEntry.getAccum_amort_discont_start_period_rub_reg_ld_3_r())
                .ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S(testEntryToEntry.getAccum_amort_discont_end_period_rub_reg_ld_3_s())
                .REVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T(testEntryToEntry.getReval_acc_amort_plus_rub_reg_ld_3_t())
                .REVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U(testEntryToEntry.getReval_acc_amort_minus_rub_reg_ld_3_u())
                .SUM_PLUS_FOREX_DIFF_REG_LD_3_V(testEntryToEntry.getSum_plus_forex_diff_reg_ld_3_v())
                .SUM_MINUS_FOREX_DIFF_REG_LD_3_W(testEntryToEntry.getSum_minus_forex_diff_reg_ld_3_w())
                .DISPOSAL_BODY_RUB_REG_LD_3_X(testEntryToEntry.getDisposal_body_rub_reg_ld_3_x())
                .DISPOSAL_DISCONT_RUB_REG_LD_3_Y(testEntryToEntry.getDisposal_discont_rub_reg_ld_3_y())
                .LDTERM_REG_LD_3_Z(testEntryToEntry.getLdterm_reg_ld_3_z())
                .TERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA(testEntryToEntry.getTermreclass_body_currentperiod_reg_ld_3_aa())
                .TERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB(testEntryToEntry.getTermreclass_percent_currentperiod_reg_ld_3_ab())
                .TERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC(testEntryToEntry.getTermreclass_body_prevperiod_reg_ld_3_ac())
                .TERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD(testEntryToEntry.getTermreclass_percent_prevperiod_reg_ld_3_ad())
                .ADVANCE_CURRENTPERIOD_REG_LD_3_AE(testEntryToEntry.getAdvance_currentperiod_reg_ld_3_ae())
                .ADVANCE_PREVPERIOD_REG_LD_3_AF(testEntryToEntry.getAdvance_prevperiod_reg_ld_3_af())
                .build();
    }

    private void pasteLeasingDepositIntoExpectedEntries() {
        leasingDeposits.forEach(ld -> {
            entries_expected.stream().filter(e -> e.getEntryID().getLeasingDeposit_id() == ld.getId())
                    .forEach(e -> e.setLeasingDeposit(ld));
        });
    }

    private void createExchangeRates() {
        if (nonNull(testDataKeeper.getExchangeRates())) {
            testDataKeeper.getExchangeRates().forEach(er -> exRates.add((toExchangeRate(er))));
        }
    }

    private ExchangeRate toExchangeRate(ExchangeRateTestData testExchangeRateToExchangeRate) {
        ExchangeRateID exchangeRateID = ExchangeRateID.builder()
                .currency(this.currencies.stream().filter(c -> c.getId().equals(testExchangeRateToExchangeRate.getCurrencyCode())).collect(Collectors.toList()).get(0))
                .scenario(this.scenarios.stream().filter(s -> s.getId().equals(testExchangeRateToExchangeRate.getScenarioCode())).collect(Collectors.toList()).get(0))
                .date(DateFormat.parsingDate(testExchangeRateToExchangeRate.getDate()))
                .build();

        ExchangeRate exchangeRate = ExchangeRate.builder()
                .exchangeRateID(exchangeRateID)
                .rate_at_date(testExchangeRateToExchangeRate.getRate_at_date())
                .average_rate_for_month(testExchangeRateToExchangeRate.getAverage_rate_for_month())
                .build();

        exchangeRate.setLastChange(ZonedDateTime.now());
        exchangeRate.setUser(user);
        return exchangeRate;
    }

    private void createEntryForCalculationEntryIfrs() {
        entryForEntryIfrsCalculation = toEntry(testDataKeeper.getEntryForEntryIfrsCalculation());
    }

    private void createIfrsAccounts() {
        if (nonNull(testDataKeeper.getIfrsAccounts())) {
            testDataKeeper.getIfrsAccounts().forEach(iAcc -> ifrsAccounts.add(toIfrsAcc(iAcc)));
        }
    }

    private IFRSAccount toIfrsAcc(IfrsAccountTestData testIfrsAccountToIfrsAccount) {
        IFRSAccount ifrsAccount = IFRSAccount.builder().id(testIfrsAccountToIfrsAccount.getId())
                .account_code(testIfrsAccountToIfrsAccount.getAccount_code())
                .account_name(testIfrsAccountToIfrsAccount.getAccount_name())
                .ct(testIfrsAccountToIfrsAccount.getCt())
                .dr(testIfrsAccountToIfrsAccount.getDr())
                .flow_code(testIfrsAccountToIfrsAccount.getFlow_code())
                .flow_name(testIfrsAccountToIfrsAccount.getFlow_name())
                .isInverseSum(testIfrsAccountToIfrsAccount.isInverseSum())
                .mappingFormAndColumn(testIfrsAccountToIfrsAccount.getMappingFormAndColumn())
                .pa(testIfrsAccountToIfrsAccount.getPa())
                .sh(testIfrsAccountToIfrsAccount.getSh())
                .build();

        ifrsAccount.setLastChange(ZonedDateTime.now());
        ifrsAccount.setUser(this.user.getId().equals(testIfrsAccountToIfrsAccount.getUserCode()) ? user : null);
        return ifrsAccount;
    }

    private void createEntriesForEntryIfrsSumDaoTests() {
        if (nonNull(testDataKeeper.getEntriesForIfrsSumDaoTest())) {
            testDataKeeper.getEntriesForIfrsSumDaoTest().forEach(e -> entriesForIfrsSumDaoTest.add(toEntry(e)));
        }
    }

    private void createEntriesIfrsAccounts() {
        if (nonNull(testDataKeeper.getEntriesIfrsExcepted())) {
            testDataKeeper.getEntriesIfrsExcepted().forEach(e -> entriesIfrsExcepted.add(toIfrsEntry(e)));
        }
    }

    private void createEntriesIfrsAccountsForDaoTests() {
        if (nonNull(testDataKeeper.getEntriesIfrsForIfrsSumDaoTests())) {
            testDataKeeper.getEntriesIfrsForIfrsSumDaoTests().forEach(e -> entriesIfrsForIfrsSumDaoTests.add(toIfrsEntry(e)));
        }
    }

    private EntryIFRSAcc toIfrsEntry(EntryIfrsAccTestData testEntryIfrsToEntryIfrs) {
        EntryIFRSAccID entryIFRSAccID = EntryIFRSAccID.builder()
                .entry(isNull(entryForEntryIfrsCalculation) ?
                        entriesForIfrsSumDaoTest.stream()
                                .filter(e -> e.getEntryID().getLeasingDeposit_id() == testEntryIfrsToEntryIfrs.getLeasingDepositCode())
                                .findFirst().get() : entryForEntryIfrsCalculation)
                .ifrsAccount(this.ifrsAccounts.stream().filter(i -> i.getId().equals(testEntryIfrsToEntryIfrs.getIfrsAccountCode())).collect(Collectors.toList()).get(0))
                .build();

        EntryIFRSAcc entryIFRSAcc = EntryIFRSAcc.builder()
                .entryIFRSAccID(entryIFRSAccID)
                .sum(testEntryIfrsToEntryIfrs.getSum())
                .build();

        entryIFRSAcc.setLastChange(DateFormat.parsingZonedDateTime(testEntryIfrsToEntryIfrs.getLastChange()));
        entryIFRSAcc.setUser(this.user.getId().equals(testEntryIfrsToEntryIfrs.getUserCode()) ? user : null);
        return entryIFRSAcc;
    }
}
