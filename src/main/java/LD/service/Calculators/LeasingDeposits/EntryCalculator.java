package LD.service.Calculators.LeasingDeposits;

import LD.dao.DaoKeeper;
import LD.model.Entry.Entry;
import LD.model.Entry.EntryID;
import LD.model.Enums.*;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.Scenario.Scenario;
import LD.repository.DepositRatesRepository;
import LD.repository.ExchangeRateRepository;
import LD.repository.PeriodRepository;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static LD.service.Calculators.LeasingDeposits.SupportEntryCalculator.calculateDurationInDaysBetween;
import static java.util.Objects.nonNull;

@Data
@Log4j2
public class EntryCalculator implements Callable<List<Entry>> {

    private final LocalDate UNINITIALIZED = LocalDate.MIN;
    Comparator<LocalDate> ZDTcomp =
            (date1, date2) -> (int) (date1.toEpochDay() - date2.toEpochDay());
    ArrayList<Entry> entriesExistingBeforeCalculating = new ArrayList<>();
    ArrayList<Entry> calculatedStornoDeletedEntries;
    ExchangeRateRepository exchangeRateRepository;
    PeriodRepository periodRepository;
    DepositRatesRepository depositRatesRepository;
    private BigDecimal depositSumDiscountedOnFirstEndDate;
    private LocalDate firstEndDate;
    private BigDecimal percentPerDay;
    private LocalDate firstPeriodWithoutEntryUtc;
    private LocalDate dateUntilThatEntriesMustBeCalculated;
    private TreeMap<LocalDate, LocalDate> mappingPeriodEndDate;
    private LocalDate depositLastDayOfFirstMonth;
    private CalculationParametersSource calculationParametersSource;
    private Scenario scenarioTo;
    private Scenario scenarioFrom;
    private LeasingDeposit leasingDepositToCalculate;
    private SupportEntryCalculator supportData;

    public EntryCalculator(LeasingDeposit leasingDepositToCalculate,
                           CalculationParametersSource calculationParametersSource,
                           DaoKeeper daoKeeper) {
        this.calculationParametersSource = calculationParametersSource;
        this.scenarioTo = this.calculationParametersSource.getScenarioTo();
        this.scenarioFrom = this.calculationParametersSource.getScenarioFrom();
        this.leasingDepositToCalculate = leasingDepositToCalculate;
        this.depositRatesRepository = daoKeeper.getDepositRatesRepository();
        this.exchangeRateRepository = daoKeeper.getExchangeRateRepository();
        this.periodRepository = daoKeeper.getPeriodRepository();
    }

    @Override
    public List<Entry> call() {
        List<Entry> result = new ArrayList<>();

        log.info("Начинается расчет транзакций в калькуляторе");
        result = this.calculate(calculationParametersSource.getFirstOpenPeriodOfScenarioTo());

        log.info("Расчет калькулятора завершен. Количество записей = {}", result.size());
        return result;
    }

    public List<Entry> calculate(LocalDate firstOpenPeriod) {
        if (isDepositAlreadyHasEntries()) {
            copyEntries();
        }

        if (isDepositDeleted()) {
            log.trace("Депозит является удалённым");
            setDeleteStatusToExistingEntries();
        } else {
            log.trace("Депозит не является удалённым");

            supportData = SupportEntryCalculator.calculateDateUntilThatEntriesMustBeCalculated(this.leasingDepositToCalculate,
                    this.scenarioTo, this.depositRatesRepository, firstOpenPeriod);

            this.mappingPeriodEndDate = supportData.getMappingPeriodEndDate();

            this.depositLastDayOfFirstMonth = supportData.getDepositLastDayOfFirstMonth();
            log.info("Первый период депозита => {}", this.depositLastDayOfFirstMonth);

            this.firstEndDate = supportData.getFirstEndDate();
            log.info("this.firstEndDate => {}", this.firstEndDate);

            this.dateUntilThatEntriesMustBeCalculated = supportData.getDateUntilThatEntriesMustBeCalculated();
            log.info("dateUntilThatEntriesMustBeCalculated = {}", dateUntilThatEntriesMustBeCalculated);

            this.percentPerDay = supportData.getDepositDayRate();

            if (isDepositDurationMoreThanOneYear()) {
                discountNominalValue();
            } else {
                keepNominalValue();
            }

            log.info("deposit_sum_discounted_on_firstEndDate = {}", depositSumDiscountedOnFirstEndDate);

            stornoExistingEntries();

            firstPeriodWithoutEntryUtc = findFirstNotCalculatedPeriod();
            log.info("firstPeriodWithoutEntryUtc = {}", firstPeriodWithoutEntryUtc);

            checkIfEntriesInScenarioAdditionIsEqualToOrOneMonthLessFirstOpenPeriodOrThrowException();

            calculatedStornoDeletedEntries.addAll(countEntrysForLD());
        }

        return calculatedStornoDeletedEntries;
    }

    private void checkIfEntriesInScenarioAdditionIsEqualToOrOneMonthLessFirstOpenPeriodOrThrowException() {
        LocalDate firstNotCalculatedPeriodOfScenarioFrom = calculateFirstUncalculatedPeriodForScenario(scenarioFrom);

        //если сценарий-источник не равен сценарию-получателю, значит расчет = ADD => FULL
        if (isCalculationScenariosDiffer()) {
            if (!(firstNotCalculatedPeriodOfScenarioFrom.withDayOfMonth(1)
                    .minusDays(1)
                    .isEqual(this.calculationParametersSource.getFirstOpenPeriodOfScenarioFrom()) ||
                    firstNotCalculatedPeriodOfScenarioFrom.isEqual(
                            this.calculationParametersSource.getFirstOpenPeriodOfScenarioFrom()))) {
                throw new IllegalArgumentException(
                        "Транзакции лизингового депозита не соответствуют закрытому периоду: " +
                                "период последней рассчитанной транзакции должен быть или равен первому открытому периоду или должен быть меньше строго на один период");
            }
        }
    }

    private void keepNominalValue() {
        depositSumDiscountedOnFirstEndDate =
                this.leasingDepositToCalculate.getDeposit_sum_not_disc();
    }

    private void discountNominalValue() {
        depositSumDiscountedOnFirstEndDate =
                countDiscountedValueFromStartDateToNeededDate(this.firstEndDate,
                        this.leasingDepositToCalculate.getStart_date());
    }

    private boolean isDepositDurationMoreThanOneYear() {
        return supportData.isDurationMoreThanOneYear();
    }

    private void copyEntries() {
        entriesExistingBeforeCalculating.addAll(this.leasingDepositToCalculate.getEntries());
    }

    private boolean isDepositAlreadyHasEntries() {
        Set<Entry> entries = this.leasingDepositToCalculate.getEntries();

        if (nonNull(entries)) {
            return entries.size() > 0;
        }

        return false;
    }

