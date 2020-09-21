package LD.service.Calculators.LeasingDeposits;

import LD.model.DepositRate.DepositRate;
import LD.model.EndDate.EndDate;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.Scenario.Scenario;
import LD.repository.DepositRatesRepository;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;

import static java.util.Objects.isNull;

@Getter
@Log4j2
public class SupportEntryCalculator {

    private TreeMap<LocalDate, LocalDate> mappingPeriodEndDate = new TreeMap<>();
    private LeasingDeposit leasingDepositToCalculate;
    private Scenario scenarioFrom;
    private Scenario scenarioTo;
    private LocalDate depositLastDayOfFirstMonth;
    private LocalDate firstEndDate;
    private LocalDate firstOpenPeriod;
    private LocalDate dateUntilThatEntriesMustBeCalculated;
    private int depositDurationDays;
    private int depositDurationMonths;
    private DepositRatesRepository depositRatesRepository;
    private BigDecimal depositYearRate;
    private BigDecimal depositDayRate;
    final int MONTHS_IN_YEAR = 12;
    final int DAYS_IN_YEAR = 365;

    public static SupportEntryCalculator calculateDateUntilThatEntriesMustBeCalculated(LeasingDeposit leasingDepositToCalculate,
                                                                                       Scenario scenarioTo,
                                                                                       DepositRatesRepository depositRatesRepository,
                                                                                       LocalDate firstOpenPeriod) {
        return new SupportEntryCalculator(leasingDepositToCalculate, scenarioTo, depositRatesRepository, firstOpenPeriod);
    }

    private SupportEntryCalculator(LeasingDeposit leasingDepositToCalculate, Scenario scenarioTo,
                                   DepositRatesRepository depositRatesRepository, LocalDate firstOpenPeriod) {
        this.leasingDepositToCalculate = leasingDepositToCalculate;
        this.scenarioTo = scenarioTo;
        this.scenarioFrom = leasingDepositToCalculate.getScenario();
        this.depositRatesRepository = depositRatesRepository;
        this.firstOpenPeriod = firstOpenPeriod;

        createMappingUtcPeriodEndDateForScenariosFromTo();
        calculateDepositLastDayOfFirstMonth();
        getFirstEndData();
        calculateDepositDurationDays();
        calculateDepositDurationMonths();
        getDepositRateOrThrowExceptionWhenZeroOrMoreThanOneRate();
        calculateDateUntilThatEntriesMustBeCalculated();
        calculateFirstDayOfNextMonthForDateUntilThatEntriesMustBeCalculated();
        transformYearPercentIntoDayPercent();
    }

    /*
     * Функция работает по предпосылкам:
     * 1. Сценарий-получатель (scenarioTo) имеет приоритет по датам завершения на сценарием-источником
     * 2. Сценарий-источник не имеет дат конца после первого открытого периода
     */
    private void createMappingUtcPeriodEndDateForScenariosFromTo() {
        for (EndDate endDate : leasingDepositToCalculate.getEnd_dates()) {
            if (isRelateScenariosFromTo(endDate)) {
                if (isMappingNotExists(endDate)) {
                    addIntoMapping(endDate);
                } else {
                    if (checkIfEndDateRelateScenarioTo(endDate)) {
                        addIntoMapping(endDate);
                    }
                }
            }
        }
    }

    private boolean isRelateScenariosFromTo(EndDate endDate) {
        return endDate.getEndDateID().getScenario().equals(scenarioFrom) ||
                endDate.getEndDateID().getScenario().equals(scenarioTo);
    }

    private boolean isMappingNotExists(EndDate endDate) {
        LocalDate findDate = endDate.getEndDateID()
                .getPeriod()
                .getDate();

        return !mappingPeriodEndDate.containsKey(findDate);
    }

    private boolean checkIfEndDateRelateScenarioTo(EndDate endDate) {
        return endDate.getEndDateID()
                .getScenario()
                .equals(scenarioTo);
    }

    private void addIntoMapping(EndDate endDate) {
        LocalDate periodDate = endDate.getEndDateID()
                .getPeriod()
                .getDate();

        LocalDate endDateIntoMapping = endDate.getEndDate();

        mappingPeriodEndDate.put(periodDate, endDateIntoMapping);
    }

    private void calculateDepositLastDayOfFirstMonth() {
        this.depositLastDayOfFirstMonth =
                this.leasingDepositToCalculate.getStart_date()
                        .withDayOfMonth(1)
                        .plusMonths(1)
                        .minusDays(1);
    }

