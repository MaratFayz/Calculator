package LD.service.Calculators.LeasingDeposits;

import LD.model.DepositRate.DepositRate;
import LD.model.DepositRate.DepositRateID_;
import LD.model.DepositRate.DepositRate_;
import LD.model.Duration.Duration_;
import LD.model.EndDate.EndDate;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.Scenario.Scenario;
import LD.repository.DepositRatesRepository;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TreeMap;

import static java.util.Objects.isNull;

@Getter
@Log4j2
public class SupportEntryCalculator {

    private TreeMap<ZonedDateTime, ZonedDateTime> mappingPeriodEndDate = new TreeMap<>();
    private LeasingDeposit leasingDepositToCalculate;
    private Scenario scenarioFrom;
    private Scenario scenarioTo;
    private ZonedDateTime depositLastDayOfFirstMonth;
    private ZonedDateTime firstEndDate;
    private ZonedDateTime firstOpenPeriod;
    private ZonedDateTime dateUntilThatEntriesMustBeCalculated;
    private int depositDurationDays;
    private int depositDurationMonths;
    private DepositRatesRepository depositRatesRepository;
    private BigDecimal depositYearRate;
    private BigDecimal depositDayRate;
    final int MONTHS_IN_YEAR = 12;
    final int DAYS_IN_YEAR = 365;

    public static SupportEntryCalculator calculateDateUntilThatEntriesMustBeCalculated(LeasingDeposit leasingDepositToCalculate, Scenario scenarioTo,
                                                                                       DepositRatesRepository depositRatesRepository, ZonedDateTime firstOpenPeriod) {
        return new SupportEntryCalculator(leasingDepositToCalculate, scenarioTo, depositRatesRepository, firstOpenPeriod);
    }

    private SupportEntryCalculator(LeasingDeposit leasingDepositToCalculate, Scenario scenarioTo,
                                   DepositRatesRepository depositRatesRepository, ZonedDateTime firstOpenPeriod) {
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
                if

                (isMappingNotExists(endDate)) {
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
        ZonedDateTime findDateUtc = transformToUtc(endDate.getEndDateID()
                .getPeriod()
                .getDate());

        return !mappingPeriodEndDate.containsKey(findDateUtc);
    }

    private ZonedDateTime transformToUtc(ZonedDateTime date) {
        return date.withZoneSameInstant(ZoneId.of("UTC"));
    }

    private boolean checkIfEndDateRelateScenarioTo(EndDate endDate) {
        return endDate.getEndDateID()
                .getScenario()
                .equals(scenarioTo);
    }

    private void addIntoMapping(EndDate endDate) {
        ZonedDateTime periodDateUtc = transformToUtc(endDate.getEndDateID()
                .getPeriod()
                .getDate());

        ZonedDateTime endDateUtc = transformToUtc(endDate.getEndDate());

        mappingPeriodEndDate.put(periodDateUtc, endDateUtc);
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
        ZonedDateTime start_date = this.leasingDepositToCalculate.getStart_date();
        this.depositDurationDays = (int) Duration.between(start_date, this.firstEndDate).toDays();
    }

    private void calculateDepositDurationMonths() {
        int LDdurationMonths = (int) Math.round(
                depositDurationDays / ((double) DAYS_IN_YEAR / (double) MONTHS_IN_YEAR));

        this.depositDurationMonths = LDdurationMonths;
    }

    private void getDepositRateOrThrowExceptionWhenZeroOrMoreThanOneRate() {
        List<DepositRate> depositRate = findDepositRatesByParametersOfDeposit();

        if (isDepositRatesSizeNotEqualToOne(depositRate)) {
            throwException();
        }

        depositYearRate = depositRate.get(0).getRATE();
        log.info("Ставка депозита = {}", depositYearRate);
    }

    private List<DepositRate> findDepositRatesByParametersOfDeposit() {
        return depositRatesRepository.findAll(equalToDepositParameters(leasingDepositToCalculate, this.depositDurationMonths));
    }

    public static Specification<DepositRate> equalToDepositParameters(LeasingDeposit leasingDepositToCalculate, int durationOfLDInMonth) {
        return (Specification<DepositRate>) (depositRatesRoot, query, cb) -> {
            return cb.and(cb.equal(depositRatesRoot.get(DepositRate_.depositRateID).get(DepositRateID_.COMPANY), leasingDepositToCalculate.getCompany()),
                    cb.lessThanOrEqualTo(depositRatesRoot.get(DepositRate_.depositRateID).get(DepositRateID_.S_TA_RT__PE_RI_OD), leasingDepositToCalculate.getStart_date()),
                    cb.greaterThanOrEqualTo(depositRatesRoot.get(DepositRate_.depositRateID).get(DepositRateID_.E_ND__PE_RI_OD), leasingDepositToCalculate.getStart_date()),
                    cb.equal(depositRatesRoot.get(DepositRate_.depositRateID).get(DepositRateID_.CURRENCY), leasingDepositToCalculate.getCurrency()),
                    cb.lessThanOrEqualTo(depositRatesRoot.get(DepositRate_.depositRateID).get(DepositRateID_.DURATION).get(Duration_.M_IN__MO_NT_H), durationOfLDInMonth),
                    cb.greaterThanOrEqualTo(depositRatesRoot.get(DepositRate_.depositRateID).get(DepositRateID_.DURATION).get(Duration_.M_AX__MO_NT_H), durationOfLDInMonth),
                    cb.equal(depositRatesRoot.get(DepositRate_.depositRateID).get(DepositRateID_.SCENARIO), leasingDepositToCalculate.getScenario()));
        };
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
        ZonedDateTime endDateForFirstOpenPeriod = getEndDateForFirstOpenPeriodFromMapping();

        if (isEndDateForFirstOpenPeriodIsAfterFirstOpenPeriod(endDateForFirstOpenPeriod)) {
            dateUntilThatEntriesMustBeCalculated = firstOpenPeriod;
        } else {
            dateUntilThatEntriesMustBeCalculated = endDateForFirstOpenPeriod;
        }
    }

    private ZonedDateTime getEndDateForFirstOpenPeriodFromMapping() {
        return this.mappingPeriodEndDate.floorEntry(firstOpenPeriod).getValue();
    }

    private boolean isEndDateForFirstOpenPeriodIsAfterFirstOpenPeriod(ZonedDateTime endDateForFirstOpenPeriod) {
        return endDateForFirstOpenPeriod.isAfter(firstOpenPeriod);
    }

    private ZonedDateTime transformIntoUtcFirstDayOfNextMonth(ZonedDateTime date) {
        return date.toLocalDate()
                .plusMonths(1)
                .withDayOfMonth(1)
                .atStartOfDay(ZoneId.of("UTC"));
    }

    private void calculateFirstDayOfNextMonthForDateUntilThatEntriesMustBeCalculated() {
        dateUntilThatEntriesMustBeCalculated = transformIntoUtcFirstDayOfNextMonth(dateUntilThatEntriesMustBeCalculated);
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