    private boolean isDepositDeleted() {
        return this.leasingDepositToCalculate.getIs_deleted() == STATUS_X.X;
    }

    private void setDeleteStatusToExistingEntries() {
        calculatedStornoDeletedEntries =
                changeStatusInLastEntries(entriesExistingBeforeCalculating, scenarioTo,
                        EntryStatus.DELETED);
    }

    private void stornoExistingEntries() {
        calculatedStornoDeletedEntries =
                changeStatusInLastEntries(entriesExistingBeforeCalculating, this.scenarioTo,
                        EntryStatus.STORNO);
    }

    private ArrayList<Entry> changeStatusInLastEntries(List<Entry> EntriesExistingBeforeCalculating,
                                                       Scenario scenarioWhereToChangeStatusInEntries,
                                                       EntryStatus newStatus) {
        ArrayList<Entry> DeletedStornoEntries = new ArrayList<>();

        List<Entry> stream_ActualEntries = EntriesExistingBeforeCalculating.stream()
                .filter(entry -> {
                    if (entry.getStatus()
                            .equals(EntryStatus.ACTUAL)) {
                        return true;
                    } else {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        if (newStatus == EntryStatus.STORNO) {
            stream_ActualEntries = stream_ActualEntries.stream()
                    .filter(entry -> {
                        if (entry.getEntryID()
                                .getScenario()
                                .equals(scenarioWhereToChangeStatusInEntries)) {
                            return true;
                        } else {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            if (scenarioWhereToChangeStatusInEntries.getStatus() == ScenarioStornoStatus.ADDITION) {
                stream_ActualEntries = stream_ActualEntries.stream()
                        .filter(entry -> entry.getEntryID()
                                .getPeriod()
                                .getDate()
                                .equals(calculationParametersSource.getFirstOpenPeriodOfScenarioTo()
                                ))
                        .collect(Collectors.toList());
            }

        }

        stream_ActualEntries.forEach(entry -> {
            entry.setStatus(newStatus);
            DeletedStornoEntries.add(entry);
        });

        return DeletedStornoEntries;
    }

    private List<Entry> countEntrysForLD() {
        ArrayList<Entry> OnlyCalculatedEntries = new ArrayList<>();
        ArrayList<Entry> CalculatedAndExistingBeforeCalculationEntries = new ArrayList<>();
        CalculatedAndExistingBeforeCalculationEntries.addAll(entriesExistingBeforeCalculating);

        LocalDate firstPeriodWithoutEntry =
                this.firstPeriodWithoutEntryUtc;
        LocalDate min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month_InDays =
                dateUntilThatEntriesMustBeCalculated;

        BigDecimal exRateAtStartDate = this.exchangeRateRepository.getRateAtDate(this.leasingDepositToCalculate.getStart_date(),
                this.leasingDepositToCalculate.getScenario(),
                this.leasingDepositToCalculate.getCurrency());

        //Для случаев, когда все транзакции сделаны => чтоб не было новых
        if (firstPeriodWithoutEntry.isBefore(min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month_InDays)) {
            log.info(
                    "firstPeriodWithoutEntry.datesUntil(min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month_InDays, java.time.Period.ofMonths(1)).collect(Collectors.toList()) = {}",
                    firstPeriodWithoutEntry.datesUntil(
                            min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month_InDays,
                            java.time.Period.ofMonths(1))
                            .collect(Collectors.toList()));
            for (LocalDate closingdate : firstPeriodWithoutEntry.datesUntil(
                    min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month_InDays,
                    java.time.Period.ofMonths(1))
                    .collect(Collectors.toList())) {
                log.info("Расчет периода с датой (до коррекции на последнюю дату) => {}",
                        closingdate);
                closingdate = closingdate.withDayOfMonth(closingdate.lengthOfMonth());
                LocalDate finalClosingdate = closingdate;
                log.info("Расчет периода с датой (после коррекции на последнюю дату) => {}",
                        finalClosingdate);

                //TODO: уницифировать контроллер (в контроллере присваивается отрицательное значение)
                // и эту проверку (здесь идет проверка на not null)
                if (isCalculationScenariosDiffer()) {
                    if (isDateCopyFromInitialized()) {
                        if ((closingdate.isEqual(calculationParametersSource.getEntriesCopyDateFromScenarioFromToScenarioTo()) ||
                                closingdate.isAfter(calculationParametersSource.getEntriesCopyDateFromScenarioFromToScenarioTo())) &&
                                closingdate.isBefore(calculationParametersSource.getFirstOpenPeriodOfScenarioFrom())) {
                            log.info("Осуществляется копирование со сценария {} на сценарий {}",
                                    calculationParametersSource.getScenarioFrom()
                                            .getName(), calculationParametersSource.getScenarioTo()
                                            .getName());

                            LocalDate finalClosingdate1 = closingdate;
                            List<Entry> L_entryTocopy = this.leasingDepositToCalculate.getEntries()
                                    .stream()
                                    .filter(entry -> entry.getEntryID()
                                            .getScenario()
                                            .equals(calculationParametersSource.getScenarioFrom()))
                                    .collect(Collectors.toList());

                            L_entryTocopy = L_entryTocopy.stream()
                                    .filter(entry -> entry.getEntryID()
                                            .getPeriod()
                                            .getDate()

                                            .isEqual(finalClosingdate1))
                                    .collect(Collectors.toList());

                            Entry entryToCopy = L_entryTocopy.get(0);

                            EntryID newEntryID = entryToCopy.getEntryID()
                                    .toBuilder()
                                    .scenario(calculationParametersSource.getScenarioTo())
                                    .CALCULATION_TIME(ZonedDateTime.now())
                                    .build();

                            Entry newEntry = entryToCopy.toBuilder()
                                    .entryID(newEntryID)
                                    .build();
                            this.calculatedStornoDeletedEntries.add(newEntry);

                            continue;
                        }

                    }
                }

                EntryID entryID = EntryID.builder()
                        .leasingDeposit_id(this.leasingDepositToCalculate.getId())
                        .CALCULATION_TIME(ZonedDateTime.now())
                        .period(periodRepository.findPeriodByDate(finalClosingdate))
                        .scenario(scenarioTo)
                        .build();

                log.info("Получен ключ транзакции => {}", entryID);

                Entry t = new Entry();
                t.setLastChange(entryID.getCALCULATION_TIME());

                if (finalClosingdate.isBefore(calculationParametersSource.getFirstOpenPeriodOfScenarioTo())) {
                    t.setStatus_EntryMadeDuringOrAfterClosedPeriod(
                            EntryPeriodCreation.AFTER_CLOSING_PERIOD);
                } else {
                    t.setStatus_EntryMadeDuringOrAfterClosedPeriod(
                            EntryPeriodCreation.CURRENT_PERIOD);
                }

                t.setUser(this.calculationParametersSource.getUser());
                t.setLeasingDeposit(this.leasingDepositToCalculate);
                t.setEntryID(entryID);
                t.setEnd_date_at_this_period(
                        this.mappingPeriodEndDate.floorEntry(finalClosingdate)
                                .getValue());
                t.setStatus(EntryStatus.ACTUAL);
                t.setPercentRateForPeriodForLD(roundNumberToScale10(supportData.getDepositYearRate()));
                t.setDISCONT_AT_START_DATE_cur_REG_LD_1_K(
                        this.depositSumDiscountedOnFirstEndDate.subtract(
                                this.leasingDepositToCalculate.getDeposit_sum_not_disc()).setScale(10, RoundingMode.HALF_UP));
                t.setDISCONT_AT_START_DATE_RUB_REG_LD_1_L(
                        t.getDISCONT_AT_START_DATE_cur_REG_LD_1_K()
                                .multiply(exRateAtStartDate).setScale(10, RoundingMode.HALF_UP));

                if (closingdate.isEqual(this.depositLastDayOfFirstMonth)) {
                    t.setDeposit_sum_not_disc_RUB_REG_LD_1_N(
                            this.leasingDepositToCalculate.getDeposit_sum_not_disc()
                                    .multiply(exRateAtStartDate).setScale(10, RoundingMode.HALF_UP));

                    t.setDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M(
                            t.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L().setScale(10, RoundingMode.HALF_UP));
                } else {
                    t.setDeposit_sum_not_disc_RUB_REG_LD_1_N(BigDecimal.ZERO);
                    t.setDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M(BigDecimal.ZERO);
                }

                LocalDate PrevClosingDate = finalClosingdate.minusMonths(1)
                        .withDayOfMonth(finalClosingdate.minusMonths(1).lengthOfMonth());
                log.info("Предыдущая дата закрытия => {}", PrevClosingDate);

                if (PrevClosingDate.isAfter(this.depositLastDayOfFirstMonth) &&
                        !this.mappingPeriodEndDate.floorEntry(PrevClosingDate)
                                .getValue()
                                .isEqual(t.getEnd_date_at_this_period())) {
                    BigDecimal deposit_sum_discounted_on_End_date_at_this_period = BigDecimal.ZERO;

                    if (isDepositDurationMoreThanOneYear()) {
                        deposit_sum_discounted_on_End_date_at_this_period =
                                countDiscountedValueFromStartDateToNeededDate(
                                        t.getEnd_date_at_this_period(),
                                        this.leasingDepositToCalculate.getStart_date());
                    } else {
                        deposit_sum_discounted_on_End_date_at_this_period =
                                this.leasingDepositToCalculate.getDeposit_sum_not_disc();
                    }

                    t.setDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P(
                            deposit_sum_discounted_on_End_date_at_this_period.subtract(
                                    this.leasingDepositToCalculate.getDeposit_sum_not_disc()).setScale(10, RoundingMode.HALF_UP));
                    t.setDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q(
                            t.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P()
                                    .multiply(exRateAtStartDate).setScale(10, RoundingMode.HALF_UP));

                    if (isDepositDurationMoreThanOneYear()) {
                        //Поиск последнего периода с суммой в поле корректировки дисконта в рублях
                        BigDecimal lastRevaluationOfDiscount = BigDecimal.ZERO;

                        if (isCalculationScenariosDiffer()) {
                            if (PrevClosingDate.isBefore(
                                    calculationParametersSource.getFirstOpenPeriodOfScenarioFrom())) {
                                lastRevaluationOfDiscount =
                                        findLastRevaluationOfDiscount(calculationParametersSource.getScenarioFrom(),
                                                finalClosingdate,
                                                CalculatedAndExistingBeforeCalculationEntries);
                            } else {
                                LocalDate prevDateBeforeFirstOpenPeriodForScenarioFrom =
                                        calculationParametersSource.getFirstOpenPeriodOfScenarioFrom()
                                                .withDayOfMonth(1)
                                                .minusDays(1);
                                BigDecimal
                                        lastCalculatedDiscountForScenarioFromOnDateBeforeFirstOpenPeriod1 =
                                        findLastRevaluationOfDiscount(calculationParametersSource.getScenarioFrom(),
                                                prevDateBeforeFirstOpenPeriodForScenarioFrom,
                                                CalculatedAndExistingBeforeCalculationEntries);

                                BigDecimal lastCalculatedDiscountForScenarioTo =
                                        findLastRevaluationOfDiscount(scenarioTo, finalClosingdate,
                                                CalculatedAndExistingBeforeCalculationEntries);

                                lastRevaluationOfDiscount =
                                        lastCalculatedDiscountForScenarioTo.compareTo(
                                                BigDecimal.ZERO) != 0 ?
                                                lastCalculatedDiscountForScenarioTo :
                                                lastCalculatedDiscountForScenarioFromOnDateBeforeFirstOpenPeriod1;
                            }
                        } else {
                            lastRevaluationOfDiscount =
                                    findLastRevaluationOfDiscount(scenarioTo, finalClosingdate,
                                            CalculatedAndExistingBeforeCalculationEntries);
                        }

                        if (lastRevaluationOfDiscount.compareTo(BigDecimal.ZERO) == 0) {
                            t.setDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(
                                    t.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q()
                                            .subtract(t.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L()).setScale(10, RoundingMode.HALF_UP));
                        } else {
                            t.setDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(
                                    t.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q()
                                            .subtract(lastRevaluationOfDiscount).setScale(10, RoundingMode.HALF_UP));
                        }

                    } else {
                        t.setDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(BigDecimal.ZERO);
                    }

                    //Поиск последнего периода с суммой в поле корректировки дисконта в валюте
                    log.info("Начинается расчет курса на прошлую дату");
                    BigDecimal curExOnPrevClosingDate = BigDecimal.ZERO;

                    if (isCalculationScenariosDiffer()) {
                        if (PrevClosingDate.isBefore(
                                calculationParametersSource.getFirstOpenPeriodOfScenarioFrom())) {
                            curExOnPrevClosingDate = this.exchangeRateRepository.getRateAtDate(PrevClosingDate,
                                    calculationParametersSource.getScenarioFrom(),
                                    this.leasingDepositToCalculate.getCurrency());
                        } else {
                            curExOnPrevClosingDate = this.exchangeRateRepository.getRateAtDate(PrevClosingDate,
                                    scenarioTo,
                                    this.leasingDepositToCalculate.getCurrency());
                        }
                    } else {
                        curExOnPrevClosingDate = this.exchangeRateRepository.getRateAtDate(PrevClosingDate,
                                scenarioTo,
                                this.leasingDepositToCalculate.getCurrency());
                    }

                    log.info("Курс валюты на конец прошлого периода => {}", curExOnPrevClosingDate);

                    BigDecimal lastCalculatedDiscount = BigDecimal.ZERO;

                    if (isCalculationScenariosDiffer()) {
                        if (PrevClosingDate.isBefore(
                                calculationParametersSource.getFirstOpenPeriodOfScenarioFrom())) {
                            lastCalculatedDiscount =
                                    findLastCalculatedDiscount(calculationParametersSource.getScenarioFrom(),
                                            finalClosingdate,
                                            CalculatedAndExistingBeforeCalculationEntries);
                        } else {
                            LocalDate prevDateBeforeFirstOpenPeriodForScenarioFrom =
                                    calculationParametersSource.getFirstOpenPeriodOfScenarioFrom()
                                            .withDayOfMonth(1)
                                            .minusDays(1);
                            BigDecimal
                                    lastCalculatedDiscountForScenarioFromOnDateBeforeFirstOpenPeriod1 =
                                    findLastCalculatedDiscount(calculationParametersSource.getScenarioFrom(),
                                            prevDateBeforeFirstOpenPeriodForScenarioFrom,
                                            CalculatedAndExistingBeforeCalculationEntries);

                            BigDecimal lastCalculatedDiscountForScenarioTo =
                                    findLastCalculatedDiscount(scenarioTo, finalClosingdate,
                                            CalculatedAndExistingBeforeCalculationEntries);

                            lastCalculatedDiscount = lastCalculatedDiscountForScenarioTo.compareTo(
                                    BigDecimal.ZERO) != 0 ? lastCalculatedDiscountForScenarioTo :
                                    lastCalculatedDiscountForScenarioFromOnDateBeforeFirstOpenPeriod1;
                        }
                    } else {
                        lastCalculatedDiscount =
                                findLastCalculatedDiscount(scenarioTo, finalClosingdate,
                                        CalculatedAndExistingBeforeCalculationEntries);
                    }

                    if (isDepositDurationMoreThanOneYear()) {
                        if (lastCalculatedDiscount.compareTo(BigDecimal.ZERO) == 0) {
                            t.setREVAL_CORR_DISC_rub_REG_LD_1_S(
                                    t.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P()
                                            .subtract(t.getDISCONT_AT_START_DATE_cur_REG_LD_1_K())
                                            .multiply(curExOnPrevClosingDate.subtract(
                                                    exRateAtStartDate)).setScale(10, RoundingMode.HALF_UP));
                        } else {
                            t.setREVAL_CORR_DISC_rub_REG_LD_1_S(
                                    t.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P()
                                            .subtract(lastCalculatedDiscount)
                                            .multiply(curExOnPrevClosingDate.subtract(
                                                    exRateAtStartDate)).setScale(10, RoundingMode.HALF_UP));
                        }
                    } else {
                        t.setREVAL_CORR_DISC_rub_REG_LD_1_S(BigDecimal.ZERO);
                    }

                    LocalDate endDateAtPrevClosingDate =
                            this.mappingPeriodEndDate.floorEntry(PrevClosingDate)
                                    .getValue();
                    BigDecimal after_DiscSumAtStartDate =
                            countDiscountedValueFromStartDateToNeededDate(
                                    t.getEnd_date_at_this_period(),
                                    this.leasingDepositToCalculate.getStart_date());
                    BigDecimal after_DiscSumWithAccumAm =
                            countDiscountedValueFromStartDateToNeededDate(
                                    t.getEnd_date_at_this_period(), PrevClosingDate);
                    BigDecimal after_Discount_cur =

                            after_DiscSumWithAccumAm.subtract(after_DiscSumAtStartDate);
                    BigDecimal before_DiscSumAtStartDate =
                            countDiscountedValueFromStartDateToNeededDate(endDateAtPrevClosingDate,
                                    this.leasingDepositToCalculate.getStart_date());
                    BigDecimal before_DiscSumWithAccumAm =
                            countDiscountedValueFromStartDateToNeededDate(endDateAtPrevClosingDate,
                                    PrevClosingDate);
                    BigDecimal before_Discount_cur =
                            before_DiscSumWithAccumAm.subtract(before_DiscSumAtStartDate);

                    if (isDepositDurationMoreThanOneYear()) {
                        t.setCORR_ACC_AMORT_DISC_rub_REG_LD_1_T(
                                after_Discount_cur.subtract(before_Discount_cur)
                                        .multiply(curExOnPrevClosingDate).setScale(10, RoundingMode.HALF_UP));
                    } else {
                        t.setCORR_ACC_AMORT_DISC_rub_REG_LD_1_T(BigDecimal.ZERO);
                    }

                    if (t.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R()
                            .compareTo(BigDecimal.ZERO) < 0) {
                        t.setCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U(
                                t.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R()
                                        .add(t.getREVAL_CORR_DISC_rub_REG_LD_1_S()).setScale(10, RoundingMode.HALF_UP));
                        t.setCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W(BigDecimal.ZERO);

                        t.setCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V(
                                t.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(10, RoundingMode.HALF_UP));
                        t.setCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X(BigDecimal.ZERO);
                    } else if (t.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R()
                            .compareTo(BigDecimal.ZERO) > 0) {
                        t.setCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W(
                                t.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R()
                                        .add(t.getREVAL_CORR_DISC_rub_REG_LD_1_S()).setScale(10, RoundingMode.HALF_UP));
                        t.setCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U(BigDecimal.ZERO);

                        t.setCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X(
                                t.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(10, RoundingMode.HALF_UP));
                        t.setCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V(
                                BigDecimal.ZERO);
                    }
                } else {
                    log.info("Дата закрытия периода позже первого отчетного периода для депозита, " +
                            "дата завершения депозита по сравнению с прошлым периодом не изменилась");

                    t.setREVAL_CORR_DISC_rub_REG_LD_1_S(BigDecimal.ZERO);
                    t.setDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P(BigDecimal.ZERO);
                    t.setDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q(BigDecimal.ZERO);
                    t.setDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(BigDecimal.ZERO);
                    t.setDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P(BigDecimal.ZERO);
                    t.setCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X(BigDecimal.ZERO);
                    t.setCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V(BigDecimal.ZERO);
                    t.setCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W(BigDecimal.ZERO);
                    t.setCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U(BigDecimal.ZERO);
                    t.setCORR_ACC_AMORT_DISC_rub_REG_LD_1_T(BigDecimal.ZERO);
                }

//Reg.LeasingDeposit.model.LeasingDeposit.2---------------------START
                log.info("Начинается поиск среднего курса на текущую отчетную дату");

                BigDecimal avgExRateForPeriod = this.exchangeRateRepository.getAverageRateAtDate(finalClosingdate,
                        scenarioTo,
                        this.leasingDepositToCalculate.getCurrency());

                log.info("Средний курс валюты текущего периода => {}", avgExRateForPeriod);

                if (isDepositDurationMoreThanOneYear()) {
                    if (t.getEnd_date_at_this_period()
                            .isAfter(finalClosingdate)) {
                        t.setACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(
                                countDiscountFromStartDateToNeededDate(
                                        t.getEnd_date_at_this_period(), finalClosingdate).setScale(10, RoundingMode.HALF_UP));
                    } else {
                        t.setACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(
                                countDiscountFromStartDateToNeededDate(
                                        t.getEnd_date_at_this_period(),
                                        t.getEnd_date_at_this_period()).setScale(10,

                                        RoundingMode.HALF_UP));
                    }

                    if (PrevClosingDate.isAfter(this.leasingDepositToCalculate.getStart_date()) ||
                            PrevClosingDate.isEqual(this.leasingDepositToCalculate.getStart_date())) {
                        t.setACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H(
                                countDiscountFromStartDateToNeededDate(
                                        t.getEnd_date_at_this_period(), PrevClosingDate).setScale(10, RoundingMode.HALF_UP));

                        List<Entry> lastEntryIn2Scenarios = new ArrayList<>();

                        if (isCalculationScenariosDiffer()) {
                            if (finalClosingdate.isEqual(
                                    calculationParametersSource.getFirstOpenPeriodOfScenarioFrom())) {
                                lastEntryIn2Scenarios =
                                        findLastEntry(calculationParametersSource.getScenarioFrom(),
                                                calculationParametersSource.getScenarioFrom(), PrevClosingDate,
                                                CalculatedAndExistingBeforeCalculationEntries);
                            } else {
                                lastEntryIn2Scenarios =
                                        findLastEntry(scenarioTo, scenarioTo, PrevClosingDate,
                                                CalculatedAndExistingBeforeCalculationEntries);
                            }
                        } else {
                            lastEntryIn2Scenarios =
                                    findLastEntry(scenarioTo, scenarioTo, PrevClosingDate,
                                            CalculatedAndExistingBeforeCalculationEntries);
                        }

                        if (lastEntryIn2Scenarios.size() > 0) {
                            if (lastEntryIn2Scenarios.get(0)
                                    .getEnd_date_at_this_period()
                                    .isEqual(t.getEnd_date_at_this_period())) {
                                t.setACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(
                                        lastEntryIn2Scenarios.get(0)
                                                .getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(10, RoundingMode.HALF_UP));
                            } else {
                                BigDecimal accumulatedDiscountRUB = BigDecimal.ZERO;

                                if (!calculationParametersSource.getScenarioFrom()
                                        .equals(calculationParametersSource.getScenarioTo())) {
                                    accumulatedDiscountRUB = calculateAccumDiscountRUB_RegLD2(
                                            this.depositLastDayOfFirstMonth,
                                            calculationParametersSource.getFirstOpenPeriodOfScenarioFrom(), calculationParametersSource.getScenarioFrom(), t);

                                    if (!calculationParametersSource.getFirstOpenPeriodOfScenarioFrom().isEqual(finalClosingdate)) {
                                        accumulatedDiscountRUB = accumulatedDiscountRUB.add(
                                                calculateAccumDiscountRUB_RegLD2(
                                                        calculationParametersSource.getFirstOpenPeriodOfScenarioFrom()
                                                        , finalClosingdate, scenarioTo, t));
                                    }
                                } else {
                                    accumulatedDiscountRUB = calculateAccumDiscountRUB_RegLD2(
                                            this.depositLastDayOfFirstMonth,
                                            finalClosingdate, scenarioTo, t);
                                }

                                t.setACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(
                                        accumulatedDiscountRUB.setScale(10, RoundingMode.HALF_UP));
                            }
                        }
                    } else {
                        t.setACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H(BigDecimal.ZERO);
                        t.setACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(BigDecimal.ZERO);
                    }
                } else {
                    t.setACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(BigDecimal.ZERO);
                    t.setACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N(BigDecimal.ZERO);
                    t.setACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H(BigDecimal.ZERO);
                    t.setACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(BigDecimal.ZERO);
                }

                t.setAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I(
                        t.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J()
                                .subtract(t.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H()).setScale(10, RoundingMode.HALF_UP));
                t.setAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M(
                        t.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I()
                                .multiply(avgExRateForPeriod).setScale(10, RoundingMode.HALF_UP));

                t.setACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N(
                        t.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K()
                                .add(t.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M()).setScale(10, RoundingMode.HALF_UP));
                //Reg.LeasingDeposit.model.LeasingDeposit.2---------------------END

                //Reg.LeasingDeposit.model.LeasingDeposit.3---------------------START
                if (t.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P()
                        .compareTo(BigDecimal.ZERO) != 0) {
                    t.setDiscountedSum_at_current_end_date_cur_REG_LD_3_G(
                            this.leasingDepositToCalculate.getDeposit_sum_not_disc()
                                    .add(t.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P()).setScale(10, RoundingMode.HALF_UP));
                } else {
                    BigDecimal lastCalculatedDiscount = BigDecimal.ZERO;

                    if (isCalculationScenariosDiffer()) {
                        if (finalClosingdate.isEqual(
                                calculationParametersSource.getFirstOpenPeriodOfScenarioFrom())) {
                            lastCalculatedDiscount =

                                    findLastCalculatedDiscount(calculationParametersSource.getScenarioFrom(),
                                            finalClosingdate,
                                            CalculatedAndExistingBeforeCalculationEntries);
                        } else {
                            lastCalculatedDiscount =
                                    findLastCalculatedDiscount(calculationParametersSource.getScenarioFrom(),
                                            calculationParametersSource.getFirstOpenPeriodOfScenarioFrom(),
                                            CalculatedAndExistingBeforeCalculationEntries);
                            lastCalculatedDiscount =
                                    lastCalculatedDiscount.compareTo(BigDecimal.ZERO) != 0 ?
                                            lastCalculatedDiscount :
                                            findLastCalculatedDiscount(scenarioTo, finalClosingdate,
                                                    CalculatedAndExistingBeforeCalculationEntries);
                        }
                    } else {
                        lastCalculatedDiscount =
                                findLastCalculatedDiscount(scenarioTo, finalClosingdate,
                                        CalculatedAndExistingBeforeCalculationEntries);
                    }

                    if (lastCalculatedDiscount.compareTo(BigDecimal.ZERO) == 0) {
                        t.setDiscountedSum_at_current_end_date_cur_REG_LD_3_G(
                                this.leasingDepositToCalculate.getDeposit_sum_not_disc()
                                        .add(t.getDISCONT_AT_START_DATE_cur_REG_LD_1_K()).setScale(10, RoundingMode.HALF_UP));
                    } else {
                        t.setDiscountedSum_at_current_end_date_cur_REG_LD_3_G(
                                this.leasingDepositToCalculate.getDeposit_sum_not_disc()
                                        .add(lastCalculatedDiscount).setScale(10, RoundingMode.HALF_UP));
                    }
                }

                BigDecimal ExRateOnClosingdate = this.exchangeRateRepository.getRateAtDate(finalClosingdate,
                        scenarioTo,
                        this.leasingDepositToCalculate.getCurrency());

                if (finalClosingdate
                        .isEqual(this.depositLastDayOfFirstMonth)) {
                    t.setINCOMING_LD_BODY_RUB_REG_LD_3_L(
                            t.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G()
                                    .multiply(exRateAtStartDate).setScale(10, RoundingMode.HALF_UP));
                    t.setACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R(BigDecimal.ZERO);
                } else {
                    BigDecimal ExRateOnPrevClosingdate = BigDecimal.ZERO;

                    if (isCalculationScenariosDiffer()) {
                        if (PrevClosingDate.isBefore(
                                calculationParametersSource.getFirstOpenPeriodOfScenarioFrom())) {
                            ExRateOnPrevClosingdate = this.exchangeRateRepository.getRateAtDate(PrevClosingDate,
                                    calculationParametersSource.getScenarioFrom(),
                                    this.leasingDepositToCalculate.getCurrency());
                        } else {
                            ExRateOnPrevClosingdate = this.exchangeRateRepository.getRateAtDate(PrevClosingDate,
                                    scenarioTo,
                                    this.leasingDepositToCalculate.getCurrency());
                        }
                    } else {
                        ExRateOnPrevClosingdate = this.exchangeRateRepository.getRateAtDate(PrevClosingDate,
                                scenarioTo,
                                this.leasingDepositToCalculate.getCurrency());
                    }

                    t.setINCOMING_LD_BODY_RUB_REG_LD_3_L(
                            t.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G()
                                    .multiply(ExRateOnPrevClosingdate).setScale(10, RoundingMode.HALF_UP));
                    t.setACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R(
                            t.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H()
                                    .multiply(ExRateOnPrevClosingdate).setScale(10, RoundingMode.HALF_UP));
                }

                t.setOUTCOMING_LD_BODY_REG_LD_3_M(
                        t.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G()
                                .multiply(ExRateOnClosingdate).setScale(10, RoundingMode.HALF_UP));

                if (t.getOUTCOMING_LD_BODY_REG_LD_3_M()
                        .compareTo(t.getINCOMING_LD_BODY_RUB_REG_LD_3_L()) > 0) {
                    t.setREVAL_LD_BODY_PLUS_REG_LD_3_N(t.getOUTCOMING_LD_BODY_REG_LD_3_M()
                            .subtract(t.getINCOMING_LD_BODY_RUB_REG_LD_3_L()).setScale(10, RoundingMode.HALF_UP));
                    t.setREVAL_LD_BODY_MINUS_REG_LD_3_O(BigDecimal.ZERO);
                } else {
                    t.setREVAL_LD_BODY_PLUS_REG_LD_3_N(BigDecimal.ZERO);
                    t.setREVAL_LD_BODY_MINUS_REG_LD_3_O(t.getOUTCOMING_LD_BODY_REG_LD_3_M()
                            .subtract(t.getINCOMING_LD_BODY_RUB_REG_LD_3_L()).setScale(10, RoundingMode.HALF_UP));
                }

                t.setACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S(
                        t.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J()
                                .multiply(ExRateOnClosingdate).setScale(10, RoundingMode.HALF_UP));

                if (t.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S()
                        .subtract(t.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R())
                        .subtract(t.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M())
                        .compareTo(BigDecimal.ZERO) > 0) {
                    t.setREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U(BigDecimal.ZERO);
                    t.setREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T(
                            t.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S()
                                    .subtract(
                                            t.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R())
                                    .subtract(t.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M()).setScale(10, RoundingMode.HALF_UP));
                } else {
                    t.setREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U(
                            t.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S()
                                    .subtract(
                                            t.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R())
                                    .subtract(t.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M()).setScale(10, RoundingMode.HALF_UP));
                    t.setREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T(BigDecimal.ZERO);
                }

                if (t.getREVAL_LD_BODY_PLUS_REG_LD_3_N()
                        .add(t.getREVAL_LD_BODY_MINUS_REG_LD_3_O())
                        .add(t.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T())
                        .add(t.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U())
                        .compareTo(BigDecimal.ZERO) > 0) {
                    t.setSUM_PLUS_FOREX_DIFF_REG_LD_3_V(t.getREVAL_LD_BODY_PLUS_REG_LD_3_N()
                            .add(t.getREVAL_LD_BODY_MINUS_REG_LD_3_O())
                            .add(t.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T())
                            .add(t.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U())
                            .negate().setScale(10, RoundingMode.HALF_UP));
                    t.setSUM_MINUS_FOREX_DIFF_REG_LD_3_W(BigDecimal.ZERO);
                } else {
                    t.setSUM_MINUS_FOREX_DIFF_REG_LD_3_W(t.getREVAL_LD_BODY_PLUS_REG_LD_3_N()
                            .add(t.getREVAL_LD_BODY_MINUS_REG_LD_3_O())
                            .add(t.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T())
                            .add(t.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U())
                            .negate().setScale(10, RoundingMode.HALF_UP));
                    t.setSUM_PLUS_FOREX_DIFF_REG_LD_3_V(BigDecimal.ZERO);
                }

                if (t.getEnd_date_at_this_period().isEqual(finalClosingdate) || (t.getEnd_date_at_this_period()
                        .isBefore(finalClosingdate) && t.getEnd_date_at_this_period()
                        .isAfter(finalClosingdate.withDayOfMonth(1)))) {
                    t.setDISPOSAL_BODY_RUB_REG_LD_3_X(t.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(10, RoundingMode.HALF_UP));
                    t.setDISPOSAL_DISCONT_RUB_REG_LD_3_Y(
                            t.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(10, RoundingMode.HALF_UP));
                } else {
                    t.setDISPOSAL_BODY_RUB_REG_LD_3_X(BigDecimal.ZERO);
                    t.setDISPOSAL_DISCONT_RUB_REG_LD_3_Y(BigDecimal.ZERO);
                }

                log.info("((int) (t.getEnd_date_at_this_period().toEpochDay() - finalClosingdate.toEpochDay()))) = {}",
                        ((int) (t.getEnd_date_at_this_period().toEpochDay() - finalClosingdate.toEpochDay())) / 30.417);

                log.info("((int) (t.getEnd_date_at_this_period().toEpochDay() - finalClosingdate.toEpochDay())) / 30.417 = {}",
                        (((int) (t.getEnd_date_at_this_period().toEpochDay() - finalClosingdate.toEpochDay())) / 30.417));

                if ((((int) (t.getEnd_date_at_this_period().toEpochDay() - finalClosingdate.toEpochDay())) / 30.417) >= 12) {
                    t.setLDTERM_REG_LD_3_Z(LeasingDepositDuration.LT);
                } else {
                    t.setLDTERM_REG_LD_3_Z(LeasingDepositDuration.ST);
                }

                if (t.getEnd_date_at_this_period().isAfter(finalClosingdate)) {
                    if (t.getLDTERM_REG_LD_3_Z()
                            .equals(LeasingDepositDuration.ST)) {
                        t.setTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA(
                                t.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(10, RoundingMode.HALF_UP));
                        t.setTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB(
                                t.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(10, RoundingMode.HALF_UP));
                    } else {
                        t.setTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA(BigDecimal.ZERO);
                        t.setTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB(BigDecimal.ZERO);
                    }
                } else {
                    t.setTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA(BigDecimal.ZERO);
                    t.setTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB(BigDecimal.ZERO);
                }

                if (t.getLDTERM_REG_LD_3_Z()
                        .equals(LeasingDepositDuration.ST)) {
                    t.setADVANCE_CURRENTPERIOD_REG_LD_3_AE(
                            this.depositSumDiscountedOnFirstEndDate.multiply(
                                    exRateAtStartDate).setScale(10, RoundingMode.HALF_UP));
                } else {
                    t.setADVANCE_CURRENTPERIOD_REG_LD_3_AE(BigDecimal.ZERO);
                }

                if (findLastEntry(this.leasingDepositToCalculate.getScenario(), scenarioTo,
                        PrevClosingDate, CalculatedAndExistingBeforeCalculationEntries).size() >
                        0) {
                    Entry lde = findLastEntry(this.leasingDepositToCalculate.getScenario(),
                            scenarioTo, PrevClosingDate,
                            CalculatedAndExistingBeforeCalculationEntries).get(0);
                    t.setTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC(
                            lde.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(10, RoundingMode.HALF_UP));
                    t.setTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD(
                            lde.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(10, RoundingMode.HALF_UP));
                    t.setADVANCE_PREVPERIOD_REG_LD_3_AF(lde.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(10, RoundingMode.HALF_UP));
                } else {
                    t.setTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC(BigDecimal.ZERO);
                    t.setTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD(BigDecimal.ZERO);
                    t.setADVANCE_PREVPERIOD_REG_LD_3_AF(BigDecimal.ZERO);
                }

//Reg.LeasingDeposit.model.LeasingDeposit.3---------------------END

                CalculatedAndExistingBeforeCalculationEntries.add(t);
                OnlyCalculatedEntries.add(t);
                log.info("Расчет за период закончен");
            }

            log.info("Все расчеты завершены");
        }

        return OnlyCalculatedEntries;
    }

    private BigDecimal roundNumberToScale10(BigDecimal number) {
        return number.setScale(10, RoundingMode.HALF_UP);
    }

    private BigDecimal countDiscountedValueFromStartDateToNeededDate(LocalDate endDate,
                                                                     LocalDate neededDate) {
        BigDecimal countDiscountedValueFromStartDateToNeededDate = BigDecimal.ZERO;

        int LDdurationDays = calculateDurationInDaysBetween(this.leasingDepositToCalculate.getStart_date(), endDate);

        countDiscountedValueFromStartDateToNeededDate =
                this.leasingDepositToCalculate.getDeposit_sum_not_disc()
                        .setScale(32)
                        .divide(BigDecimal.ONE.add(percentPerDay)
                                .pow(LDdurationDays), RoundingMode.UP);

        int LDdurationFormStartToNeededDays = calculateDurationInDaysBetween(this.leasingDepositToCalculate.getStart_date(), neededDate);

        countDiscountedValueFromStartDateToNeededDate =
                countDiscountedValueFromStartDateToNeededDate.multiply(
                        BigDecimal.ONE.add(percentPerDay)
                                .pow(LDdurationFormStartToNeededDays));

        return countDiscountedValueFromStartDateToNeededDate;
    }

    private BigDecimal countDiscountFromStartDateToNeededDate(LocalDate endDate, LocalDate neededDate) {
        return countDiscountedValueFromStartDateToNeededDate(endDate, neededDate).subtract(
                countDiscountedValueFromStartDateToNeededDate(endDate,
                        this.leasingDepositToCalculate.getStart_date()));
    }

    private List<Entry> findLastEntry(Scenario scenarioFrom, Scenario scenarioTo,
                                      LocalDate Date, List<Entry> entries) {
        List<Entry> LastEntry = entries.stream()
                .filter(entry -> entry.getStatus()
                        .equals(EntryStatus.ACTUAL))
                .filter(entry -> entry.getEntryID()
                        .getScenario()
                        .equals(scenarioFrom) || entry.getEntryID()
                        .getScenario()
                        .equals(scenarioTo))
                .filter(entry -> entry.getEntryID()
                        .getPeriod()
                        .getDate()
                        .equals(Date))
                .collect(Collectors.toList());

        if (LastEntry.size() == 0
                || LastEntry.size() == 1) {
            return LastEntry;
        }
        if (LastEntry.size() > 1) {
            return LastEntry.stream()
                    .filter(entry -> entry.getEntryID()
                            .getScenario()
                            .equals(scenarioFrom))
                    .collect(Collectors.toList());
        }

        return LastEntry;
    }

    private BigDecimal findLastCalculatedDiscount(Scenario scenarioWhereFind,
                                                  LocalDate finalClosingdate,
                                                  List<Entry> entries) {
        BigDecimal LastCalculatedDiscount = BigDecimal.ZERO;

        TreeMap<LocalDate, BigDecimal> tmZDT_BD = entries.stream()
                .filter(entry -> entry.getStatus()
                        .equals(EntryStatus.ACTUAL))
                .filter(entry -> entry.getEntryID()
                        .getScenario()
                        .equals(scenarioWhereFind))
                .filter(entry -> entry.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P()
                        .compareTo(BigDecimal.ZERO) != 0)
                .collect(TreeMap::new, (tm, entry) -> {
                    tm.put(entry.getEntryID()
                                    .getPeriod()
                                    .getDate(),
                            entry.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P());
                }, (tm1, tm2) -> tm1.putAll(tm2));

        Optional<LocalDate> ZDTOfLastRevaluationBefore = tmZDT_BD.keySet()
                .stream()
                .filter(key -> key.isBefore(finalClosingdate))
                .max(this.ZDTcomp);

        if (ZDTOfLastRevaluationBefore.isPresent()) {
            LastCalculatedDiscount = tmZDT_BD.get(ZDTOfLastRevaluationBefore.get());
        }

        return LastCalculatedDiscount;
    }

    private BigDecimal findLastRevaluationOfDiscount(Scenario scenarioTo,
                                                     LocalDate finalClosingdate,
                                                     List<Entry> entries) {
        BigDecimal LastRevaluation = BigDecimal.ZERO;

        TreeMap<LocalDate, BigDecimal> tmZDT_BD = entries.stream()
                .filter(entry -> entry.getStatus()
                        .equals(EntryStatus.ACTUAL) && entry.getEntryID()
                        .getScenario()
                        .equals(scenarioTo))
                .collect(TreeMap::new, (tm, entry) -> {
                    if (entry.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q()
                            .compareTo(BigDecimal.ZERO) != 0) {
                        tm.put(entry.getEntryID()
                                        .getPeriod()
                                        .getDate(),
                                entry.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q());
                    }
                }, (tm1, tm2) -> tm1.putAll(tm2));

        Optional<LocalDate> ZDTOfLastRevaluationBefore = tmZDT_BD.keySet()
                .stream()
                .filter(key -> key.isBefore(finalClosingdate))
                .max(this.ZDTcomp);

        if (ZDTOfLastRevaluationBefore.isPresent()) {
            LastRevaluation = tmZDT_BD.get(ZDTOfLastRevaluationBefore.get());
        }

        return LastRevaluation;
    }

    private LocalDate findFirstNotCalculatedPeriod() {
        LocalDate firstNotCalculatedPeriodOfScenarioTo = findFirstNotCalculatedPeriodOfScenarioTo();


        return firstNotCalculatedPeriodOfScenarioTo;
    }

    private LocalDate findFirstNotCalculatedPeriodOfScenarioTo() {
        LocalDate firstNotCalculatedPeriodOfScenarioTO = UNINITIALIZED;

        if (isCalculationScenariosEqual() && isScenarioFromAdditional()) {
            firstNotCalculatedPeriodOfScenarioTO = calculateFirstUncalculatedPeriodForScenario(scenarioFrom);
        }

        if (isCalculationScenariosDiffer()) {
            if (isDateCopyFromInitialized()) {
                firstNotCalculatedPeriodOfScenarioTO = calculationParametersSource.getEntriesCopyDateFromScenarioFromToScenarioTo();
            } else {
                firstNotCalculatedPeriodOfScenarioTO = calculationParametersSource.getFirstOpenPeriodOfScenarioFrom();
            }
        }

        //TODO: Fix: FULL -> FULL impossible!
        if (isCalculationScenariosEqual() && isScenarioFromFullStorno()) {
            firstNotCalculatedPeriodOfScenarioTO = calculateFirstUncalculatedPeriodForScenario(scenarioFrom);
        }
        return firstNotCalculatedPeriodOfScenarioTO;
    }

    private boolean isDateCopyFromInitialized() {
        return !this.calculationParametersSource.getEntriesCopyDateFromScenarioFromToScenarioTo().isEqual(UNINITIALIZED);
    }

    private boolean isScenarioFromAdditional() {
        return scenarioFrom.getStatus().equals(ScenarioStornoStatus.ADDITION);
    }

    private boolean isScenarioFromFullStorno() {
        return scenarioFrom.getStatus().equals(ScenarioStornoStatus.FULL);
    }

    private boolean isCalculationScenariosEqual() {
        return scenarioFrom.equals(scenarioTo);
    }

    private boolean isCalculationScenariosDiffer() {
        return !scenarioFrom.equals(scenarioTo);
    }

    private LocalDate calculateFirstUncalculatedPeriodForScenario(Scenario scenario) {
        LocalDate LastPeriodWithEntry = this.depositLastDayOfFirstMonth.minusMonths(1);

        for (LocalDate date : getDatesFromStartMonthTillDateUntilThatEntriesMustBeCalculated()) {
            date = transformIntoLastDayOfMonth(date);

            if (isCountEntriesWithScenarioAndDateAtLeastOne(scenario, date)) {
                LastPeriodWithEntry = date;
            } else {
                break;
            }
        }

        LocalDate nextDateAfterLastWithEntry = LastPeriodWithEntry.plusMonths(1);
        nextDateAfterLastWithEntry = transformIntoLastDayOfMonth(nextDateAfterLastWithEntry);

        return nextDateAfterLastWithEntry;
    }

    private boolean isCountEntriesWithScenarioAndDateAtLeastOne(Scenario scenario, LocalDate date) {
        return entriesExistingBeforeCalculating.stream()
                .filter(entry -> entry.getEntryID()
                        .getScenario()
                        .equals(scenario))
                .filter(entry -> entry.getStatus()
                        .equals(EntryStatus.ACTUAL))
                .filter(entry -> entry.getEntryID()
                        .getPeriod()
                        .getDate()
                        .isEqual(date))
                .count() > 0;
    }

    private LocalDate transformIntoLastDayOfMonth(LocalDate date) {
        return date.withDayOfMonth(date.lengthOfMonth());
    }

    private List<LocalDate> getDatesFromStartMonthTillDateUntilThatEntriesMustBeCalculated() {
        return this.depositLastDayOfFirstMonth.datesUntil(
                dateUntilThatEntriesMustBeCalculated,
                java.time.Period.ofMonths(1))
                .collect(Collectors.toList());
    }

    private LocalDate getDateOneMonthBehindLastDayOfFirstMonth() {
        return this.depositLastDayOfFirstMonth.minusMonths(1);
    }

    BigDecimal calculateAccumDiscountRUB_RegLD2(LocalDate startCalculatingInclusive,
                                                LocalDate dateUntilCountExclusive,
                                                Scenario whereCalculate,
                                                Entry calculatingEntry) {
        if (startCalculatingInclusive.isEqual(dateUntilCountExclusive)) {
            throw new IllegalArgumentException("Wrong argument values: startCalculatingInclusive equals dateUntilCountExclusive");
        }

        BigDecimal accumulatedDiscountRUB = BigDecimal.ZERO;

        for (LocalDate date : startCalculatingInclusive.datesUntil(
                dateUntilCountExclusive.withDayOfMonth(1)
                , java.time.Period.ofMonths(1))
                .collect(Collectors.toList())) {
            LocalDate lastPeriod = date.withDayOfMonth(1).minusDays(1);

            if (lastPeriod.isBefore(this.leasingDepositToCalculate.getStart_date())) {
                lastPeriod = this.leasingDepositToCalculate.getStart_date();
            }

            LocalDate dateLastDayOfMonth = date.withDayOfMonth(date.lengthOfMonth());

            if (!dateLastDayOfMonth.isEqual(dateUntilCountExclusive)) {
                BigDecimal avgExRateForCalculating = this.exchangeRateRepository.getAverageRateAtDate(dateLastDayOfMonth,
                        whereCalculate,
                        this.leasingDepositToCalculate.getCurrency());

                BigDecimal discountForPeriodCUR = this.countDiscountFromStartDateToNeededDate(
                        calculatingEntry.getEnd_date_at_this_period(),
                        dateLastDayOfMonth)
                        .subtract(this.countDiscountFromStartDateToNeededDate(
                                calculatingEntry.getEnd_date_at_this_period(),
                                lastPeriod));

                accumulatedDiscountRUB = accumulatedDiscountRUB.add(
                        discountForPeriodCUR.multiply(avgExRateForCalculating));
            }
        }

        return accumulatedDiscountRUB;
    }
}