    private void getFirstEndData() {
        firstEndDate = mappingPeriodEndDate.get(depositLastDayOfFirstMonth);

        if (isNull(firstEndDate)) {
            throw new IllegalArgumentException(
                    "There no ONE end date for " + depositLastDayOfFirstMonth);
        }
    }

    private void calculateDepositDurationDays() {
        LocalDate start_date = this.leasingDepositToCalculate.getStart_date();
        this.depositDurationDays = calculateDurationInDaysBetween(start_date, firstEndDate);
    }

    public static int calculateDurationInDaysBetween(LocalDate dateFrom, LocalDate dateTo) {
        return (int) (dateTo.toEpochDay() - dateFrom.toEpochDay());
    }

    private void calculateDepositDurationMonths() {
        int LDdurationMonths = (int) Math.round(depositDurationDays / ((double) DAYS_IN_YEAR / (double) MONTHS_IN_YEAR));

        this.depositDurationMonths = LDdurationMonths;
    }

    private void getDepositRateOrThrowExceptionWhenZeroOrMoreThanOneRate() {
        depositYearRate = findDepositRatesByParametersOfDeposit();
        log.info("Ставка годовая равна: {}", depositDayRate);
    }

    private BigDecimal findDepositRatesByParametersOfDeposit() {
        return depositRatesRepository.getRateByCompanyMonthDurationCurrencyStartDateScenario(leasingDepositToCalculate.getCompany(),
                this.depositDurationMonths, leasingDepositToCalculate.getCurrency(),
                leasingDepositToCalculate.getStart_date(),
                leasingDepositToCalculate.getScenario());
    }


    private boolean isDepositRatesSizeNotEqualToOne(List<DepositRate> depositRate) {
        int REQUIRED_SIZE_DEPOSIT_RATES_FOR_LEASING_DEPOSIT = 1;
        return depositRate.size() != REQUIRED_SIZE_DEPOSIT_RATES_FOR_LEASING_DEPOSIT;
    }

    private void throwException() {
        throw new IllegalArgumentException("There is no ONE rate for " + "\n" +
                "company = " + this.leasingDepositToCalculate.getCompany().getCode() + "\n" +
                "for date = " + this.leasingDepositToCalculate.getStart_date() + "\n" +
                "for currency = " + this.leasingDepositToCalculate.getCurrency().getShort_name() + "\n" +
                "for duration = " + this.depositDurationMonths);
    }

    private void calculateDateUntilThatEntriesMustBeCalculated() {
        LocalDate endDateForFirstOpenPeriod = getEndDateForFirstOpenPeriodFromMapping();

        if (isEndDateForFirstOpenPeriodIsAfterFirstOpenPeriod(endDateForFirstOpenPeriod)) {
            dateUntilThatEntriesMustBeCalculated = firstOpenPeriod;
        } else {
            dateUntilThatEntriesMustBeCalculated = endDateForFirstOpenPeriod;
        }
    }

    private LocalDate getEndDateForFirstOpenPeriodFromMapping() {
        return this.mappingPeriodEndDate.floorEntry(firstOpenPeriod).getValue();
    }

    private boolean isEndDateForFirstOpenPeriodIsAfterFirstOpenPeriod(LocalDate endDateForFirstOpenPeriod) {
        return endDateForFirstOpenPeriod.isAfter(firstOpenPeriod);
    }

    private LocalDate transformIntoFirstDayOfNextMonth(LocalDate date) {
        return date
                .plusMonths(1)
                .withDayOfMonth(1);
    }

    private void calculateFirstDayOfNextMonthForDateUntilThatEntriesMustBeCalculated() {
        dateUntilThatEntriesMustBeCalculated = transformIntoFirstDayOfNextMonth(dateUntilThatEntriesMustBeCalculated);
    }

    private void transformYearPercentIntoDayPercent() {
        depositDayRate = calculateDayRateWithExtraOne().subtract(BigDecimal.ONE);
    }

    private BigDecimal calculateDayRateWithExtraOne() {
        double base = divideBy100(depositYearRate).add(BigDecimal.ONE).doubleValue();
        double power = (double) 1 / (double) DAYS_IN_YEAR;
        double basePowered = StrictMath.pow(base, power);

        return BigDecimal.valueOf(basePowered).setScale(32, RoundingMode.UP);
    }

    private BigDecimal divideBy100(BigDecimal number) {
        return number.divide(BigDecimal.valueOf(100));
    }

    public boolean isDurationMoreThanOneYear() {
        return this.depositDurationDays > DAYS_IN_YEAR;
    }
}