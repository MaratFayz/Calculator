package LD.service.Calculators.LeasingDeposits;

import LD.model.Entry.Entry;
import LD.model.Entry.EntryID;
import LD.model.Enums.*;
import LD.model.ExchangeRate.ExchangeRate;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.Period.Period;
import LD.model.Scenario.Scenario;
import LD.repository.DepositRatesRepository;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Data
@Log4j2
public class EntryCalculator implements Callable<List<Entry>> {

    final int numberDaysInYear = 365;
    Comparator<ZonedDateTime> ZDTcomp =
            (date1, date2) -> (int) (date1.toEpochSecond() - date2.toEpochSecond());
    ArrayList<Entry> EntriesExistingBeforeCalculating = new ArrayList<>();
    ArrayList<Entry> CalculatedStornoDeletedEntries;
    DepositRatesRepository depositRatesRepository;
    BigDecimal LDYearPercent;
    private BigDecimal deposit_sum_discounted_on_firstEndDate;
    private ZonedDateTime firstEndDate;
    private BigDecimal percentPerDay;
    private int LDdurationDays;
    private int LDdurationMonths;
    private ZonedDateTime FirstPeriodWithoutTransactionUTC;
    private ZonedDateTime min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Day;
    private TreeMap<ZonedDateTime, ZonedDateTime> mappingPeriodEndDate;
    private LocalDate depositLastDayOfFirstMonth;
    private GeneralDataKeeper GeneralDataKeeper;
    private Scenario scenarioTo;
    private LeasingDeposit leasingDepositToCalculate;

    public EntryCalculator(LeasingDeposit leasingDepositToCalculate,
                           GeneralDataKeeper GeneralDataKeeper,
                           DepositRatesRepository depositRatesRepository) {
        this.GeneralDataKeeper = GeneralDataKeeper;
        this.scenarioTo = this.GeneralDataKeeper.getTo();
        this.leasingDepositToCalculate = leasingDepositToCalculate;
        this.depositRatesRepository = depositRatesRepository;
    }

    @Override
    public List<Entry> call() {
        List<Entry> result = new ArrayList<>();

        log.info("Начинается расчет транзакций в калькуляторе");
        result = this.calculate(GeneralDataKeeper.getFirstOpenPeriod_ScenarioTo());

        log.info("Расчет калькулятора завершен. Результат = {}", result);
        return result;
    }

    public List<Entry> calculate(ZonedDateTime firstOpenPeriod) {
        if (isDepositAlreadyHasEntries()) {
            copyEntries();
        }

        if (isDepositDeleted()) {
            log.trace("Депозит является удалённым");
            setDeleteStatusToExistingEntries();
        } else {
            log.trace("Депозит не является удалённым");

            SupportEntryCalculator supportData = SupportEntryCalculator.calculateDateUntilThatEntriesMustBeCalculated(this.leasingDepositToCalculate,
                    this.scenarioTo, this.depositRatesRepository, firstOpenPeriod);

            this.mappingPeriodEndDate = supportData.getMappingPeriodEndDate();

            this.depositLastDayOfFirstMonth = supportData.getDepositLastDayOfFirstMonth().toLocalDate();
            log.info("Первый период депозита => {}", this.depositLastDayOfFirstMonth);

            this.firstEndDate = supportData.getFirstEndDate();
            log.info("this.firstEndDate => {}", this.firstEndDate);

            this.setLDdurationDays(supportData.getDepositDurationDays());
            log.info("this.getLDdurationDays() => {}", this.getLDdurationDays());

            this.setLDdurationMonths(supportData.getDepositDurationMonths());
            log.info("this.getLDdurationMonths() => {}", this.getLDdurationMonths());

            log.info("Начало расчета ставки депозита");
            this.LDYearPercent = supportData.getDepositYearRate();
            log.info("Конец расчета ставки депозита. Ставка равна = {}", LDYearPercent);

            this.min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Day =
                    this.countActualClosingDateForTheFirstOpenPeriod(firstOpenPeriod);
            log.info("min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Day = {}",
                    min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Day);

            this.percentPerDay = getPercentPerDay(LDYearPercent);

            if (this.getLDdurationDays() > numberDaysInYear) {
                deposit_sum_discounted_on_firstEndDate =
                        countDiscountedValueFromStartDateToNeededDate(this.firstEndDate,
                                this.leasingDepositToCalculate.getStart_date());
            } else {
                deposit_sum_discounted_on_firstEndDate =
                        this.leasingDepositToCalculate.getDeposit_sum_not_disc();
            }

            log.info("deposit_sum_discounted_on_firstEndDate = {}",
                    deposit_sum_discounted_on_firstEndDate);

            stornoExistingEntries();

            //дата не учитывает случай, если сначала не было транзакций, потом были, потом снова нет.
            //предположение: транзакции есть всегда сначала
            this.FirstPeriodWithoutTransactionUTC = this.countFirstPeriodWithoutTransaction(
                    this.leasingDepositToCalculate.getScenario(), GeneralDataKeeper.getTo(),
                    EntriesExistingBeforeCalculating);
            log.info("FirstPeriodWithoutTransactionUTC = {}", FirstPeriodWithoutTransactionUTC);

            CalculatedStornoDeletedEntries.addAll(
                    countTransactionsForLD(EntriesExistingBeforeCalculating,
                            GeneralDataKeeper.getAllPeriods(), this.scenarioTo,
                            GeneralDataKeeper.getAllExRates()));
        }

        return CalculatedStornoDeletedEntries;
    }

    private void copyEntries() {
        EntriesExistingBeforeCalculating.addAll(this.leasingDepositToCalculate.getEntries());
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
        CalculatedStornoDeletedEntries =
                changeStatusInLastEntries(EntriesExistingBeforeCalculating, scenarioTo,
                        EntryStatus.DELETED);
    }

    private void stornoExistingEntries() {
        CalculatedStornoDeletedEntries =
                changeStatusInLastEntries(EntriesExistingBeforeCalculating, this.scenarioTo,
                        EntryStatus.STORNO);
    }

