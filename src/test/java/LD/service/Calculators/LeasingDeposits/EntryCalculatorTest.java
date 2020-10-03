package LD.service.Calculators.LeasingDeposits;

import LD.dao.DaoKeeper;
import LD.dao.DaoKeeperImpl;
import LD.model.Currency.Currency;
import LD.model.Entry.Entry;
import LD.model.Enums.EntryStatus;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.Scenario.Scenario;
import LD.repository.DepositRatesRepository;
import LD.repository.ExchangeRateRepository;
import LD.repository.PeriodRepository;
import Utils.Builders;
import Utils.EntryComparator;
import Utils.TestEntitiesKeeper;
import Utils.XmlDataLoader.LoadXmlFileForLeasingDepositsTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static Utils.Builders.getDate;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class EntryCalculatorTest {

    TestEntitiesKeeper testEntitiesKeeper;
    @Mock
    CalculationParametersSource calculationParametersSource;
    @Mock
    ExchangeRateRepository exchangeRateRepository;
    @Mock
    PeriodRepository periodRepository;
    @Mock
    DepositRatesRepository depositRatesRepository;
    @InjectMocks
    DaoKeeper daoKeeper = new DaoKeeperImpl();

    EntryCalculator lec;
    ExecutorService threadExecutor;
    List<Entry> calculatedEntries = new ArrayList<>();
    LeasingDeposit leasingDepositToCalculate;

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldReturn10SameEntries_whenInputsAreCorrectAnd10Calculations.xml")
    void calculate_shouldReturn10SameEntries_whenInputsAreCorrectAnd10Calculations() {
        Scenario fact = testEntitiesKeeper.getScenarios().stream().filter(s -> s.getName().equals("FACT")).findFirst().get();
        Mockito.when(calculationParametersSource.getScenarioTo()).thenReturn(fact);
        Mockito.when(calculationParametersSource.getScenarioFrom()).thenReturn(fact);
        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(depositRatesRepository.getRateByCompanyMonthDurationCurrencyStartDateScenario(any(), any(), any(), any(), any())).thenReturn(testEntitiesKeeper.getDepositRates().get(0).getRATE());

        testEntitiesKeeper.getExRates().forEach(er -> {
            Mockito.lenient().when(exchangeRateRepository.getRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getRate_at_date());
            Mockito.lenient().when(exchangeRateRepository.getAverageRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getAverage_rate_for_month());
        });

        testEntitiesKeeper.getPeriods().forEach(period -> {
            Mockito.lenient().when(periodRepository.findPeriodByDate(eq(period.getDate()))).thenReturn(period);
        });

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);

        Entry expectedEntry = testEntitiesKeeper.getEntries_expected().get(0);

        for (int i = 0; i < 10; i++) {
            assertDoesNotThrow(() -> {
                lec = new EntryCalculator(leasingDepositToCalculate, calculationParametersSource, daoKeeper);
                Future<List<Entry>> entries = threadExecutor.submit(lec);
                calculatedEntries.addAll(entries.get());

                EntryComparator.compare(expectedEntry, calculatedEntries.get(0));
            });
        }

        threadExecutor.shutdown();
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldReturnCorrectEntries_whenDepositEndDateIsLessFirstOpenPeriodAndEntryExists.xml")
    public void calculate_shouldReturnCorrectEntries_whenDepositEndDateIsLessFirstOpenPeriodAndEntryExists() throws ExecutionException, InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Scenario fact = testEntitiesKeeper.getScenarios().get(0);
        Mockito.when(calculationParametersSource.getScenarioTo()).thenReturn(fact);
        Mockito.when(calculationParametersSource.getScenarioFrom()).thenReturn(fact);
        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(depositRatesRepository.getRateByCompanyMonthDurationCurrencyStartDateScenario(any(), any(), any(), any(), any())).thenReturn(testEntitiesKeeper.getDepositRates().get(0).getRATE());

        testEntitiesKeeper.getExRates().forEach(er -> {
            Mockito.lenient().when(exchangeRateRepository.getRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getRate_at_date());
            Mockito.lenient().when(exchangeRateRepository.getAverageRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getAverage_rate_for_month());
        });

        testEntitiesKeeper.getPeriods().forEach(period -> {
            Mockito.lenient().when(periodRepository.findPeriodByDate(eq(period.getDate()))).thenReturn(period);
        });


        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, calculationParametersSource, daoKeeper);

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(BigDecimal.valueOf(100000.0), leasingDepositToCalculate.getDeposit_sum_not_disc());
        assertEquals(BigDecimal.valueOf(88027.34), lec.getDepositSumDiscountedOnFirstEndDate().setScale(2, RoundingMode.HALF_UP));
        assertEquals(Builders.getDate(30, 4, 2017), lec.getFirstPeriodWithoutEntryUtc());
        assertEquals(32, lec.getCalculatedStornoDeletedEntries().size());
        EntryComparator.compare(testEntitiesKeeper.getEntries_expected(), calculatedEntries, 0);
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldReturnAllDeletedEntries_whenDepositIsDeleted.xml")
    public void calculate_shouldReturnAllDeletedEntries_whenDepositIsDeleted() throws ExecutionException, InterruptedException {
        Scenario fact = testEntitiesKeeper.getScenarios().get(0);
        Mockito.when(calculationParametersSource.getScenarioTo()).thenReturn(fact);
        Mockito.when(calculationParametersSource.getScenarioFrom()).thenReturn(fact);
        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());

        testEntitiesKeeper.getExRates().forEach(er -> {
            Mockito.lenient().when(exchangeRateRepository.getRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getRate_at_date());
            Mockito.lenient().when(exchangeRateRepository.getAverageRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getAverage_rate_for_month());
        });

        testEntitiesKeeper.getPeriods().forEach(period -> {
            Mockito.lenient().when(periodRepository.findPeriodByDate(eq(period.getDate()))).thenReturn(period);
        });

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, calculationParametersSource, daoKeeper);

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertNull(lec.getFirstPeriodWithoutEntryUtc());
        assertEquals(33, lec.getCalculatedStornoDeletedEntries().stream()
                .filter(entry -> entry.getStatus() == EntryStatus.DELETED)
                .count());
        assertEquals(33, lec.getCalculatedStornoDeletedEntries().size());
        assertEquals(0, lec.getCalculatedStornoDeletedEntries().stream()
                .filter(entry -> entry.getStatus() == EntryStatus.ACTUAL)
                .count());
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldNotReturnAnyEntries_whenDepositEndDateIsLessCalculationPeriod.xml")
    public void calculate_shouldNotReturnAnyEntries_whenDepositEndDateIsLessCalculationPeriod() throws ExecutionException, InterruptedException {
        Scenario fact = testEntitiesKeeper.getScenarios().get(0);
        Mockito.when(calculationParametersSource.getScenarioTo()).thenReturn(fact);
        Mockito.when(calculationParametersSource.getScenarioFrom()).thenReturn(fact);
        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(depositRatesRepository.getRateByCompanyMonthDurationCurrencyStartDateScenario(any(), any(), any(), any(), any())).thenReturn(testEntitiesKeeper.getDepositRates().get(0).getRATE());

        testEntitiesKeeper.getExRates().forEach(er -> {
            Mockito.lenient().when(exchangeRateRepository.getRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getRate_at_date());
            Mockito.lenient().when(exchangeRateRepository.getAverageRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getAverage_rate_for_month());
        });

        testEntitiesKeeper.getPeriods().forEach(period -> {
            Mockito.lenient().when(periodRepository.findPeriodByDate(eq(period.getDate()))).thenReturn(period);
        });

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, calculationParametersSource, daoKeeper);

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(Builders.getDate(31, 12, 2019), lec.getFirstPeriodWithoutEntryUtc());
        assertEquals(0, lec.getCalculatedStornoDeletedEntries().size());
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldReturnOneNewEntryAndOneStornoEntry_whenEntryForCalculationPeriodExistsAndScenarioIsAddition.xml")
    public void calculate_shouldReturnOneNewEntryAndOneStornoEntry_whenEntryForCalculationPeriodExistsAndScenarioIsAddition() throws ExecutionException, InterruptedException {
        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);

        Mockito.when(calculationParametersSource.getScenarioFrom()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(calculationParametersSource.getScenarioTo()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        //Mockito.when(calculationParametersSource.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        //Mockito.when(calculationParametersSource.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        Scenario fact = testEntitiesKeeper.getScenarios().stream().filter(s -> s.getName().equals("FACT")).findFirst().get();
        Currency usd = testEntitiesKeeper.getLeasingDeposits().get(0).getCurrency();
        Mockito.when(calculationParametersSource.getScenarioTo()).thenReturn(fact);
        Mockito.when(calculationParametersSource.getScenarioFrom()).thenReturn(fact);
        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(depositRatesRepository.getRateByCompanyMonthDurationCurrencyStartDateScenario(any(), any(), any(), any(), any())).thenReturn(testEntitiesKeeper.getDepositRates().get(0).getRATE());

        testEntitiesKeeper.getExRates().forEach(er -> {
            Mockito.lenient().when(exchangeRateRepository.getRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getRate_at_date());
            Mockito.lenient().when(exchangeRateRepository.getAverageRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getAverage_rate_for_month());
        });

        testEntitiesKeeper.getPeriods().forEach(period -> {
            System.out.println(period);
            Mockito.lenient().when(periodRepository.findPeriodByDate(eq(period.getDate()))).thenReturn(period);
        });

        threadExecutor = Executors.newFixedThreadPool(10);

        lec = new EntryCalculator(leasingDepositToCalculate, calculationParametersSource, daoKeeper);

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(getDate(30, 11, 2019), lec.getFirstPeriodWithoutEntryUtc());
        assertEquals(1, lec.getCalculatedStornoDeletedEntries().stream()
                .filter(entry -> entry.getStatus() == EntryStatus.STORNO)
                .count());
        assertEquals(2, lec.getCalculatedStornoDeletedEntries().size());
        assertEquals(1, lec.getCalculatedStornoDeletedEntries().stream()
                .filter(entry -> entry.getStatus() == EntryStatus.ACTUAL)
                .count());
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldReturnAllNewEntriesAndAllStornoEntries_whenScenarioIsFull.xml")
    public void calculate_shouldReturnAllNewEntriesAndAllStornoEntries_whenScenarioIsFull() throws ExecutionException, InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Scenario scenarioFull = testEntitiesKeeper.getScenarios().get(0);
        Mockito.when(calculationParametersSource.getScenarioTo()).thenReturn(scenarioFull);
        Mockito.when(calculationParametersSource.getScenarioFrom()).thenReturn(scenarioFull);
        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(depositRatesRepository.getRateByCompanyMonthDurationCurrencyStartDateScenario(any(), any(), any(), any(), any())).thenReturn(testEntitiesKeeper.getDepositRates().get(0).getRATE());

        testEntitiesKeeper.getExRates().forEach(er -> {
            Mockito.lenient().when(exchangeRateRepository.getRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getRate_at_date());
            Mockito.lenient().when(exchangeRateRepository.getAverageRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getAverage_rate_for_month());
        });

        testEntitiesKeeper.getPeriods().forEach(period -> {
            Mockito.lenient().when(periodRepository.findPeriodByDate(eq(period.getDate()))).thenReturn(period);
        });

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, calculationParametersSource, daoKeeper);

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(getDate(31, 3, 2017), lec.getFirstPeriodWithoutEntryUtc());

        assertEquals(33, lec.getCalculatedStornoDeletedEntries().stream()
                .filter(entry -> entry.getStatus() == EntryStatus.STORNO)
                .count());

        assertEquals(66, lec.getCalculatedStornoDeletedEntries().size());

        assertEquals(33, lec.getCalculatedStornoDeletedEntries().stream()
                .filter(entry -> entry.getStatus() == EntryStatus.ACTUAL)
                .count());

        EntryComparator.compare(testEntitiesKeeper.getEntries_expected(),
                calculatedEntries.stream().filter(e -> e.getStatus() == EntryStatus.ACTUAL).collect(Collectors.toList()), 0);
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldReturnCorrectEntries_whenDepositEndDateIsLessFirstOpenPeriodAndNoEntries.xml")
    public void calculate_shouldReturnCorrectEntries_whenDepositEndDateIsLessFirstOpenPeriodAndNoEntries() throws ExecutionException, InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Mockito.when(calculationParametersSource.getScenarioFrom()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(calculationParametersSource.getScenarioTo()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());

        Scenario fact = testEntitiesKeeper.getScenarios().stream().filter(s -> s.getName().equals("FACT")).findFirst().get();
        Currency usd = testEntitiesKeeper.getLeasingDeposits().get(0).getCurrency();
        Mockito.when(calculationParametersSource.getScenarioTo()).thenReturn(fact);
        Mockito.when(calculationParametersSource.getScenarioFrom()).thenReturn(fact);
        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(depositRatesRepository.getRateByCompanyMonthDurationCurrencyStartDateScenario(any(), any(), any(), any(), any())).thenReturn(testEntitiesKeeper.getDepositRates().get(0).getRATE());

        testEntitiesKeeper.getExRates().forEach(er -> {
            Mockito.lenient().when(exchangeRateRepository.getRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getRate_at_date());
            Mockito.lenient().when(exchangeRateRepository.getAverageRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getAverage_rate_for_month());
        });

        testEntitiesKeeper.getPeriods().forEach(period -> {
            System.out.println(period);
            Mockito.lenient().when(periodRepository.findPeriodByDate(eq(period.getDate()))).thenReturn(period);
        });

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, calculationParametersSource, daoKeeper);

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(BigDecimal.valueOf(100000.0), leasingDepositToCalculate.getDeposit_sum_not_disc());
        assertEquals(BigDecimal.valueOf(88027.34), lec.getDepositSumDiscountedOnFirstEndDate().setScale(2, RoundingMode.HALF_UP));
        assertEquals(Builders.getDate(31, 3, 2017), lec.getFirstPeriodWithoutEntryUtc());
        assertEquals(33, lec.getCalculatedStornoDeletedEntries().size());
        EntryComparator.compare(testEntitiesKeeper.getEntries_expected(), calculatedEntries, 0);
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldReturnCorrectEntries_whenDepositEndDateIsHigherFirstOpenPeriodAndNoEntries.xml")
    public void calculate_shouldReturnCorrectEntries_whenDepositEndDateIsHigherFirstOpenPeriodAndNoEntries() throws ExecutionException, InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Mockito.when(calculationParametersSource.getScenarioFrom()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(calculationParametersSource.getScenarioTo()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());

        Scenario fact = testEntitiesKeeper.getScenarios().stream().filter(s -> s.getName().equals("FACT")).findFirst().get();
        Currency usd = testEntitiesKeeper.getLeasingDeposits().get(0).getCurrency();
        Mockito.when(calculationParametersSource.getScenarioTo()).thenReturn(fact);
        Mockito.when(calculationParametersSource.getScenarioFrom()).thenReturn(fact);
        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(depositRatesRepository.getRateByCompanyMonthDurationCurrencyStartDateScenario(any(), any(), any(), any(), any())).thenReturn(testEntitiesKeeper.getDepositRates().get(0).getRATE());

        testEntitiesKeeper.getExRates().forEach(er -> {
            Mockito.lenient().when(exchangeRateRepository.getRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getRate_at_date());
            Mockito.lenient().when(exchangeRateRepository.getAverageRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getAverage_rate_for_month());
        });

        testEntitiesKeeper.getPeriods().forEach(period -> {
            System.out.println(period);
            Mockito.lenient().when(periodRepository.findPeriodByDate(eq(period.getDate()))).thenReturn(period);
        });

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, calculationParametersSource, daoKeeper);

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(BigDecimal.valueOf(100000.0), leasingDepositToCalculate.getDeposit_sum_not_disc());
        assertEquals(BigDecimal.valueOf(88027.34), lec.getDepositSumDiscountedOnFirstEndDate().setScale(2, RoundingMode.HALF_UP));
        assertEquals(Builders.getDate(31, 3, 2017), lec.getFirstPeriodWithoutEntryUtc());
        assertEquals(30, lec.getCalculatedStornoDeletedEntries().size());
        EntryComparator.compare(testEntitiesKeeper.getEntries_expected(), calculatedEntries, 0);
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldReturnCorrectEntries_whenScenarioFromAdditionAndScenarioToFull.xml")
    public void calculate_shouldReturnCorrectEntries_whenScenarioFromAdditionAndScenarioToFull() throws ExecutionException, InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Mockito.when(calculationParametersSource.getScenarioFrom()).thenReturn(testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getStatus() == ScenarioStornoStatus.ADDITION).collect(Collectors.toList()).get(0));
        Mockito.when(calculationParametersSource.getScenarioTo()).thenReturn(testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getStatus() == ScenarioStornoStatus.FULL).collect(Collectors.toList()).get(0));
        Mockito.when(calculationParametersSource.getEntriesCopyDateFromScenarioFromToScenarioTo()).thenReturn(testEntitiesKeeper.getPeriodInScenarioFromForCopyingEntriesToScenarioTo());
        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioFrom()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioFrom());
        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(depositRatesRepository.getRateByCompanyMonthDurationCurrencyStartDateScenario(any(), any(), any(), any(), any())).thenReturn(testEntitiesKeeper.getDepositRates().get(0).getRATE());

        testEntitiesKeeper.getExRates().forEach(er -> {
            Mockito.lenient().when(exchangeRateRepository.getRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getRate_at_date());
            Mockito.lenient().when(exchangeRateRepository.getAverageRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getAverage_rate_for_month());
        });

        testEntitiesKeeper.getPeriods().forEach(period -> {
            Mockito.lenient().when(periodRepository.findPeriodByDate(eq(period.getDate()))).thenReturn(period);
        });

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);

        EntryCalculator calculatorTestForScenarioSourceDestination = new EntryCalculator(leasingDepositToCalculate, calculationParametersSource, daoKeeper);

        Future<List<Entry>> entries = threadExecutor.submit(calculatorTestForScenarioSourceDestination);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(16, calculatorTestForScenarioSourceDestination.getCalculatedStornoDeletedEntries().stream()
                .filter(entry -> entry.getEntryID().getScenario().getStatus() == ScenarioStornoStatus.FULL)
                .filter(entry -> entry.getStatus() == EntryStatus.STORNO)
                .count());
        assertEquals(32, calculatorTestForScenarioSourceDestination.getCalculatedStornoDeletedEntries().size());
        assertEquals(16, calculatorTestForScenarioSourceDestination.getCalculatedStornoDeletedEntries().stream()
                .filter(entry -> entry.getEntryID().getScenario().getStatus() == ScenarioStornoStatus.FULL)
                .filter(entry -> entry.getStatus() == EntryStatus.ACTUAL)
                .count());
        EntryComparator.compare(testEntitiesKeeper.getEntries_expected(),
                calculatedEntries.stream().filter(e -> e.getEntryID().getScenario().getStatus() == ScenarioStornoStatus.FULL & e.getStatus() == EntryStatus.ACTUAL).collect(Collectors.toList()), 0);
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldReturnNotDiscountedEntries_whenLdDurationIsLess12Months.xml")
    public void calculate_shouldReturnNotDiscountedEntries_whenLdDurationIsLess12Months() throws ExecutionException, InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Scenario scenario = testEntitiesKeeper.getScenarios().get(0);
        Mockito.when(calculationParametersSource.getScenarioFrom()).thenReturn(scenario);
        Mockito.when(calculationParametersSource.getScenarioTo()).thenReturn(scenario);
        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(depositRatesRepository.getRateByCompanyMonthDurationCurrencyStartDateScenario(any(), any(), any(), any(), any())).thenReturn(testEntitiesKeeper.getDepositRates().get(0).getRATE());

        testEntitiesKeeper.getExRates().forEach(er -> {
            Mockito.lenient().when(exchangeRateRepository.getRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getRate_at_date());
            Mockito.lenient().when(exchangeRateRepository.getAverageRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getAverage_rate_for_month());
        });

        testEntitiesKeeper.getPeriods().forEach(period -> {
            Mockito.lenient().when(periodRepository.findPeriodByDate(eq(period.getDate()))).thenReturn(period);
        });

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, calculationParametersSource, daoKeeper);

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(BigDecimal.valueOf(100000).setScale(0, RoundingMode.HALF_UP), leasingDepositToCalculate.getDeposit_sum_not_disc().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(100000).setScale(0, RoundingMode.HALF_UP), lec.getDepositSumDiscountedOnFirstEndDate().setScale(0, RoundingMode.HALF_UP));
        assertEquals(Builders.getDate(31, 1, 2019), lec.getFirstPeriodWithoutEntryUtc());
        assertEquals(2, lec.getCalculatedStornoDeletedEntries().size());
        EntryComparator.compare(testEntitiesKeeper.getEntries_expected(), calculatedEntries, 0);
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldThrowException_whenFirstOpenPeriodOfTwoScenariosAreNotEqualToOrOneMonthMoreThanPeriodOfLastEntry.xml")
    public void calculate_shouldThrowException_whenFirstOpenPeriodOfTwoScenariosAreNotEqualToOrOneMonthMoreThanPeriodOfLastEntry() {
        Scenario scenarioAddition = testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getStatus() == ScenarioStornoStatus.ADDITION).collect(Collectors.toList()).get(0);
        Mockito.when(calculationParametersSource.getScenarioFrom()).thenReturn(scenarioAddition);
        Scenario scenarioFull = testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getStatus() == ScenarioStornoStatus.FULL).collect(Collectors.toList()).get(0);
        Mockito.when(calculationParametersSource.getScenarioTo()).thenReturn(scenarioFull);
        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioFrom()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioFrom());
        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(calculationParametersSource.getEntriesCopyDateFromScenarioFromToScenarioTo()).thenReturn(LocalDate.MIN);
        Mockito.when(depositRatesRepository.getRateByCompanyMonthDurationCurrencyStartDateScenario(any(), any(), any(), any(), any())).thenReturn(testEntitiesKeeper.getDepositRates().get(0).getRATE());

        testEntitiesKeeper.getExRates().forEach(er -> {
            Mockito.lenient().when(exchangeRateRepository.getRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getRate_at_date());
            Mockito.lenient().when(exchangeRateRepository.getAverageRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getAverage_rate_for_month());
        });

        testEntitiesKeeper.getPeriods().forEach(period -> {
            Mockito.lenient().when(periodRepository.findPeriodByDate(eq(period.getDate()))).thenReturn(period);
        });

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, calculationParametersSource, daoKeeper);

        Throwable e = Assertions.assertThrows(ExecutionException.class, () -> {
            Future<List<Entry>> entries = threadExecutor.submit(lec);
            calculatedEntries.addAll(entries.get());

            threadExecutor.shutdown();
        });

        assertEquals(e.getCause().getMessage(), "Транзакции лизингового депозита не соответствуют закрытому периоду: " +
                "период последней рассчитанной транзакции должен быть или равен первому открытому периоду или должен быть меньше строго на один период");
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldNotThrowException_whenFirstOpenPeriodsOfTwoScenariosEqualToPeriodOfLastEntry.xml")
    public void calculate_shouldNotThrowException_whenFirstOpenPeriodOfTwoScenarioOneMonthMoreThanPeriodOfLastEntry() {
        Scenario scenarioAddition = testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getStatus() == ScenarioStornoStatus.ADDITION).collect(Collectors.toList()).get(0);
        Mockito.when(calculationParametersSource.getScenarioFrom()).thenReturn(scenarioAddition);
        Scenario scenarioFull = testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getStatus() == ScenarioStornoStatus.FULL).collect(Collectors.toList()).get(0);
        Mockito.when(calculationParametersSource.getScenarioTo()).thenReturn(scenarioFull);

        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioFrom()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioFrom());
        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(calculationParametersSource.getEntriesCopyDateFromScenarioFromToScenarioTo()).thenReturn(LocalDate.MIN);

        Mockito.when(depositRatesRepository.getRateByCompanyMonthDurationCurrencyStartDateScenario(any(), any(), any(), any(), any())).thenReturn(testEntitiesKeeper.getDepositRates().get(0).getRATE());

        testEntitiesKeeper.getExRates().forEach(er -> {
            Mockito.lenient().when(exchangeRateRepository.getRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getRate_at_date());
            Mockito.lenient().when(exchangeRateRepository.getAverageRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getAverage_rate_for_month());
        });

        testEntitiesKeeper.getPeriods().forEach(period -> {
            Mockito.lenient().when(periodRepository.findPeriodByDate(eq(period.getDate()))).thenReturn(period);
        });

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, calculationParametersSource, daoKeeper);

        Assertions.assertDoesNotThrow(() -> {
            Future<List<Entry>> entries = threadExecutor.submit(lec);
            calculatedEntries.addAll(entries.get());

            threadExecutor.shutdown();
        });
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldNotThrowException_whenFirstOpenPeriodOfTwoScenarioOneMonthMoreThanPeriodOfLastEntry.xml")
    public void calculate_shouldNotThrowException_whenFirstOpenPeriodOfScenarioIsOneMonthMoreThanPeriodOfLastEntry() {
        Scenario scenarioFrom = testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getStatus() == ScenarioStornoStatus.ADDITION).collect(Collectors.toList()).get(0);
        Mockito.when(calculationParametersSource.getScenarioFrom()).thenReturn(scenarioFrom);
        Scenario scenarioTo = testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getStatus() == ScenarioStornoStatus.FULL).collect(Collectors.toList()).get(0);
        Mockito.when(calculationParametersSource.getScenarioTo()).thenReturn(scenarioTo);
        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioFrom()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioFrom());
        Mockito.when(calculationParametersSource.getEntriesCopyDateFromScenarioFromToScenarioTo()).thenReturn(LocalDate.MIN);
        Mockito.when(calculationParametersSource.getFirstOpenPeriodOfScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(depositRatesRepository.getRateByCompanyMonthDurationCurrencyStartDateScenario(any(), any(), any(), any(), any())).thenReturn(testEntitiesKeeper.getDepositRates().get(0).getRATE());

        testEntitiesKeeper.getExRates().forEach(er -> {
            Mockito.lenient().when(exchangeRateRepository.getRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getRate_at_date());
            Mockito.lenient().when(exchangeRateRepository.getAverageRateAtDate(eq(er.getExchangeRateID().getDate()), eq(er.getExchangeRateID().getScenario()), eq(er.getExchangeRateID().getCurrency()))).thenReturn(er.getAverage_rate_for_month());
        });

        testEntitiesKeeper.getPeriods().forEach(period -> {
            Mockito.lenient().when(periodRepository.findPeriodByDate(eq(period.getDate()))).thenReturn(period);
        });

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, calculationParametersSource, daoKeeper);

        Assertions.assertDoesNotThrow(() -> {
            Future<List<Entry>> entries = threadExecutor.submit(lec);
            calculatedEntries.addAll(entries.get());

            threadExecutor.shutdown();
        });
    }
}