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
import java.time.Period;
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
    private final LocalDate firstOpenPeriodOfScenarioFrom;
    private final LocalDate firstOpenPeriodOfScenarioTo;
    private Comparator<LocalDate> ZDTcomp =
            (date1, date2) -> (int) (date1.toEpochDay() - date2.toEpochDay());

    private ArrayList<Entry> entriesExistingBeforeCalculating = new ArrayList<>();
    private ArrayList<Entry> calculatedStornoDeletedEntries = new ArrayList<>();
    private ArrayList<Entry> onlyCalculatedEntries = new ArrayList<>();
    private ArrayList<Entry> calculatedAndExistingBeforeCalculationEntries = new ArrayList<>();

    private ExchangeRateRepository exchangeRateRepository;
    private PeriodRepository periodRepository;
    private DepositRatesRepository depositRatesRepository;
    private BigDecimal depositSumDiscountedOnFirstEndDate;
    private LocalDate firstEndDate;
    private BigDecimal percentPerDay;
    private LocalDate firstNotCalculatedPeriod;
    private LocalDate dateUntilThatEntriesMustBeCalculated;
    private TreeMap<LocalDate, LocalDate> mappingPeriodEndDate;
    private LocalDate depositLastDayOfFirstMonth;
    private CalculationParametersSource calculationParametersSource;
    private Scenario scenarioTo;
    private Scenario scenarioFrom;
    private LeasingDeposit leasingDepositToCalculate;
    private SupportEntryCalculator supportData;
    private BigDecimal exchangeRateToRubAtStartDate;
    private BigDecimal nominalDepositSum;

    public EntryCalculator(LeasingDeposit leasingDepositToCalculate,
                           CalculationParametersSource calculationParametersSource,
                           DaoKeeper daoKeeper) {
        this.calculationParametersSource = calculationParametersSource;
        this.scenarioTo = this.calculationParametersSource.getScenarioTo();
        this.scenarioFrom = this.calculationParametersSource.getScenarioFrom();
        this.firstOpenPeriodOfScenarioFrom = this.calculationParametersSource.getFirstOpenPeriodOfScenarioFrom();
        this.firstOpenPeriodOfScenarioTo = this.calculationParametersSource.getFirstOpenPeriodOfScenarioTo();
        this.leasingDepositToCalculate = leasingDepositToCalculate;
        this.depositRatesRepository = daoKeeper.getDepositRatesRepository();
        this.exchangeRateRepository = daoKeeper.getExchangeRateRepository();
        this.periodRepository = daoKeeper.getPeriodRepository();
    }

    @Override
    public List<Entry> call() {
        log.info("Начинается расчет проводок в калькуляторе");
        List<Entry> entries = calculateEntries();

        log.info("Расчет калькулятора завершен. Количество проводок = {}", entries.size());
        return entries;
    }

    public List<Entry> calculateEntries() {
        if (isDepositAlreadyHasEntries()) {
            copyEntriesIntoCalculationResultList();
        }

        if (isDepositDeleted()) {
            log.trace("Депозит является удалённым");
            setDeleteStatusToAllEntriesIntoCalculationResultList();
        } else {
            log.trace("Депозит не является удалённым");

            calculateNominalSumRub();

            supportData = SupportEntryCalculator.calculateDateUntilThatEntriesMustBeCalculated(this.leasingDepositToCalculate,
                    this.scenarioTo, this.depositRatesRepository, this.firstOpenPeriodOfScenarioTo);

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

            log.info("depositSumDiscountedOnFirstEndDate = {}", depositSumDiscountedOnFirstEndDate);

            stornoExistingEntries();

            firstNotCalculatedPeriod = findFirstNotCalculatedPeriod();
            log.info("firstNotCalculatedPeriod = {}", firstNotCalculatedPeriod);

            calculateNewEntriesForPeriod();
        }

        return calculatedStornoDeletedEntries;
    }

    private void calculateNominalSumRub() {
        nominalDepositSum = this.leasingDepositToCalculate.getDeposit_sum_not_disc();
    }

    private boolean isDepositAlreadyHasEntries() {
        Set<Entry> entries = this.leasingDepositToCalculate.getEntries();

        if (nonNull(entries)) {
            return entries.size() > 0;
        }

        return false;
    }

    private void copyEntriesIntoCalculationResultList() {
        entriesExistingBeforeCalculating.addAll(this.leasingDepositToCalculate.getEntries());
    }

    private boolean isDepositDeleted() {
        return this.leasingDepositToCalculate.getIs_deleted() == STATUS_X.X;
    }

    private void setDeleteStatusToAllEntriesIntoCalculationResultList() {
        List<Entry> deletedEntries = entriesExistingBeforeCalculating.stream()
                .filter(entry -> entry.getStatus().equals(EntryStatus.ACTUAL))
                .peek(entry -> entry.setStatus(EntryStatus.DELETED))
                .collect(Collectors.toList());

        calculatedStornoDeletedEntries.addAll(deletedEntries);
    }

    private boolean isDepositDurationMoreThanOneYear() {
        return supportData.isDurationMoreThanOneYear();
    }

    private void discountNominalValue() {
        depositSumDiscountedOnFirstEndDate = discountNominalValueUntilStartDateFromDate(this.firstEndDate);
    }

    private void keepNominalValue() {
        depositSumDiscountedOnFirstEndDate = nominalDepositSum;
    }

    private void stornoExistingEntries() {
        List<Entry> stornoEntries = entriesExistingBeforeCalculating.stream()
                .filter(entry -> entry.getStatus().equals(EntryStatus.ACTUAL))
                .filter(entry -> entry.getEntryID().getScenario().equals(this.scenarioTo))
                .filter(entry -> {
                    if (scenarioTo.getStatus() == ScenarioStornoStatus.ADDITION) {
                        return entry.getEntryID()
                                .getPeriod()
                                .getDate()
                                .equals(this.firstOpenPeriodOfScenarioTo);
                    } else {
                        return true;
                    }
                })
                .peek(entry -> entry.setStatus(EntryStatus.STORNO))
                .collect(Collectors.toList());

        calculatedStornoDeletedEntries.addAll(stornoEntries);
    }

    private LocalDate findFirstNotCalculatedPeriod() {
        LocalDate firstNotCalculatedPeriod = UNINITIALIZED;

        if (isCalculationScenariosEqual()) {
            if (isScenarioFromAdditional()) {
                firstNotCalculatedPeriod = calculateFirstUncalculatedPeriodForScenario(scenarioFrom);
            } else if (isScenarioFromFullStorno()) {
                throw new IllegalArgumentException("Запрещённая операция расчёта между одним сценарием со статусом: FULL -> FULL");
            }
        } else if (isCalculationScenariosDiffer()) {
            throwExceptionIfLastEntryOfDepositInScenarioFromNotEqualToOrOneMonthLessFirstOpenPeriod();

            if (isDateCopyFromInitialized()) {
                firstNotCalculatedPeriod = calculationParametersSource.getEntriesCopyDateFromScenarioFromToScenarioTo();
            } else {
                firstNotCalculatedPeriod = firstOpenPeriodOfScenarioFrom;
            }
        }

        return firstNotCalculatedPeriod;
    }

    private boolean isCalculationScenariosEqual() {
        return scenarioFrom.equals(scenarioTo);
    }

    private boolean isScenarioFromAdditional() {
        return scenarioFrom.getStatus().equals(ScenarioStornoStatus.ADDITION);
    }

    private LocalDate calculateFirstUncalculatedPeriodForScenario(Scenario scenario) {
        LocalDate LastPeriodWithEntry = this.depositLastDayOfFirstMonth.minusMonths(1);

        for (LocalDate date : getDatesFromStartMonthTillDateUntilEntriesMustBeCalculated()) {
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

    private List<LocalDate> getDatesFromStartMonthTillDateUntilEntriesMustBeCalculated() {
        return this.depositLastDayOfFirstMonth.datesUntil(
                dateUntilThatEntriesMustBeCalculated,
                java.time.Period.ofMonths(1))
                .collect(Collectors.toList());
    }

    private LocalDate transformIntoLastDayOfMonth(LocalDate date) {
        return date.withDayOfMonth(date.lengthOfMonth());
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

    private boolean isScenarioFromFullStorno() {
        return scenarioFrom.getStatus().equals(ScenarioStornoStatus.FULL);
    }

    private void calculateNewEntriesForPeriod() {
        calculatedAndExistingBeforeCalculationEntries.addAll(entriesExistingBeforeCalculating);

        if (allEntriesNotCalculated()) {
            for (LocalDate calculationPeriod : getPeriodsForCalculation()) {
                log.info("Расчет периода с датой => {}", calculationPeriod);

                if (isCalculationScenariosDiffer()) {
                    if (isDateCopyFromInitialized()) {
                        if (isCopyDateBeforeFirstOpenPeriodOfScenarioFrom(calculationPeriod)) {
                            copyEntryFromScenarioFromIntoScenarioTo(calculationPeriod);
                            continue;
                        }
                    }
                }

                calculateNewEntriesForPeriod(calculationPeriod);
            }
        }

        log.info("Все расчеты завершены");

        calculatedStornoDeletedEntries.addAll(onlyCalculatedEntries);
    }

    private boolean allEntriesNotCalculated() {
        //Для случаев, когда все транзакции сделаны => чтоб не было новых
        return firstNotCalculatedPeriod.isBefore(dateUntilThatEntriesMustBeCalculated);
    }

    private List<LocalDate> getPeriodsForCalculation() {
        return firstNotCalculatedPeriod.datesUntil(dateUntilThatEntriesMustBeCalculated, Period.ofMonths(1))
                .map(period -> period.withDayOfMonth(period.lengthOfMonth()))
                .collect(Collectors.toList());
    }

    private boolean isCalculationScenariosDiffer() {
        return !scenarioFrom.equals(scenarioTo);
    }

    private void throwExceptionIfLastEntryOfDepositInScenarioFromNotEqualToOrOneMonthLessFirstOpenPeriod() {
        LocalDate firstNotCalculatedPeriodOfScenarioFrom = calculateFirstUncalculatedPeriodForScenario(scenarioFrom);

        LocalDate lastDateWithEntry = firstNotCalculatedPeriodOfScenarioFrom.withDayOfMonth(1).minusDays(1);
        if (!(lastDateWithEntry.isEqual(firstOpenPeriodOfScenarioFrom) ||
                firstNotCalculatedPeriodOfScenarioFrom.isEqual(firstOpenPeriodOfScenarioFrom))) {
            throw new IllegalArgumentException(
                    "Транзакции лизингового депозита не соответствуют закрытому периоду: " +
                            "период последней рассчитанной транзакции должен быть или " +
                            "равен первому открытому периоду или должен быть меньше строго на один период");
        }
    }

    private boolean isDateCopyFromInitialized() {
        return !this.calculationParametersSource.getEntriesCopyDateFromScenarioFromToScenarioTo().isEqual(UNINITIALIZED);
    }

    private boolean isCopyDateBeforeFirstOpenPeriodOfScenarioFrom(LocalDate calculationPeriod) {
        return (calculationPeriod.isEqual(calculationParametersSource.getEntriesCopyDateFromScenarioFromToScenarioTo()) ||
                calculationPeriod.isAfter(calculationParametersSource.getEntriesCopyDateFromScenarioFromToScenarioTo())) &&
                calculationPeriod.isBefore(firstOpenPeriodOfScenarioFrom);
    }

    private void copyEntryFromScenarioFromIntoScenarioTo(LocalDate calculationPeriod) {
        log.info("Осуществляется копирование со сценария {} на сценарий {}",
                scenarioFrom.getName(),
                scenarioTo.getName());

        Entry entryToCopy = entriesExistingBeforeCalculating.stream()
                .filter(entry -> entry.getEntryID().getScenario().equals(scenarioFrom))
                .filter(entry -> entry.getEntryID().getPeriod().getDate().isEqual(calculationPeriod))
                .findAny()
                .get();

        EntryID newEntryID = entryToCopy.getEntryID()
                .toBuilder()
                .scenario(scenarioTo)
                .CALCULATION_TIME(ZonedDateTime.now())
                .build();

        Entry newEntry = entryToCopy.toBuilder()
                .entryID(newEntryID)
                .build();

        this.calculatedStornoDeletedEntries.add(newEntry);
    }

    private void calculateNewEntriesForPeriod(LocalDate calculationPeriod) {
        getExchangeRateAtStartDate();

        Entry newEntry = createEntryIdAndMetaData(calculationPeriod);

        LocalDate previousCalculatedPeriod = calculatePreviousCalculatedPeriodDate(calculationPeriod);
        log.info("Предыдущая дата закрытия => {}", previousCalculatedPeriod);

        calculateRegLd1(previousCalculatedPeriod, calculationPeriod, newEntry);
        calculateRegLd2(calculationPeriod, newEntry, previousCalculatedPeriod);
        calculateRegLd3(calculationPeriod, newEntry, previousCalculatedPeriod);

        calculatedAndExistingBeforeCalculationEntries.add(newEntry);
        onlyCalculatedEntries.add(newEntry);
        log.info("Расчет за период закончен");
    }

    private void getExchangeRateAtStartDate() {
        exchangeRateToRubAtStartDate = this.exchangeRateRepository.getRateAtDate(
                this.leasingDepositToCalculate.getStart_date(),
                this.leasingDepositToCalculate.getScenario(),
                this.leasingDepositToCalculate.getCurrency());
    }

    private Entry createEntryIdAndMetaData(LocalDate calculationPeriod) {
        EntryID entryID = EntryID.builder()
                .leasingDeposit_id(this.leasingDepositToCalculate.getId())
                .CALCULATION_TIME(ZonedDateTime.now())
                .period(periodRepository.findPeriodByDate(calculationPeriod))
                .scenario(scenarioTo)
                .build();

        log.info("Получен ключ транзакции => {}", entryID);

        Entry newEntry = new Entry();
        newEntry.setLastChange(entryID.getCALCULATION_TIME());

        if (calculationPeriod.isBefore(this.firstOpenPeriodOfScenarioTo)) {
            newEntry.setStatus_EntryMadeDuringOrAfterClosedPeriod(
                    EntryPeriodCreation.AFTER_CLOSING_PERIOD);
        } else {
            newEntry.setStatus_EntryMadeDuringOrAfterClosedPeriod(
                    EntryPeriodCreation.CURRENT_PERIOD);
        }

        newEntry.setUserLastChanged(this.calculationParametersSource.getUser());
        newEntry.setLeasingDeposit(this.leasingDepositToCalculate);
        newEntry.setEntryID(entryID);
        newEntry.setEnd_date_at_this_period(this.mappingPeriodEndDate.floorEntry(calculationPeriod).getValue());
        newEntry.setStatus(EntryStatus.ACTUAL);
        newEntry.setPercentRateForPeriodForLD(roundNumberToScale10(supportData.getDepositYearRate()));
        return newEntry;
    }

    private LocalDate calculatePreviousCalculatedPeriodDate(LocalDate calculationPeriod) {
        return calculationPeriod.minusMonths(1).withDayOfMonth(calculationPeriod.minusMonths(1).lengthOfMonth());
    }


    private LocalDate calculateRegLd1(LocalDate previousClosingDate, LocalDate calculationPeriod, Entry calculatingEntry) {
        calculateAndSaveDiscountAtStartDateInto(calculatingEntry);
        calculateAndSaveDiscountAtStartDateRubInto(calculatingEntry);

        if (isEqualFirstDepositPeriod(calculationPeriod)) {
            calculateAndSaveNominalSumRubInto(calculatingEntry);
            calculateAndSaveDiscountRubInto(calculatingEntry);
        } else {
            saveZeroNominalSumRubInto(calculatingEntry);
            saveZeroDiscountSumRubInto(calculatingEntry);
        }

        if (isOneMonthAheadThanFirstMonth(previousClosingDate) &&
                isEndDateChangedComparedToPreviousPeriod(previousClosingDate, calculatingEntry)) {
            BigDecimal discountedDepositValueOnCurrentEndDate = BigDecimal.ZERO;

            if (isDepositDurationMoreThanOneYear()) {
                discountedDepositValueOnCurrentEndDate =
                        discountNominalValueUntilStartDateFromDate(calculatingEntry.getEnd_date_at_this_period());
            } else {
                discountedDepositValueOnCurrentEndDate = nominalDepositSum;
            }

            calculatingEntry.setDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P(
                    discountedDepositValueOnCurrentEndDate.subtract(
                            nominalDepositSum).setScale(10, RoundingMode.HALF_UP));
            calculatingEntry.setDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q(
                    calculatingEntry.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P()
                            .multiply(exchangeRateToRubAtStartDate).setScale(10, RoundingMode.HALF_UP));

            if (isDepositDurationMoreThanOneYear()) {
                //Поиск последнего периода с суммой в поле корректировки дисконта в рублях
                BigDecimal lastRevaluationOfDiscount = BigDecimal.ZERO;

                if (isCalculationScenariosDiffer()) {
                    if (previousClosingDate.isBefore(firstOpenPeriodOfScenarioFrom)) {
                        lastRevaluationOfDiscount = findLastRevaluationOfDiscount(scenarioFrom, calculationPeriod);
                    } else {
                        LocalDate prevDateBeforeFirstOpenPeriodForScenarioFrom =
                                firstOpenPeriodOfScenarioFrom
                                        .withDayOfMonth(1)
                                        .minusDays(1);
                        BigDecimal
                                lastCalculatedDiscountForScenarioFromOnDateBeforeFirstOpenPeriod1 =
                                findLastRevaluationOfDiscount(scenarioFrom, prevDateBeforeFirstOpenPeriodForScenarioFrom);

                        BigDecimal lastCalculatedDiscountForScenarioTo =
                                findLastRevaluationOfDiscount(scenarioTo, calculationPeriod);

                        lastRevaluationOfDiscount =
                                lastCalculatedDiscountForScenarioTo.compareTo(
                                        BigDecimal.ZERO) != 0 ?
                                        lastCalculatedDiscountForScenarioTo :
                                        lastCalculatedDiscountForScenarioFromOnDateBeforeFirstOpenPeriod1;
                    }
                } else {
                    lastRevaluationOfDiscount =
                            findLastRevaluationOfDiscount(scenarioTo, calculationPeriod);
                }

                if (lastRevaluationOfDiscount.compareTo(BigDecimal.ZERO) == 0) {
                    calculatingEntry.setDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(
                            calculatingEntry.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q()
                                    .subtract(calculatingEntry.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L()).setScale(10, RoundingMode.HALF_UP));
                } else {
                    calculatingEntry.setDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(
                            calculatingEntry.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q()
                                    .subtract(lastRevaluationOfDiscount).setScale(10, RoundingMode.HALF_UP));
                }

            } else {
                calculatingEntry.setDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(BigDecimal.ZERO);
            }

            //Поиск последнего периода с суммой в поле корректировки дисконта в валюте
            BigDecimal curExOnPrevClosingDate = getExchangeRateOnPreviousCalculatingDate(previousClosingDate);

            log.info("Курс валюты на конец прошлого периода => {}", curExOnPrevClosingDate);

            BigDecimal lastCalculatedDiscount = BigDecimal.ZERO;

            if (isCalculationScenariosDiffer()) {
                if (previousClosingDate.isBefore(firstOpenPeriodOfScenarioFrom)) {
                    lastCalculatedDiscount = findLastCalculatedDiscount(scenarioFrom, calculationPeriod);
                } else {
                    LocalDate prevDateBeforeFirstOpenPeriodForScenarioFrom =
                            firstOpenPeriodOfScenarioFrom
                                    .withDayOfMonth(1)
                                    .minusDays(1);
                    BigDecimal
                            lastCalculatedDiscountForScenarioFromOnDateBeforeFirstOpenPeriod1 =
                            findLastCalculatedDiscount(scenarioFrom, prevDateBeforeFirstOpenPeriodForScenarioFrom);

                    BigDecimal lastCalculatedDiscountForScenarioTo =
                            findLastCalculatedDiscount(scenarioTo, calculationPeriod);

                    lastCalculatedDiscount = lastCalculatedDiscountForScenarioTo.compareTo(
                            BigDecimal.ZERO) != 0 ? lastCalculatedDiscountForScenarioTo :
                            lastCalculatedDiscountForScenarioFromOnDateBeforeFirstOpenPeriod1;
                }
            } else {
                lastCalculatedDiscount = findLastCalculatedDiscount(scenarioTo, calculationPeriod);
            }

            if (isDepositDurationMoreThanOneYear()) {
                if (lastCalculatedDiscount.compareTo(BigDecimal.ZERO) == 0) {
                    calculatingEntry.setREVAL_CORR_DISC_rub_REG_LD_1_S(
                            calculatingEntry.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P()
                                    .subtract(calculatingEntry.getDISCONT_AT_START_DATE_cur_REG_LD_1_K())
                                    .multiply(curExOnPrevClosingDate.subtract(
                                            exchangeRateToRubAtStartDate)).setScale(10, RoundingMode.HALF_UP));
                } else {
                    calculatingEntry.setREVAL_CORR_DISC_rub_REG_LD_1_S(
                            calculatingEntry.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P()
                                    .subtract(lastCalculatedDiscount)
                                    .multiply(curExOnPrevClosingDate.subtract(
                                            exchangeRateToRubAtStartDate)).setScale(10, RoundingMode.HALF_UP));
                }
            } else {
                calculatingEntry.setREVAL_CORR_DISC_rub_REG_LD_1_S(BigDecimal.ZERO);
            }

            LocalDate endDateAtPrevClosingDate = this.mappingPeriodEndDate.floorEntry(previousClosingDate).getValue();
            BigDecimal after_DiscSumAtStartDate =
                    calculateDiscountedValueFromStartDateToNeededDate(
                            calculatingEntry.getEnd_date_at_this_period(),
                            this.leasingDepositToCalculate.getStart_date());
            BigDecimal after_DiscSumWithAccumAm =
                    calculateDiscountedValueFromStartDateToNeededDate(
                            calculatingEntry.getEnd_date_at_this_period(), previousClosingDate);
            BigDecimal after_Discount_cur = after_DiscSumWithAccumAm.subtract(after_DiscSumAtStartDate);
            BigDecimal before_DiscSumAtStartDate =
                    calculateDiscountedValueFromStartDateToNeededDate(endDateAtPrevClosingDate,
                            this.leasingDepositToCalculate.getStart_date());
            BigDecimal before_DiscSumWithAccumAm =
                    calculateDiscountedValueFromStartDateToNeededDate(endDateAtPrevClosingDate,
                            previousClosingDate);
            BigDecimal before_Discount_cur =
                    before_DiscSumWithAccumAm.subtract(before_DiscSumAtStartDate);

            if (isDepositDurationMoreThanOneYear()) {
                calculatingEntry.setCORR_ACC_AMORT_DISC_rub_REG_LD_1_T(
                        after_Discount_cur.subtract(before_Discount_cur)
                                .multiply(curExOnPrevClosingDate).setScale(10, RoundingMode.HALF_UP));
            } else {
                calculatingEntry.setCORR_ACC_AMORT_DISC_rub_REG_LD_1_T(BigDecimal.ZERO);
            }

            if (calculatingEntry.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().compareTo(BigDecimal.ZERO) < 0) {
                calculatingEntry.setCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U(
                        calculatingEntry.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R()
                                .add(calculatingEntry.getREVAL_CORR_DISC_rub_REG_LD_1_S()).setScale(10, RoundingMode.HALF_UP));
                calculatingEntry.setCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W(BigDecimal.ZERO);

                calculatingEntry.setCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V(
                        calculatingEntry.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(10, RoundingMode.HALF_UP));
                calculatingEntry.setCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X(BigDecimal.ZERO);
            } else if (calculatingEntry.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R()
                    .compareTo(BigDecimal.ZERO) > 0) {
                calculatingEntry.setCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W(
                        calculatingEntry.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R()
                                .add(calculatingEntry.getREVAL_CORR_DISC_rub_REG_LD_1_S()).setScale(10, RoundingMode.HALF_UP));
                calculatingEntry.setCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U(BigDecimal.ZERO);

                calculatingEntry.setCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X(
                        calculatingEntry.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T().setScale(10, RoundingMode.HALF_UP));
                calculatingEntry.setCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V(
                        BigDecimal.ZERO);
            }
        } else {
            setZeroToFieldsDependsOnChangingDates(calculatingEntry);
        }
        return previousClosingDate;
    }

    private void calculateAndSaveDiscountAtStartDateInto(Entry newEntry) {

        BigDecimal discontAtStartDate = this.depositSumDiscountedOnFirstEndDate.subtract(
                nominalDepositSum);

        discontAtStartDate = roundNumberToScale10(discontAtStartDate);

        newEntry.setDISCONT_AT_START_DATE_cur_REG_LD_1_K(discontAtStartDate);
    }

    private BigDecimal roundNumberToScale10(BigDecimal number) {
        return number.setScale(10, RoundingMode.HALF_UP);
    }

    private void calculateAndSaveDiscountAtStartDateRubInto(Entry newEntry) {
        BigDecimal discontAtStartDateRub = newEntry.getDISCONT_AT_START_DATE_cur_REG_LD_1_K()
                .multiply(exchangeRateToRubAtStartDate);

        discontAtStartDateRub = roundNumberToScale10(discontAtStartDateRub);

        newEntry.setDISCONT_AT_START_DATE_RUB_REG_LD_1_L(discontAtStartDateRub);
    }

    private void calculateAndSaveDiscountRubInto(Entry calculatingEntry) {
        BigDecimal discountAtStartDateRub = calculatingEntry.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L();

        discountAtStartDateRub = roundNumberToScale10(discountAtStartDateRub);

        calculatingEntry.setDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M(discountAtStartDateRub);
    }

    private void calculateAndSaveNominalSumRubInto(Entry calculatingEntry) {
        BigDecimal nominalDepositSumRub = nominalDepositSum.multiply(exchangeRateToRubAtStartDate);

        nominalDepositSumRub = roundNumberToScale10(nominalDepositSumRub);

        calculatingEntry.setDeposit_sum_not_disc_RUB_REG_LD_1_N(nominalDepositSumRub);
    }

    private void saveZeroNominalSumRubInto(Entry calculatingEntry) {
        calculatingEntry.setDeposit_sum_not_disc_RUB_REG_LD_1_N(BigDecimal.ZERO);
    }

    private void saveZeroDiscountSumRubInto(Entry calculatingEntry) {
        calculatingEntry.setDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M(BigDecimal.ZERO);
    }

    private boolean isOneMonthAheadThanFirstMonth(LocalDate previousClosingDate) {
        return previousClosingDate.isAfter(this.depositLastDayOfFirstMonth);
    }

    private boolean isEndDateChangedComparedToPreviousPeriod(LocalDate previousClosingDate, Entry calculatingEntry) {
        return !this.mappingPeriodEndDate.floorEntry(previousClosingDate)
                .getValue()
                .isEqual(calculatingEntry.getEnd_date_at_this_period());
    }

    //-->>

    private void setZeroToFieldsDependsOnChangingDates(Entry calculatingEntry) {
        log.info("Дата закрытия периода позже первого отчетного периода для депозита, " +
                "дата завершения депозита по сравнению с прошлым периодом не изменилась");

        calculatingEntry.setREVAL_CORR_DISC_rub_REG_LD_1_S(BigDecimal.ZERO);
        calculatingEntry.setDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P(BigDecimal.ZERO);
        calculatingEntry.setDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q(BigDecimal.ZERO);
        calculatingEntry.setDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(BigDecimal.ZERO);
        calculatingEntry.setDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P(BigDecimal.ZERO);
        calculatingEntry.setCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X(BigDecimal.ZERO);
        calculatingEntry.setCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V(BigDecimal.ZERO);
        calculatingEntry.setCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W(BigDecimal.ZERO);
        calculatingEntry.setCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U(BigDecimal.ZERO);
        calculatingEntry.setCORR_ACC_AMORT_DISC_rub_REG_LD_1_T(BigDecimal.ZERO);
    }

    private boolean isEqualFirstDepositPeriod(LocalDate calculationPeriod) {
        return calculationPeriod.isEqual(this.depositLastDayOfFirstMonth);
    }

    private void calculateRegLd3(LocalDate calculationPeriod, Entry newEntry, LocalDate previousCalculationPeriod) {
        //Reg.LeasingDeposit.model.LeasingDeposit.3---------------------START
        if (newEntry.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().compareTo(BigDecimal.ZERO) != 0) {
            newEntry.setDiscountedSum_at_current_end_date_cur_REG_LD_3_G(
                    nominalDepositSum
                            .add(newEntry.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P()).setScale(10, RoundingMode.HALF_UP));
        } else {
            BigDecimal lastCalculatedDiscount = BigDecimal.ZERO;

            if (isCalculationScenariosDiffer()) {
                if (calculationPeriod.isEqual(
                        firstOpenPeriodOfScenarioFrom)) {
                    lastCalculatedDiscount =

                            findLastCalculatedDiscount(scenarioFrom, calculationPeriod);
                } else {
                    lastCalculatedDiscount =
                            findLastCalculatedDiscount(scenarioFrom, firstOpenPeriodOfScenarioFrom);
                    lastCalculatedDiscount =
                            lastCalculatedDiscount.compareTo(BigDecimal.ZERO) != 0 ?
                                    lastCalculatedDiscount :
                                    findLastCalculatedDiscount(scenarioTo, calculationPeriod);
                }
            } else {
                lastCalculatedDiscount =
                        findLastCalculatedDiscount(scenarioTo, calculationPeriod);
            }

            if (lastCalculatedDiscount.compareTo(BigDecimal.ZERO) == 0) {
                newEntry.setDiscountedSum_at_current_end_date_cur_REG_LD_3_G(
                        nominalDepositSum
                                .add(newEntry.getDISCONT_AT_START_DATE_cur_REG_LD_1_K()).setScale(10, RoundingMode.HALF_UP));
            } else {
                newEntry.setDiscountedSum_at_current_end_date_cur_REG_LD_3_G(
                        nominalDepositSum
                                .add(lastCalculatedDiscount).setScale(10, RoundingMode.HALF_UP));
            }
        }

        BigDecimal ExRateOnClosingdate = this.exchangeRateRepository.getRateAtDate(calculationPeriod,
                scenarioTo,
                this.leasingDepositToCalculate.getCurrency());

        if (isEqualFirstDepositPeriod(calculationPeriod)) {
            newEntry.setINCOMING_LD_BODY_RUB_REG_LD_3_L(
                    newEntry.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G()
                            .multiply(exchangeRateToRubAtStartDate).setScale(10, RoundingMode.HALF_UP));
            newEntry.setACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R(BigDecimal.ZERO);
        } else {
            BigDecimal ExRateOnPrevClosingdate = getExchangeRateOnPreviousCalculatingDate(previousCalculationPeriod);

            newEntry.setINCOMING_LD_BODY_RUB_REG_LD_3_L(
                    newEntry.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G()
                            .multiply(ExRateOnPrevClosingdate).setScale(10, RoundingMode.HALF_UP));
            newEntry.setACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R(
                    newEntry.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H()
                            .multiply(ExRateOnPrevClosingdate).setScale(10, RoundingMode.HALF_UP));
        }

        newEntry.setOUTCOMING_LD_BODY_REG_LD_3_M(
                newEntry.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G()
                        .multiply(ExRateOnClosingdate).setScale(10, RoundingMode.HALF_UP));

        if (newEntry.getOUTCOMING_LD_BODY_REG_LD_3_M()
                .compareTo(newEntry.getINCOMING_LD_BODY_RUB_REG_LD_3_L()) > 0) {
            newEntry.setREVAL_LD_BODY_PLUS_REG_LD_3_N(newEntry.getOUTCOMING_LD_BODY_REG_LD_3_M()
                    .subtract(newEntry.getINCOMING_LD_BODY_RUB_REG_LD_3_L()).setScale(10, RoundingMode.HALF_UP));
            newEntry.setREVAL_LD_BODY_MINUS_REG_LD_3_O(BigDecimal.ZERO);
        } else {
            newEntry.setREVAL_LD_BODY_PLUS_REG_LD_3_N(BigDecimal.ZERO);
            newEntry.setREVAL_LD_BODY_MINUS_REG_LD_3_O(newEntry.getOUTCOMING_LD_BODY_REG_LD_3_M()
                    .subtract(newEntry.getINCOMING_LD_BODY_RUB_REG_LD_3_L()).setScale(10, RoundingMode.HALF_UP));
        }

        newEntry.setACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S(
                newEntry.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J()
                        .multiply(ExRateOnClosingdate).setScale(10, RoundingMode.HALF_UP));

        if (newEntry.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S()
                .subtract(newEntry.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R())
                .subtract(newEntry.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M())
                .compareTo(BigDecimal.ZERO) > 0) {
            newEntry.setREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U(BigDecimal.ZERO);
            newEntry.setREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T(
                    newEntry.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S()
                            .subtract(
                                    newEntry.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R())
                            .subtract(newEntry.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M()).setScale(10, RoundingMode.HALF_UP));
        } else {
            newEntry.setREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U(
                    newEntry.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S()
                            .subtract(
                                    newEntry.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R())
                            .subtract(newEntry.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M()).setScale(10, RoundingMode.HALF_UP));
            newEntry.setREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T(BigDecimal.ZERO);
        }

        if (newEntry.getREVAL_LD_BODY_PLUS_REG_LD_3_N()
                .add(newEntry.getREVAL_LD_BODY_MINUS_REG_LD_3_O())
                .add(newEntry.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T())
                .add(newEntry.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U())
                .compareTo(BigDecimal.ZERO) > 0) {
            newEntry.setSUM_PLUS_FOREX_DIFF_REG_LD_3_V(newEntry.getREVAL_LD_BODY_PLUS_REG_LD_3_N()
                    .add(newEntry.getREVAL_LD_BODY_MINUS_REG_LD_3_O())
                    .add(newEntry.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T())
                    .add(newEntry.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U())
                    .negate().setScale(10, RoundingMode.HALF_UP));
            newEntry.setSUM_MINUS_FOREX_DIFF_REG_LD_3_W(BigDecimal.ZERO);
        } else {
            newEntry.setSUM_MINUS_FOREX_DIFF_REG_LD_3_W(newEntry.getREVAL_LD_BODY_PLUS_REG_LD_3_N()
                    .add(newEntry.getREVAL_LD_BODY_MINUS_REG_LD_3_O())
                    .add(newEntry.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T())
                    .add(newEntry.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U())
                    .negate().setScale(10, RoundingMode.HALF_UP));
            newEntry.setSUM_PLUS_FOREX_DIFF_REG_LD_3_V(BigDecimal.ZERO);
        }

        if (newEntry.getEnd_date_at_this_period().isEqual(calculationPeriod) || (newEntry.getEnd_date_at_this_period()
                .isBefore(calculationPeriod) && newEntry.getEnd_date_at_this_period()
                .isAfter(calculationPeriod.withDayOfMonth(1)))) {
            newEntry.setDISPOSAL_BODY_RUB_REG_LD_3_X(newEntry.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(10, RoundingMode.HALF_UP));
            newEntry.setDISPOSAL_DISCONT_RUB_REG_LD_3_Y(
                    newEntry.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(10, RoundingMode.HALF_UP));
        } else {
            newEntry.setDISPOSAL_BODY_RUB_REG_LD_3_X(BigDecimal.ZERO);
            newEntry.setDISPOSAL_DISCONT_RUB_REG_LD_3_Y(BigDecimal.ZERO);
        }

        if (calculateDurationMonthsUntilCurrentEndDate(calculationPeriod, newEntry) >= 12) {
            log.info("Депозит долгосрочный");
            newEntry.setLDTERM_REG_LD_3_Z(LeasingDepositDuration.LT);
        } else {
            log.info("Депозит краткосрочный");
            newEntry.setLDTERM_REG_LD_3_Z(LeasingDepositDuration.ST);
        }

        if (newEntry.getEnd_date_at_this_period().isAfter(calculationPeriod)) {
            if (newEntry.getLDTERM_REG_LD_3_Z()
                    .equals(LeasingDepositDuration.ST)) {
                newEntry.setTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA(
                        newEntry.getOUTCOMING_LD_BODY_REG_LD_3_M().setScale(10, RoundingMode.HALF_UP));
                newEntry.setTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB(
                        newEntry.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().setScale(10, RoundingMode.HALF_UP));
            } else {
                newEntry.setTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA(BigDecimal.ZERO);
                newEntry.setTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB(BigDecimal.ZERO);
            }
        } else {
            newEntry.setTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA(BigDecimal.ZERO);
            newEntry.setTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB(BigDecimal.ZERO);
        }

        if (newEntry.getLDTERM_REG_LD_3_Z()
                .equals(LeasingDepositDuration.ST)) {
            newEntry.setADVANCE_CURRENTPERIOD_REG_LD_3_AE(
                    this.depositSumDiscountedOnFirstEndDate.multiply(
                            exchangeRateToRubAtStartDate).setScale(10, RoundingMode.HALF_UP));
        } else {
            newEntry.setADVANCE_CURRENTPERIOD_REG_LD_3_AE(BigDecimal.ZERO);
        }

        if (findLastEntry(this.leasingDepositToCalculate.getScenario(), scenarioTo,
                previousCalculationPeriod).size() >
                0) {
            Entry lde = findLastEntry(this.leasingDepositToCalculate.getScenario(),
                    scenarioTo, previousCalculationPeriod).get(0);
            newEntry.setTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC(
                    lde.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().setScale(10, RoundingMode.HALF_UP));
            newEntry.setTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD(
                    lde.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().setScale(10, RoundingMode.HALF_UP));
            newEntry.setADVANCE_PREVPERIOD_REG_LD_3_AF(lde.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().setScale(10, RoundingMode.HALF_UP));
        } else {
            newEntry.setTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC(BigDecimal.ZERO);
            newEntry.setTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD(BigDecimal.ZERO);
            newEntry.setADVANCE_PREVPERIOD_REG_LD_3_AF(BigDecimal.ZERO);
        }

//Reg.LeasingDeposit.model.LeasingDeposit.3---------------------END
    }

    private double calculateDurationMonthsUntilCurrentEndDate(LocalDate calculationPeriod, Entry newEntry) {
        return calculateDurationInDaysBetween(calculationPeriod, newEntry.getEnd_date_at_this_period()) / 30.417;
    }

    private void calculateRegLd2(LocalDate finalClosingdate, Entry newEntry, LocalDate previousClosingDate) {
        //Reg.LeasingDeposit.model.LeasingDeposit.2---------------------START
        log.info("Начинается поиск среднего курса на текущую отчетную дату");

        BigDecimal avgExRateForPeriod = this.exchangeRateRepository.getAverageRateAtDate(finalClosingdate,
                scenarioTo,
                this.leasingDepositToCalculate.getCurrency());

        log.info("Средний курс валюты текущего периода => {}", avgExRateForPeriod);

        if (isDepositDurationMoreThanOneYear()) {
            if (newEntry.getEnd_date_at_this_period()
                    .isAfter(finalClosingdate)) {
                newEntry.setACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(
                        countDiscountFromStartDateToNeededDate(
                                newEntry.getEnd_date_at_this_period(), finalClosingdate).setScale(10, RoundingMode.HALF_UP));
            } else {
                newEntry.setACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(
                        countDiscountFromStartDateToNeededDate(
                                newEntry.getEnd_date_at_this_period(),
                                newEntry.getEnd_date_at_this_period()).setScale(10,

                                RoundingMode.HALF_UP));
            }

            if (previousClosingDate.isAfter(this.leasingDepositToCalculate.getStart_date()) ||
                    previousClosingDate.isEqual(this.leasingDepositToCalculate.getStart_date())) {
                newEntry.setACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H(
                        countDiscountFromStartDateToNeededDate(
                                newEntry.getEnd_date_at_this_period(), previousClosingDate).setScale(10, RoundingMode.HALF_UP));

                List<Entry> lastEntryIn2Scenarios = new ArrayList<>();

                if (isCalculationScenariosDiffer()) {
                    if (finalClosingdate.isEqual(
                            firstOpenPeriodOfScenarioFrom)) {
                        lastEntryIn2Scenarios =
                                findLastEntry(scenarioFrom,
                                        scenarioFrom, previousClosingDate);
                    } else {
                        lastEntryIn2Scenarios =
                                findLastEntry(scenarioTo, scenarioTo, previousClosingDate);
                    }
                } else {
                    lastEntryIn2Scenarios =
                            findLastEntry(scenarioTo, scenarioTo, previousClosingDate);
                }

                if (lastEntryIn2Scenarios.size() > 0) {
                    if (lastEntryIn2Scenarios.get(0)
                            .getEnd_date_at_this_period()
                            .isEqual(newEntry.getEnd_date_at_this_period())) {
                        newEntry.setACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(
                                lastEntryIn2Scenarios.get(0)
                                        .getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(10, RoundingMode.HALF_UP));
                    } else {
                        BigDecimal accumulatedDiscountRUB = BigDecimal.ZERO;

                        if (isCalculationScenariosDiffer()) {
                            accumulatedDiscountRUB = calculateAccumDiscountRUB_RegLD2(
                                    this.depositLastDayOfFirstMonth,
                                    firstOpenPeriodOfScenarioFrom, scenarioFrom, newEntry);

                            if (!firstOpenPeriodOfScenarioFrom.isEqual(finalClosingdate)) {
                                accumulatedDiscountRUB = accumulatedDiscountRUB.add(
                                        calculateAccumDiscountRUB_RegLD2(
                                                firstOpenPeriodOfScenarioFrom
                                                , finalClosingdate, scenarioTo, newEntry));
                            }
                        } else {
                            accumulatedDiscountRUB = calculateAccumDiscountRUB_RegLD2(
                                    this.depositLastDayOfFirstMonth,
                                    finalClosingdate, scenarioTo, newEntry);
                        }

                        newEntry.setACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(
                                accumulatedDiscountRUB.setScale(10, RoundingMode.HALF_UP));
                    }
                }
            } else {
                newEntry.setACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H(BigDecimal.ZERO);
                newEntry.setACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(BigDecimal.ZERO);
            }
        } else {
            newEntry.setACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(BigDecimal.ZERO);
            newEntry.setACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N(BigDecimal.ZERO);
            newEntry.setACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H(BigDecimal.ZERO);
            newEntry.setACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(BigDecimal.ZERO);
        }

        newEntry.setAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I(
                newEntry.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J()
                        .subtract(newEntry.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H()).setScale(10, RoundingMode.HALF_UP));
        newEntry.setAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M(
                newEntry.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I()
                        .multiply(avgExRateForPeriod).setScale(10, RoundingMode.HALF_UP));

        newEntry.setACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N(
                newEntry.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K()
                        .add(newEntry.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M()).setScale(10, RoundingMode.HALF_UP));
        //Reg.LeasingDeposit.model.LeasingDeposit.2---------------------END
    }

    private BigDecimal getExchangeRateOnPreviousCalculatingDate(LocalDate prevClosingDate) {
        log.info("Начинается расчет курса на прошлую дату");

        BigDecimal curExOnPrevClosingDate;
        if (isCalculationScenariosDiffer()) {
            if (prevClosingDate.isBefore(firstOpenPeriodOfScenarioFrom)) {
                curExOnPrevClosingDate = this.exchangeRateRepository.getRateAtDate(prevClosingDate,
                        scenarioFrom,
                        this.leasingDepositToCalculate.getCurrency());
            } else {
                curExOnPrevClosingDate = this.exchangeRateRepository.getRateAtDate(prevClosingDate,
                        scenarioTo,
                        this.leasingDepositToCalculate.getCurrency());
            }
        } else {
            curExOnPrevClosingDate = this.exchangeRateRepository.getRateAtDate(prevClosingDate,
                    scenarioTo,
                    this.leasingDepositToCalculate.getCurrency());
        }
        return curExOnPrevClosingDate;
    }

    private BigDecimal calculateDiscountedValueFromStartDateToNeededDate(LocalDate endDate, LocalDate neededDate) {
        BigDecimal discountedNominalValue = discountNominalValueUntilStartDateFromDate(endDate);
        return increaseDiscountedNominalValueUntilNeededDate(discountedNominalValue, neededDate);
    }

    private BigDecimal increaseDiscountedNominalValueUntilNeededDate(BigDecimal discountedNominalValue, LocalDate neededDate) {
        int ldDurationFromStartToNeededDays = calculateDurationInDaysBetween(this.leasingDepositToCalculate.getStart_date(), neededDate);

        return discountedNominalValue.multiply(BigDecimal.ONE.add(percentPerDay).pow(ldDurationFromStartToNeededDays));
    }

    private BigDecimal discountNominalValueUntilStartDateFromDate(LocalDate endDate) {
        BigDecimal discountNominalValue = BigDecimal.ZERO;

        int ldDurationDays = calculateDurationInDaysBetween(this.leasingDepositToCalculate.getStart_date(), endDate);

        discountNominalValue = nominalDepositSum
                .setScale(32)
                .divide(BigDecimal.ONE.add(percentPerDay).pow(ldDurationDays),
                        RoundingMode.HALF_UP);

        return discountNominalValue;
    }

    private BigDecimal countDiscountFromStartDateToNeededDate(LocalDate endDate, LocalDate neededDate) {
        return calculateDiscountedValueFromStartDateToNeededDate(endDate, neededDate).subtract(
                calculateDiscountedValueFromStartDateToNeededDate(endDate, this.leasingDepositToCalculate.getStart_date()));
    }

    private List<Entry> findLastEntry(Scenario scenarioFrom, Scenario scenarioTo, LocalDate Date) {
        List<Entry> LastEntry = calculatedAndExistingBeforeCalculationEntries.stream()
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

    private BigDecimal findLastCalculatedDiscount(Scenario scenarioWhereFind, LocalDate finalClosingdate) {
        BigDecimal LastCalculatedDiscount = BigDecimal.ZERO;

        TreeMap<LocalDate, BigDecimal> tmZDT_BD = calculatedAndExistingBeforeCalculationEntries.stream()
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

    private BigDecimal findLastRevaluationOfDiscount(Scenario scenarioTo, LocalDate finalClosingdate) {
        BigDecimal LastRevaluation = BigDecimal.ZERO;

        TreeMap<LocalDate, BigDecimal> tmZDT_BD = calculatedAndExistingBeforeCalculationEntries.stream()
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