    private BigDecimal getPercentPerDay(BigDecimal yearPercent) {
        BigDecimal percentPerDay = BigDecimal.valueOf(StrictMath.pow(
                yearPercent.divide(BigDecimal.valueOf(100))
                        .add(BigDecimal.ONE)
                        .doubleValue(), (double) 1 / (double) numberDaysInYear))
                .setScale(32, RoundingMode.UP)
                .subtract(BigDecimal.ONE);
        return percentPerDay;
    }

    private ArrayList<Entry> changeStatusInLastEntries(List<Entry> EntriesExistingBeforeCalculating,
                                                       Scenario scenarioWhereToChangeStatusInEntries,
                                                       EntryStatus newStatus) {
        ArrayList<Entry> DeletedStornoEntries = new ArrayList<>();

        List<Entry> stream_ActualEntries = EntriesExistingBeforeCalculating.stream()
                .filter(transaction -> {
                    if (transaction.getStatus()
                            .equals(EntryStatus.ACTUAL)) {
                        return true;
                    } else {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        if (newStatus == EntryStatus.STORNO) {
            stream_ActualEntries = stream_ActualEntries.stream()
                    .filter(transaction -> {
                        if (transaction.getEntryID()
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
                        .filter(transaction -> transaction.getEntryID()
                                .getPeriod()
                                .getDate()
                                .toLocalDate()
                                .equals(GeneralDataKeeper.getFirstOpenPeriod_ScenarioTo()
                                        .toLocalDate()))
                        .collect(Collectors.toList());
            }

        }

        stream_ActualEntries.forEach(transaction -> {
            transaction.setStatus(newStatus);
            DeletedStornoEntries.add(transaction);
        });

        return DeletedStornoEntries;
    }

    private List<Entry> countTransactionsForLD(List<Entry> EntriesExistingBeforeCalculating,
                                               List<Period> periods, Scenario scSAVE,
                                               List<ExchangeRate> allExRates) {
        ArrayList<Entry> OnlyCalculatedEntries = new ArrayList<>();
        ArrayList<Entry> CalculatedAndExistingBeforeCalculationEntries = new ArrayList<>();
        CalculatedAndExistingBeforeCalculationEntries.addAll(EntriesExistingBeforeCalculating);

        LocalDate firstPeriodWithoutTransaction =
                this.FirstPeriodWithoutTransactionUTC.toLocalDate();
        LocalDate min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month_InDays =
                min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Day.toLocalDate();

        BigDecimal exRateAtStartDate = allExRates.stream()
                .filter(er -> er.getExchangeRateID()
                        .getDate()
                        .isEqual(this.leasingDepositToCalculate.getStart_date().withZoneSameLocal(ZoneId.of("UTC"))))
                .filter(er -> er.getExchangeRateID()
                        .getCurrency()
                        .equals(this.leasingDepositToCalculate.getCurrency()))
                /*
                 * предполагается, что на момент расчета на другой сценарий сохранения (не равный источнику)
                 * все транзакции в сценарии-источнике уже будут рассчитаны
                 */
                .filter(er -> er.getExchangeRateID()
                        .getScenario()
                        .equals(this.leasingDepositToCalculate.getScenario()))
                .map(er -> er.getRate_at_date())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Не найден курс на начальную дату жизни депозита => " +
                                this.leasingDepositToCalculate.getStart_date()));

        //Для случаев, когда все транзакции сделаны => чтоб не было новых
        if (firstPeriodWithoutTransaction.isBefore(min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month_InDays)) {
            log.info(
                    "firstPeriodWithoutTransaction.datesUntil(min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month_InDays, java.time.Period.ofMonths(1)).collect(Collectors.toList()) = {}",
                    firstPeriodWithoutTransaction.datesUntil(
                            min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month_InDays,
                            java.time.Period.ofMonths(1))
                            .collect(Collectors.toList()));
            for (LocalDate closingdate : firstPeriodWithoutTransaction.datesUntil(
                    min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month_InDays,
                    java.time.Period.ofMonths(1))
                    .collect(Collectors.toList())) {
                log.info("Расчет периода с датой (до коррекции на последнюю дату) => {}",
                        closingdate);
                closingdate = closingdate.withDayOfMonth(closingdate.lengthOfMonth());
                ZonedDateTime finalClosingdate =
                        ZonedDateTime.of(closingdate, LocalTime.MIDNIGHT, ZoneId.of("UTC"));
                log.info("Расчет периода с датой (после коррекции на последнюю дату) => {}",
                        finalClosingdate);

                //TODO: уницифировать контроллер (в контроллере присваивается отрицательное значение)
                // и эту проверку (здесь идет проверка на not null)
                if (!GeneralDataKeeper.getFrom().equals(GeneralDataKeeper.getTo())) {
                    if (!this.GeneralDataKeeper.getPeriod_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo().isEqual(ZonedDateTime.of(LocalDateTime.MIN, ZoneId.of("UTC")))) {
                        if ((closingdate.isEqual(GeneralDataKeeper.getPeriod_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo().toLocalDate()) ||
                                closingdate.isAfter(GeneralDataKeeper.getPeriod_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo().toLocalDate())) &&
                                closingdate.isBefore(GeneralDataKeeper.getFirstOpenPeriod_ScenarioFrom().toLocalDate())) {
                            log.info("Осуществляется копирование со сценария {} на сценарий {}",
                                    GeneralDataKeeper.getFrom()
                                            .getName(), GeneralDataKeeper.getTo()
                                            .getName());

                            LocalDate finalClosingdate1 = closingdate;
                            List<Entry> L_entryTocopy = this.leasingDepositToCalculate.getEntries()
                                    .stream()
                                    .filter(entry -> entry.getEntryID()
                                            .getScenario()
                                            .equals(GeneralDataKeeper.getFrom()))
                                    .collect(Collectors.toList());

                            L_entryTocopy = L_entryTocopy.stream()
                                    .filter(entry -> entry.getEntryID()
                                            .getPeriod()
                                            .getDate()
                                            .toLocalDate()
                                            .isEqual(finalClosingdate1))
                                    .collect(Collectors.toList());

                            Entry entryToCopy = L_entryTocopy.get(0);

                            EntryID newEntryID = entryToCopy.getEntryID()
                                    .toBuilder()
                                    .scenario(GeneralDataKeeper.getTo())
                                    .CALCULATION_TIME(ZonedDateTime.now())
                                    .build();

                            Entry newEntry = entryToCopy.toBuilder()
                                    .entryID(newEntryID)
                                    .build();
                            this.CalculatedStornoDeletedEntries.add(newEntry);

                            continue;
                        }

                    }
                }

                EntryID entryID = EntryID.builder()
                        .leasingDeposit_id(this.leasingDepositToCalculate.getId())
                        .CALCULATION_TIME(ZonedDateTime.now())
                        .period(periods.stream()
                                .filter(period -> period.getDate()
                                        .isEqual(finalClosingdate))
                                .collect(Collectors.toList())
                                .get(0))
                        .scenario(scSAVE)
                        .build();

                log.info("Получен ключ транзакции => {}", entryID);

                Entry t = new Entry();
                t.setLastChange(entryID.getCALCULATION_TIME());

                if (finalClosingdate.isBefore(GeneralDataKeeper.getFirstOpenPeriod_ScenarioTo())) {
                    t.setStatus_EntryMadeDuringOrAfterClosedPeriod(
                            EntryPeriodCreation.AFTER_CLOSING_PERIOD);
                } else {
                    t.setStatus_EntryMadeDuringOrAfterClosedPeriod(
                            EntryPeriodCreation.CURRENT_PERIOD);
                }

                t.setUser(this.GeneralDataKeeper.getUser());
                t.setLeasingDeposit(this.leasingDepositToCalculate);
                t.setEntryID(entryID);
                t.setEnd_date_at_this_period(
                        this.mappingPeriodEndDate.floorEntry(finalClosingdate)
                                .getValue());
                t.setStatus(EntryStatus.ACTUAL);
                t.setPercentRateForPeriodForLD(this.LDYearPercent.setScale(10, RoundingMode.HALF_UP));
                t.setDISCONT_AT_START_DATE_cur_REG_LD_1_K(
                        this.deposit_sum_discounted_on_firstEndDate.subtract(
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

                ZonedDateTime PrevClosingDate = finalClosingdate.minusMonths(1)
                        .withDayOfMonth(finalClosingdate.minusMonths(1)
                                .toLocalDate()
                                .lengthOfMonth());
                log.info("Предыдущая дата закрытия => {}", PrevClosingDate);

                if (PrevClosingDate.isAfter(
                        this.depositLastDayOfFirstMonth.atStartOfDay(ZoneId.of("UTC"))) &&
                        !this.mappingPeriodEndDate.floorEntry(PrevClosingDate)
                                .getValue()
                                .isEqual(t.getEnd_date_at_this_period())) {
                    BigDecimal deposit_sum_discounted_on_End_date_at_this_period = BigDecimal.ZERO;

                    if (this.getLDdurationDays() > 365) {
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

                    if (this.getLDdurationDays() > 365) {
                        //Поиск последнего периода с суммой в поле корректировки дисконта в рублях
                        BigDecimal lastRevaluationOfDiscount = BigDecimal.ZERO;

                        if (!GeneralDataKeeper.getFrom()
                                .equals(GeneralDataKeeper.getTo())) {
                            if (PrevClosingDate.isBefore(
                                    GeneralDataKeeper.getFirstOpenPeriod_ScenarioFrom())) {
                                lastRevaluationOfDiscount =
                                        findLastRevaluationOfDiscount(GeneralDataKeeper.getFrom(),
                                                finalClosingdate,
                                                CalculatedAndExistingBeforeCalculationEntries);
                            } else {
                                ZonedDateTime prevDateBeforeFirstOpenPeriodForScenarioFrom =
                                        GeneralDataKeeper.getFirstOpenPeriod_ScenarioFrom()
                                                .withDayOfMonth(1)
                                                .minusDays(1);
                                BigDecimal
                                        lastCalculatedDiscountForScenarioFromOnDateBeforeFirstOpenPeriod1 =
                                        findLastRevaluationOfDiscount(GeneralDataKeeper.getFrom(),
                                                prevDateBeforeFirstOpenPeriodForScenarioFrom,
                                                CalculatedAndExistingBeforeCalculationEntries);

                                BigDecimal lastCalculatedDiscountForScenarioTo =
                                        findLastRevaluationOfDiscount(scSAVE, finalClosingdate,
                                                CalculatedAndExistingBeforeCalculationEntries);

                                lastRevaluationOfDiscount =
                                        lastCalculatedDiscountForScenarioTo.compareTo(
                                                BigDecimal.ZERO) != 0 ?
                                                lastCalculatedDiscountForScenarioTo :
                                                lastCalculatedDiscountForScenarioFromOnDateBeforeFirstOpenPeriod1;
                            }
                        } else {
                            lastRevaluationOfDiscount =
                                    findLastRevaluationOfDiscount(scSAVE, finalClosingdate,
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

                    if (!GeneralDataKeeper.getFrom()
                            .equals(GeneralDataKeeper.getTo())) {
                        if (PrevClosingDate.isBefore(
                                GeneralDataKeeper.getFirstOpenPeriod_ScenarioFrom())) {
                            curExOnPrevClosingDate = allExRates.stream()
                                    .filter(er -> er.getExchangeRateID()
                                            .getDate()
                                            .toLocalDate()
                                            .isEqual(PrevClosingDate.toLocalDate()))
                                    .filter(er -> er.getExchangeRateID()
                                            .getCurrency()
                                            .equals(this.leasingDepositToCalculate.getCurrency()))
                                    /*
                                     * предполагается, что на момент расчета на другой сценарий сохранения (не равный источнику)
                                     * все транзакции в сценарии-источнике уже будут рассчитаны
                                     */
                                    .filter(er -> er.getExchangeRateID()
                                            .getScenario()
                                            .equals(GeneralDataKeeper.getFrom()))
                                    .map(er -> er.getRate_at_date())
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalArgumentException(
                                            "Не найден курс на дату => " +
                                                    PrevClosingDate.toLocalDate()));
                        } else {
                            curExOnPrevClosingDate = allExRates.stream()
                                    .filter(er -> er.getExchangeRateID()
                                            .getDate()
                                            .toLocalDate()
                                            .isEqual(PrevClosingDate.toLocalDate()))
                                    .filter(er -> er.getExchangeRateID()
                                            .getCurrency()
                                            .equals(this.leasingDepositToCalculate.getCurrency()))
                                    /*
                                     * предполагается, что на момент расчета на другой сценарий сохранения (не равный источнику)
                                     * все транзакции в сценарии-источнике уже будут рассчитаны
                                     */
                                    .filter(er -> er.getExchangeRateID()
                                            .getScenario()
                                            .equals(scSAVE))
                                    .map(er -> er.getRate_at_date())
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalArgumentException(
                                            "Не найден курс на дату => " +
                                                    PrevClosingDate.toLocalDate()));
                        }
                    } else {
                        curExOnPrevClosingDate = allExRates.stream()
                                .filter(er -> er.getExchangeRateID()
                                        .getDate()
                                        .toLocalDate()
                                        .isEqual(PrevClosingDate.toLocalDate()))
                                .filter(er -> er.getExchangeRateID()
                                        .getCurrency()
                                        .equals(this.leasingDepositToCalculate.getCurrency()))
                                /*
                                 * предполагается, что на момент расчета на другой сценарий сохранения (не равный источнику)
                                 * все транзакции в сценарии-источнике уже будут рассчитаны
                                 */
                                .filter(er -> er.getExchangeRateID()
                                        .getScenario()
                                        .equals(scSAVE))
                                .map(er -> er.getRate_at_date())
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Не найден курс на дату => " +
                                                PrevClosingDate.toLocalDate()));
                    }

                    log.info("Курс валюты на конец прошлого периода => {}", curExOnPrevClosingDate);

                    BigDecimal lastCalculatedDiscount = BigDecimal.ZERO;

                    if (!GeneralDataKeeper.getFrom()
                            .equals(GeneralDataKeeper.getTo())) {
                        if (PrevClosingDate.isBefore(
                                GeneralDataKeeper.getFirstOpenPeriod_ScenarioFrom())) {
                            lastCalculatedDiscount =
                                    findLastCalculatedDiscount(GeneralDataKeeper.getFrom(),
                                            finalClosingdate,
                                            CalculatedAndExistingBeforeCalculationEntries);
                        } else {
                            ZonedDateTime prevDateBeforeFirstOpenPeriodForScenarioFrom =
                                    GeneralDataKeeper.getFirstOpenPeriod_ScenarioFrom()
                                            .withDayOfMonth(1)
                                            .minusDays(1);
                            BigDecimal
                                    lastCalculatedDiscountForScenarioFromOnDateBeforeFirstOpenPeriod1 =
                                    findLastCalculatedDiscount(GeneralDataKeeper.getFrom(),
                                            prevDateBeforeFirstOpenPeriodForScenarioFrom,
                                            CalculatedAndExistingBeforeCalculationEntries);

                            BigDecimal lastCalculatedDiscountForScenarioTo =
                                    findLastCalculatedDiscount(scSAVE, finalClosingdate,
                                            CalculatedAndExistingBeforeCalculationEntries);

                            lastCalculatedDiscount = lastCalculatedDiscountForScenarioTo.compareTo(
                                    BigDecimal.ZERO) != 0 ? lastCalculatedDiscountForScenarioTo :
                                    lastCalculatedDiscountForScenarioFromOnDateBeforeFirstOpenPeriod1;
                        }
                    } else {
                        lastCalculatedDiscount =
                                findLastCalculatedDiscount(scSAVE, finalClosingdate,
                                        CalculatedAndExistingBeforeCalculationEntries);
                    }

                    if (this.getLDdurationDays() > 365) {
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

                    ZonedDateTime endDateAtPrevClosingDate =
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

                    if (this.getLDdurationDays() > 365) {
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

                BigDecimal avgExRateForPeriod = allExRates.stream()
                        .filter(er -> er.getExchangeRateID()
                                .getDate()
                                .withZoneSameInstant(ZoneId.of("UTC"))
                                .isEqual(finalClosingdate))
                        .filter(er -> er.getExchangeRateID()
                                .getCurrency()
                                .equals(this.leasingDepositToCalculate.getCurrency()))
                        /*
                         * предполагается, что на момент расчета на другой сценарий сохранения (не равный источнику)
                         * все транзакции в сценарии-источнике уже будут рассчитаны
                         */
                        .filter(er -> er.getExchangeRateID()
                                .getScenario()
                                .equals(scSAVE))
                        .map(ExchangeRate::getAverage_rate_for_month)
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Не найден средний курс за период " + finalClosingdate));

                log.info("Средний курс валюты текущего периода => {}", avgExRateForPeriod);

                if (this.getLDdurationDays() > 365) {
                    if (t.getEnd_date_at_this_period()
                            .isAfter(finalClosingdate)) {
                        t.setACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(
                                countDiscountFromStartDateToNeededDate(
                                        t.getEnd_date_at_this_period(), finalClosingdate).setScale(10, RoundingMode.HALF_UP));
                    } else {
                        t.setACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(
                                countDiscountFromStartDateToNeededDate(
                                        t.getEnd_date_at_this_period(),
                                        t.getEnd_date_at_this_period()).setScale(10, RoundingMode.HALF_UP));
                    }

                    if (PrevClosingDate.isAfter(this.leasingDepositToCalculate.getStart_date()
                            .withZoneSameLocal(ZoneId.of("UTC"))) || PrevClosingDate.isEqual(
                            this.leasingDepositToCalculate.getStart_date()
                                    .withZoneSameLocal(ZoneId.of("UTC")))) {
                        t.setACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H(
                                countDiscountFromStartDateToNeededDate(
                                        t.getEnd_date_at_this_period(), PrevClosingDate).setScale(10, RoundingMode.HALF_UP));

                        List<Entry> lastTransactionIn2Scenarios = new ArrayList<>();

                        if (!GeneralDataKeeper.getFrom()
                                .equals(GeneralDataKeeper.getTo())) {
                            if (finalClosingdate.isEqual(
                                    GeneralDataKeeper.getFirstOpenPeriod_ScenarioFrom())) {
                                lastTransactionIn2Scenarios =
                                        findLastTransaction(GeneralDataKeeper.getFrom(),
                                                GeneralDataKeeper.getFrom(), PrevClosingDate,
                                                CalculatedAndExistingBeforeCalculationEntries);
                            } else {
                                lastTransactionIn2Scenarios =
                                        findLastTransaction(scSAVE, scSAVE, PrevClosingDate,
                                                CalculatedAndExistingBeforeCalculationEntries);
                            }
                        } else {
                            lastTransactionIn2Scenarios =
                                    findLastTransaction(scSAVE, scSAVE, PrevClosingDate,
                                            CalculatedAndExistingBeforeCalculationEntries);
                        }

                        if (lastTransactionIn2Scenarios.size() > 0) {
                            if (lastTransactionIn2Scenarios.get(0)
                                    .getEnd_date_at_this_period()
                                    .isEqual(t.getEnd_date_at_this_period())) {
                                t.setACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(
                                        lastTransactionIn2Scenarios.get(0)
                                                .getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N().setScale(10, RoundingMode.HALF_UP));
                            } else {
                                BigDecimal accumulatedDiscountRUB = BigDecimal.ZERO;

                                if (!GeneralDataKeeper.getFrom()
                                        .equals(GeneralDataKeeper.getTo())) {
                                    accumulatedDiscountRUB = calculateAccumDiscountRUB_RegLD2(
                                            this.depositLastDayOfFirstMonth,
                                            GeneralDataKeeper.getFirstOpenPeriod_ScenarioFrom(),
                                            allExRates, GeneralDataKeeper.getFrom(), t);

                                    if (!GeneralDataKeeper.getFirstOpenPeriod_ScenarioFrom().isEqual(finalClosingdate)) {
                                        accumulatedDiscountRUB = accumulatedDiscountRUB.add(
                                                calculateAccumDiscountRUB_RegLD2(
                                                        GeneralDataKeeper.getFirstOpenPeriod_ScenarioFrom()
                                                                .toLocalDate(), finalClosingdate,
                                                        allExRates, scSAVE, t));
                                    }
                                } else {
                                    accumulatedDiscountRUB = calculateAccumDiscountRUB_RegLD2(
                                            this.depositLastDayOfFirstMonth,
                                            finalClosingdate, allExRates, scSAVE, t);
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

                    if (!GeneralDataKeeper.getFrom()
                            .equals(GeneralDataKeeper.getTo())) {
                        if (finalClosingdate.isEqual(
                                GeneralDataKeeper.getFirstOpenPeriod_ScenarioFrom())) {
                            lastCalculatedDiscount =
                                    findLastCalculatedDiscount(GeneralDataKeeper.getFrom(),
                                            finalClosingdate,
                                            CalculatedAndExistingBeforeCalculationEntries);
                        } else {
                            lastCalculatedDiscount =
                                    findLastCalculatedDiscount(GeneralDataKeeper.getFrom(),
                                            GeneralDataKeeper.getFirstOpenPeriod_ScenarioFrom(),
                                            CalculatedAndExistingBeforeCalculationEntries);
                            lastCalculatedDiscount =
                                    lastCalculatedDiscount.compareTo(BigDecimal.ZERO) != 0 ?
                                            lastCalculatedDiscount :
                                            findLastCalculatedDiscount(scSAVE, finalClosingdate,
                                                    CalculatedAndExistingBeforeCalculationEntries);
                        }
                    } else {
                        lastCalculatedDiscount =
                                findLastCalculatedDiscount(scSAVE, finalClosingdate,
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

                BigDecimal ExRateOnClosingdate = allExRates.stream()
                        .filter(er -> er.getExchangeRateID()
                                .getDate()
                                .withZoneSameInstant(ZoneId.of("UTC"))
                                .isEqual(finalClosingdate))
                        .filter(er -> er.getExchangeRateID()
                                .getCurrency()
                                .equals(this.leasingDepositToCalculate.getCurrency()))
                        /*
                         * предполагается, что на момент расчета на другой сценарий сохранения (не равный источнику)
                         * все транзакции в сценарии-источнике уже будут рассчитаны
                         */
                        .filter(er -> er.getExchangeRateID()
                                .getScenario()
                                .equals(scSAVE))
                        .map(er -> er.getRate_at_date())
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Не найден курс на дату => " + finalClosingdate));

                if (finalClosingdate.toLocalDate()
                        .isEqual(this.depositLastDayOfFirstMonth)) {
                    t.setINCOMING_LD_BODY_RUB_REG_LD_3_L(
                            t.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G()
                                    .multiply(exRateAtStartDate).setScale(10, RoundingMode.HALF_UP));
                    t.setACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R(BigDecimal.ZERO);
                } else {
                    BigDecimal ExRateOnPrevClosingdate = BigDecimal.ZERO;

                    if (!GeneralDataKeeper.getFrom()
                            .equals(GeneralDataKeeper.getTo())) {
                        if (PrevClosingDate.isBefore(
                                GeneralDataKeeper.getFirstOpenPeriod_ScenarioFrom())) {
                            ExRateOnPrevClosingdate = allExRates.stream()
                                    .filter(er -> er.getExchangeRateID()
                                            .getDate()
                                            .toLocalDate()
                                            .isEqual(PrevClosingDate.toLocalDate()))
                                    .filter(er -> er.getExchangeRateID()
                                            .getCurrency()
                                            .equals(this.leasingDepositToCalculate.getCurrency()))
                                    /*
                                     * предполагается, что на момент расчета на другой сценарий сохранения (не равный источнику)
                                     * все транзакции в сценарии-источнике уже будут рассчитаны
                                     */
                                    .filter(er -> er.getExchangeRateID()
                                            .getScenario()
                                            .equals(GeneralDataKeeper.getFrom()))
                                    .map(er -> er.getRate_at_date())
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalArgumentException(
                                            "Не найден курс на дату => " +
                                                    PrevClosingDate.toLocalDate()));
                        } else {
                            ExRateOnPrevClosingdate = allExRates.stream()
                                    .filter(er -> er.getExchangeRateID()
                                            .getDate()
                                            .toLocalDate()
                                            .isEqual(PrevClosingDate.toLocalDate()))
                                    .filter(er -> er.getExchangeRateID()
                                            .getCurrency()
                                            .equals(this.leasingDepositToCalculate.getCurrency()))
                                    /*
                                     * предполагается, что на момент расчета на другой сценарий сохранения (не равный источнику)
                                     * все транзакции в сценарии-источнике уже будут рассчитаны
                                     */
                                    .filter(er -> er.getExchangeRateID()
                                            .getScenario()
                                            .equals(scSAVE))
                                    .map(er -> er.getRate_at_date())
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalArgumentException(
                                            "Не найден курс на дату => " +
                                                    PrevClosingDate.toLocalDate()));
                        }
                    } else {
                        ExRateOnPrevClosingdate = allExRates.stream()
                                .filter(er -> er.getExchangeRateID()
                                        .getDate()
                                        .toLocalDate()
                                        .isEqual(PrevClosingDate.toLocalDate()))
                                .filter(er -> er.getExchangeRateID()
                                        .getCurrency()
                                        .equals(this.leasingDepositToCalculate.getCurrency()))
                                /*
                                 * предполагается, что на момент расчета на другой сценарий сохранения (не равный источнику)
                                 * все транзакции в сценарии-источнике уже будут рассчитаны
                                 */
                                .filter(er -> er.getExchangeRateID()
                                        .getScenario()
                                        .equals(scSAVE))
                                .map(er -> er.getRate_at_date())
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Не найден курс на дату => " +
                                                PrevClosingDate.toLocalDate()));
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

                if (Duration.between(finalClosingdate, t.getEnd_date_at_this_period())
                        .toDays() / 30.417 >= 12) {
                    t.setLDTERM_REG_LD_3_Z(LeasingDepositDuration.LT);
                } else {
                    t.setLDTERM_REG_LD_3_Z(LeasingDepositDuration.ST);
                }

                if (t.getEnd_date_at_this_period()
                        .isAfter(finalClosingdate)) {
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
                            this.deposit_sum_discounted_on_firstEndDate.multiply(
                                    exRateAtStartDate).setScale(10, RoundingMode.HALF_UP));
                } else {
                    t.setADVANCE_CURRENTPERIOD_REG_LD_3_AE(BigDecimal.ZERO);
                }

                if (findLastTransaction(this.leasingDepositToCalculate.getScenario(), scSAVE,
                        PrevClosingDate, CalculatedAndExistingBeforeCalculationEntries).size() >
                        0) {
                    Entry lde = findLastTransaction(this.leasingDepositToCalculate.getScenario(),
                            scSAVE, PrevClosingDate,
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

    private BigDecimal countDiscountedValueFromStartDateToNeededDate(ZonedDateTime endDate,
                                                                     ZonedDateTime neededDate) {
        BigDecimal countDiscountedValueFromStartDateToNeededDate = BigDecimal.ZERO;

        int LDdurationDays =
                (int) Duration.between(this.leasingDepositToCalculate.getStart_date(), endDate)
                        .toDays();
        countDiscountedValueFromStartDateToNeededDate =
                this.leasingDepositToCalculate.getDeposit_sum_not_disc()
                        .setScale(32)
                        .divide(BigDecimal.ONE.add(percentPerDay)
                                .pow(LDdurationDays), RoundingMode.UP);

        int LDdurationFormStartToNeededDays =
                (int) Duration.between(this.leasingDepositToCalculate.getStart_date(), neededDate)
                        .toDays();

        countDiscountedValueFromStartDateToNeededDate =
                countDiscountedValueFromStartDateToNeededDate.multiply(
                        BigDecimal.ONE.add(percentPerDay)
                                .pow(LDdurationFormStartToNeededDays));

        return countDiscountedValueFromStartDateToNeededDate;
    }

    private BigDecimal countDiscountFromStartDateToNeededDate(ZonedDateTime endDate,
                                                              ZonedDateTime neededDate) {
        return countDiscountedValueFromStartDateToNeededDate(endDate, neededDate).subtract(
                countDiscountedValueFromStartDateToNeededDate(endDate,
                        this.leasingDepositToCalculate.getStart_date()));
    }

    private List<Entry> findLastTransaction(Scenario scenarioFrom, Scenario scenarioTo,
                                            ZonedDateTime Date, List<Entry> entries) {
        List<Entry> LastTransaction = entries.stream()
                .filter(transaction -> transaction.getStatus()
                        .equals(EntryStatus.ACTUAL))
                .filter(transaction -> transaction.getEntryID()
                        .getScenario()
                        .equals(scenarioFrom) || transaction.getEntryID()
                        .getScenario()
                        .equals(scenarioTo))
                .filter(transaction -> transaction.getEntryID()
                        .getPeriod()
                        .getDate()
                        .withZoneSameInstant(ZoneId.of("UTC"))
                        .equals(Date))
                .collect(Collectors.toList());

        if (LastTransaction.size() == 0 || LastTransaction.size() == 1) {
            return LastTransaction;
        }
        if (LastTransaction.size() > 1) {
            return LastTransaction.stream()
                    .filter(transaction -> transaction.getEntryID()
                            .getScenario()
                            .equals(scenarioFrom))
                    .collect(Collectors.toList());
        }

        return LastTransaction;
    }

    private BigDecimal findLastCalculatedDiscount(Scenario scenarioWhereFind,
                                                  ZonedDateTime finalClosingdate,
                                                  List<Entry> entries) {
        BigDecimal LastCalculatedDiscount = BigDecimal.ZERO;

        TreeMap<ZonedDateTime, BigDecimal> tmZDT_BD = entries.stream()
                .filter(transaction -> transaction.getStatus()
                        .equals(EntryStatus.ACTUAL))
                .filter(transaction -> transaction.getEntryID()
                        .getScenario()
                        .equals(scenarioWhereFind))
                .filter(transaction -> transaction.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P()
                        .compareTo(BigDecimal.ZERO) != 0)
                .collect(TreeMap::new, (tm, transaction) -> {
                    tm.put(transaction.getEntryID()
                                    .getPeriod()
                                    .getDate(),
                            transaction.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P());
                }, (tm1, tm2) -> tm1.putAll(tm2));

        Optional<ZonedDateTime> ZDTOfLastRevaluationBefore = tmZDT_BD.keySet()
                .stream()
                .filter(key -> key.isBefore(finalClosingdate))
                .max(this.ZDTcomp);

        if (ZDTOfLastRevaluationBefore.isPresent()) {
            LastCalculatedDiscount = tmZDT_BD.get(ZDTOfLastRevaluationBefore.get());
        }

        return LastCalculatedDiscount;
    }

    private BigDecimal findLastRevaluationOfDiscount(Scenario scSAVE,
                                                     ZonedDateTime finalClosingdate,
                                                     List<Entry> entries) {
        BigDecimal LastRevaluation = BigDecimal.ZERO;

        TreeMap<ZonedDateTime, BigDecimal> tmZDT_BD = entries.stream()
                .filter(transaction -> transaction.getStatus()
                        .equals(EntryStatus.ACTUAL) && transaction.getEntryID()
                        .getScenario()
                        .equals(scSAVE))
                .collect(TreeMap::new, (tm, transaction) -> {
                    if (transaction.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q()
                            .compareTo(BigDecimal.ZERO) != 0) {
                        tm.put(transaction.getEntryID()
                                        .getPeriod()
                                        .getDate(),
                                transaction.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q());
                    }
                }, (tm1, tm2) -> tm1.putAll(tm2));

        Optional<ZonedDateTime> ZDTOfLastRevaluationBefore = tmZDT_BD.keySet()
                .stream()
                .filter(key -> key.isBefore(finalClosingdate))
                .max(this.ZDTcomp);

        if (ZDTOfLastRevaluationBefore.isPresent()) {
            LastRevaluation = tmZDT_BD.get(ZDTOfLastRevaluationBefore.get());
        }

        return LastRevaluation;
    }

    private ZonedDateTime countActualClosingDateForTheFirstOpenPeriod(
            ZonedDateTime firstOpenPeriod) {
        ZonedDateTime lastEndDateLDForFirstOpenPeriod =
                this.mappingPeriodEndDate.floorEntry(firstOpenPeriod)
                        .getValue();

        ZonedDateTime min_betw_lastEndDateLD_and_firstOpenPeriod =
                lastEndDateLDForFirstOpenPeriod.isAfter(firstOpenPeriod) ? firstOpenPeriod :
                        lastEndDateLDForFirstOpenPeriod;
        ZonedDateTime min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Day =
                min_betw_lastEndDateLD_and_firstOpenPeriod.toLocalDate()
                        .plusMonths(1)
                        .withDayOfMonth(1)
                        .atStartOfDay(ZoneId.of("UTC"));

        return min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Day;
    }

    private ZonedDateTime countFirstPeriodWithoutTransaction(Scenario Scenario_FROM,
                                                             Scenario Scenario_TO,
                                                             List<Entry> entries) {
        ZonedDateTime nextDateAfterLastWithTransaction_scenarioTO =
                ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

        if (Scenario_FROM.equals(Scenario_TO) && Scenario_FROM.getStatus().equals(ScenarioStornoStatus.ADDITION)) {
            nextDateAfterLastWithTransaction_scenarioTO =
                    countFirstPeriodWithoutTransactionInScenario(Scenario_FROM, entries);
        }

        if (!Scenario_FROM.equals(Scenario_TO)) {
            if (!this.GeneralDataKeeper.getPeriod_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo().isEqual(ZonedDateTime.of(LocalDateTime.MIN, ZoneId.of("UTC")))) {
                nextDateAfterLastWithTransaction_scenarioTO =
                        this.GeneralDataKeeper.getPeriod_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo();
            } else {
                nextDateAfterLastWithTransaction_scenarioTO =
                        GeneralDataKeeper.getFirstOpenPeriod_ScenarioFrom();
            }
        }

        if (Scenario_FROM.equals(Scenario_TO) && Scenario_FROM.getStatus()
                .equals(ScenarioStornoStatus.FULL)) {
            nextDateAfterLastWithTransaction_scenarioTO =
                    countFirstPeriodWithoutTransactionInScenario(Scenario_FROM, entries);
        }

        ZonedDateTime nextDateAfterLastWithTransaction_scenarioFROM =
                countFirstPeriodWithoutTransactionInScenario(Scenario_FROM, entries);

        //если сценарий-источник не равен сценарию-получателю, значит расчет = ADD => FULL
        if (!this.GeneralDataKeeper.getTo().equals(this.GeneralDataKeeper.getFrom())) {
            if (!(nextDateAfterLastWithTransaction_scenarioFROM.withDayOfMonth(1)
                    .minusDays(1)
                    .isEqual(this.GeneralDataKeeper.getFirstOpenPeriod_ScenarioFrom()) ||
                    nextDateAfterLastWithTransaction_scenarioFROM.isEqual(
                            this.GeneralDataKeeper.getFirstOpenPeriod_ScenarioFrom()))) {
                throw new IllegalArgumentException(
                        "Транзакции лизингового депозита не соответствуют закрытому периоду: " +
                                "период последней рассчитанной транзакции должен быть или равен первому открытому периоду или должен быть меньше строго на один период");
            }
        }

        return nextDateAfterLastWithTransaction_scenarioTO;
    }

    private ZonedDateTime countFirstPeriodWithoutTransactionInScenario(
            Scenario inWhatScenarioFindLastEntry, List<Entry> entries) {
        ZonedDateTime LastPeriodWithTransactionUTC =
                this.depositLastDayOfFirstMonth.atStartOfDay(ZoneId.of("UTC"))
                        .minusMonths(1);

        for (LocalDate closingdate : this.depositLastDayOfFirstMonth.datesUntil(
                min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Day.toLocalDate(),
                java.time.Period.ofMonths(1))
                .collect(Collectors.toList())) {
            closingdate = closingdate.withDayOfMonth(closingdate.lengthOfMonth());

            LocalDate finalClosingdate = closingdate;
            if (entries.stream()
                    .filter(entry -> entry.getEntryID()
                            .getScenario()
                            .equals(inWhatScenarioFindLastEntry))
                    .filter(entry -> entry.getStatus()
                            .equals(EntryStatus.ACTUAL))
                    .filter(entry -> entry.getEntryID()
                            .getPeriod()
                            .getDate()
                            .toLocalDate()
                            .isEqual(finalClosingdate))
                    .count() > 0) {
                LastPeriodWithTransactionUTC =
                        ZonedDateTime.of(closingdate, LocalTime.MIDNIGHT, ZoneId.of("UTC"));
            } else {
                break;
            }
        }

        ZonedDateTime nextDateAfterLastWithTransaction = LastPeriodWithTransactionUTC.plusMonths(1);
        nextDateAfterLastWithTransaction = nextDateAfterLastWithTransaction.withDayOfMonth(
                nextDateAfterLastWithTransaction.toLocalDate()
                        .lengthOfMonth());

        return nextDateAfterLastWithTransaction;
    }

    BigDecimal calculateAccumDiscountRUB_RegLD2(LocalDate startCalculatingInclusive,
                                                ZonedDateTime dateUntilCountExclusive,
                                                List<ExchangeRate> allExRates,
                                                Scenario whereCalculate,
                                                Entry calculatingEntry) {
        if (startCalculatingInclusive.isEqual(dateUntilCountExclusive.toLocalDate())) {
            throw new IllegalArgumentException("Wrong argument values: startCalculatingInclusive equals dateUntilCountExclusive");
        }

        BigDecimal accumulatedDiscountRUB = BigDecimal.ZERO;

        for (LocalDate date : startCalculatingInclusive.datesUntil(
                dateUntilCountExclusive.withDayOfMonth(1)
                        .toLocalDate(), java.time.Period.ofMonths(1))
                .collect(Collectors.toList())) {
            LocalDate lastPeriod = date.withDayOfMonth(1).minusDays(1);

            if (lastPeriod.isBefore(this.leasingDepositToCalculate.getStart_date().toLocalDate())) {
                lastPeriod = this.leasingDepositToCalculate.getStart_date().toLocalDate();
            }

            LocalDate dateLastDayOfMonth = date.withDayOfMonth(date.lengthOfMonth());

            if (!dateLastDayOfMonth.isEqual(dateUntilCountExclusive.toLocalDate())) {
                List<ExchangeRate> List_avgExRateForCalculating = allExRates.stream()
                        .filter(er -> er.getExchangeRateID()
                                .getDate()
                                .withZoneSameInstant(ZoneId.of("UTC"))
                                .isEqual(ZonedDateTime.of(dateLastDayOfMonth, LocalTime.MIDNIGHT,
                                        ZoneId.of("UTC"))))
                        .filter(er -> er.getExchangeRateID()
                                .getCurrency()
                                .equals(this.leasingDepositToCalculate.getCurrency()))
                        .filter(er -> er.getExchangeRateID()
                                .getScenario()
                                .equals(whereCalculate))
                        .collect(Collectors.toList());

                BigDecimal avgExRateForCalculating = BigDecimal.ZERO;

                if (List_avgExRateForCalculating.size() == 0) {
                    new IllegalArgumentException(
                            "Не найден средний курс за период " + dateUntilCountExclusive);
                }
                if (List_avgExRateForCalculating.size() == 1) {
                    avgExRateForCalculating = List_avgExRateForCalculating.get(0)
                            .getAverage_rate_for_month();
                }
                if (List_avgExRateForCalculating.size() > 1) {
                    avgExRateForCalculating = List_avgExRateForCalculating.stream()
                            .filter(er -> er.getExchangeRateID()
                                    .getScenario()
                                    .equals(this.leasingDepositToCalculate.getScenario()))
                            .map(ExchangeRate::getAverage_rate_for_month)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Не найден средний курс за период " + dateLastDayOfMonth));
                }

                BigDecimal discountForPeriodCUR = this.countDiscountFromStartDateToNeededDate(
                        calculatingEntry.getEnd_date_at_this_period(),
                        ZonedDateTime.of(dateLastDayOfMonth, LocalTime.MIDNIGHT, ZoneId.of("UTC")))
                        .subtract(this.countDiscountFromStartDateToNeededDate(
                                calculatingEntry.getEnd_date_at_this_period(),
                                ZonedDateTime.of(lastPeriod, LocalTime.MIDNIGHT,
                                        ZoneId.of("UTC"))));

                accumulatedDiscountRUB = accumulatedDiscountRUB.add(
                        discountForPeriodCUR.multiply(avgExRateForCalculating));
            }
        }

        return accumulatedDiscountRUB;
    }
}
