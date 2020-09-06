package LD.service.Calculators.LeasingDeposits;

import LD.model.DepositRate.DepositRate;
import LD.model.Entry.Entry;
import LD.model.Enums.EntryStatus;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.repository.DepositRatesRepository;
import Utils.Builders;
import Utils.EntryComparator;
import Utils.TestEntitiesKeeper;
import Utils.XmlDataLoader.LoadXmlFileForLeasingDepositsTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static Utils.Builders.getDate;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class EntryCalculatorTest {

    TestEntitiesKeeper testEntitiesKeeper;
    @Mock
    GeneralDataKeeper gdk;
    @Mock
    DepositRatesRepository depositRatesRepository;
    @InjectMocks
    EntryCalculator lec;

    ExecutorService threadExecutor;
    List<Entry> calculatedEntries = new ArrayList<>();

    LeasingDeposit leasingDepositToCalculate;

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testData_LeasingDeposits_1.xml")
    void calculate_shouldReturn10SameCalculations_whenInputsAreCorrectAnd10Calculations() {
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().stream().filter(s -> s.getName().equals("FACT")).findFirst().get());
        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().stream().filter(s -> s.getName().equals("FACT")).findFirst().get());
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(gdk.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        Mockito.when(gdk.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        Specification<DepositRate> dr = lec.getDepRateForLD(leasingDepositToCalculate, lec.getLDdurationMonths());
        Mockito.when(depositRatesRepository.findAll(Mockito.any(dr.getClass()))).thenReturn(testEntitiesKeeper.getDepositRates());

        Entry expectedEntry = testEntitiesKeeper.getEntries_expected().get(0);

        for (int i = 0; i < 10; i++) {
            assertDoesNotThrow(() -> {
                lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);
                Future<List<Entry>> entries = threadExecutor.submit(lec);
                calculatedEntries.addAll(entries.get());

                EntryComparator.compare(expectedEntry, calculatedEntries.get(0));
            });
        }

        threadExecutor.shutdown();
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testData_LeasingDeposits_2.xml")
    public void calculate_shouldReturnCorrectCalculations_whenDepositEndDateIsLessFirstOpenPeriodAndEntryExists() throws ExecutionException, InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(gdk.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        Mockito.when(gdk.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);
        Specification<DepositRate> dr = lec.getDepRateForLD(leasingDepositToCalculate, lec.getLDdurationMonths());
        Mockito.when(depositRatesRepository.findAll(Mockito.any(dr.getClass()))).thenReturn(testEntitiesKeeper.getDepositRates());

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(BigDecimal.valueOf(100000.0), leasingDepositToCalculate.getDeposit_sum_not_disc());
        assertEquals(BigDecimal.valueOf(88027.34), lec.getDeposit_sum_discounted_on_firstEndDate().setScale(2, RoundingMode.HALF_UP));
        assertEquals(Builders.getDate(30, 4, 2017), lec.getFirstPeriodWithoutTransactionUTC());
        assertEquals(32, lec.getCalculatedStornoDeletedEntries().size());
        EntryComparator.compare(testEntitiesKeeper.getEntries_expected(), calculatedEntries, 0);
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testData_LeasingDeposits_3.xml")
    public void calculate_shouldReturnAllDeletedEntries_whenDepositIsDeleted() throws ExecutionException, InterruptedException {
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);
        Specification<DepositRate> dr = lec.getDepRateForLD(leasingDepositToCalculate, lec.getLDdurationMonths());

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertNull(lec.getFirstPeriodWithoutTransactionUTC());
        assertEquals(33, lec.getCalculatedStornoDeletedEntries().stream()
                .filter(entry -> entry.getStatus() == EntryStatus.DELETED)
                .count());
        assertEquals(33, lec.getCalculatedStornoDeletedEntries().size());
        assertEquals(0, lec.getCalculatedStornoDeletedEntries().stream()
                .filter(entry -> entry.getStatus() == EntryStatus.ACTUAL)
                .count());
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testData_LeasingDeposits_4.xml")
    public void calculate_shouldNotReturnAnyEntries_whenDepositEndDateIsLessCalculationPeriod() throws ExecutionException, InterruptedException {
        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);

        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(gdk.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        Mockito.when(gdk.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        threadExecutor = Executors.newFixedThreadPool(10);

        lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);
        Specification<DepositRate> dr = lec.getDepRateForLD(leasingDepositToCalculate, lec.getLDdurationMonths());
        Mockito.when(depositRatesRepository.findAll(Mockito.any(dr.getClass()))).thenReturn(testEntitiesKeeper.getDepositRates());

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(Builders.getDate(31, 12, 2019), lec.getFirstPeriodWithoutTransactionUTC());
        assertEquals(0, lec.getCalculatedStornoDeletedEntries().size());
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testData_LeasingDeposits_5.xml")
    public void calculate_shouldReturnOneNewEntryAndOneStornoEntry_whenEntryForCalculationPeriodExistsAndScenarioIsAddition() throws ExecutionException, InterruptedException {
        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);

        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(gdk.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        Mockito.when(gdk.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        threadExecutor = Executors.newFixedThreadPool(10);

        lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);
        Specification<DepositRate> dr = lec.getDepRateForLD(leasingDepositToCalculate, lec.getLDdurationMonths());
        Mockito.when(depositRatesRepository.findAll(Mockito.any(dr.getClass()))).thenReturn(testEntitiesKeeper.getDepositRates());

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(getDate(30, 11, 2019), lec.getFirstPeriodWithoutTransactionUTC());
        assertEquals(1, lec.getCalculatedStornoDeletedEntries().stream()
                .filter(entry -> entry.getStatus() == EntryStatus.STORNO)
                .count());
        assertEquals(2, lec.getCalculatedStornoDeletedEntries().size());
        assertEquals(1, lec.getCalculatedStornoDeletedEntries().stream()
                .filter(entry -> entry.getStatus() == EntryStatus.ACTUAL)
                .count());
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testData_LeasingDeposits_6.xml")
    public void calculate_shouldReturnAllNewEntriesAndAllStornoEntries_whenScenarioIsFull() throws ExecutionException, InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);

        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(gdk.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        Mockito.when(gdk.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        threadExecutor = Executors.newFixedThreadPool(10);

        lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);
        Specification<DepositRate> dr = lec.getDepRateForLD(leasingDepositToCalculate, lec.getLDdurationMonths());
        Mockito.when(depositRatesRepository.findAll(Mockito.any(dr.getClass()))).thenReturn(testEntitiesKeeper.getDepositRates());

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(getDate(31, 3, 2017), lec.getFirstPeriodWithoutTransactionUTC());

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
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testData_LeasingDeposits_7.xml")
    public void calculate_shouldReturnCorrectCalculations_whenDepositEndDateIsLessFirstOpenPeriodAndNoEntries() throws ExecutionException, InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(gdk.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        Mockito.when(gdk.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);
        Specification<DepositRate> dr = lec.getDepRateForLD(leasingDepositToCalculate, lec.getLDdurationMonths());
        Mockito.when(depositRatesRepository.findAll(Mockito.any(dr.getClass()))).thenReturn(testEntitiesKeeper.getDepositRates());

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(BigDecimal.valueOf(100000.0), leasingDepositToCalculate.getDeposit_sum_not_disc());
        assertEquals(BigDecimal.valueOf(88027.34), lec.getDeposit_sum_discounted_on_firstEndDate().setScale(2, RoundingMode.HALF_UP));
        assertEquals(Builders.getDate(31, 3, 2017), lec.getFirstPeriodWithoutTransactionUTC());
        assertEquals(33, lec.getCalculatedStornoDeletedEntries().size());
        EntryComparator.compare(testEntitiesKeeper.getEntries_expected(), calculatedEntries, 0);
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testData_LeasingDeposits_8.xml")
    public void calculate_shouldReturnCorrectCalculations_whenDepositEndDateIsHigherFirstOpenPeriodAndNoEntries() throws ExecutionException, InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(gdk.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        Mockito.when(gdk.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);
        Specification<DepositRate> dr = lec.getDepRateForLD(leasingDepositToCalculate, lec.getLDdurationMonths());
        Mockito.when(depositRatesRepository.findAll(Mockito.any(dr.getClass()))).thenReturn(testEntitiesKeeper.getDepositRates());

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(BigDecimal.valueOf(100000.0), leasingDepositToCalculate.getDeposit_sum_not_disc());
        assertEquals(BigDecimal.valueOf(88027.34), lec.getDeposit_sum_discounted_on_firstEndDate().setScale(2, RoundingMode.HALF_UP));
        assertEquals(Builders.getDate(31, 3, 2017), lec.getFirstPeriodWithoutTransactionUTC());
        assertEquals(30, lec.getCalculatedStornoDeletedEntries().size());
        EntryComparator.compare(testEntitiesKeeper.getEntries_expected(), calculatedEntries, 0);
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testData_LeasingDeposits_9.xml")
    public void c() throws ExecutionException, InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        //расчет на сценарии-источнике
        //копирование сентября и октября со сценария-источника
        //расчет сценария-получателя на ноябре
        //итого всего три транзакции на сценарии-получателе новые со стасом ACTUAL
        //также добавить сюда все транзакции до ноября = 32, которые будут сторнированы
        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        Mockito.when(depositRatesRepository.findAll(Mockito.any(Specification.class))).thenReturn(testEntitiesKeeper.getDepositRates());

        //расчёт на сценарий-получатель
        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getStatus() == ScenarioStornoStatus.ADDITION).collect(Collectors.toList()).get(0));
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getStatus() == ScenarioStornoStatus.FULL).collect(Collectors.toList()).get(0));
        Mockito.when(gdk.getPeriod_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo()).thenReturn(testEntitiesKeeper.getPeriodInScenarioFromForCopyingEntriesToScenarioTo());
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioFrom()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioFrom());
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(gdk.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        Mockito.when(gdk.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        EntryCalculator calculatorTestForScenarioSourceDestination = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);

        Future<List<Entry>> entries = threadExecutor.submit(calculatorTestForScenarioSourceDestination);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(33, calculatorTestForScenarioSourceDestination.getCalculatedStornoDeletedEntries().stream()
                .filter(entry -> entry.getEntryID().getScenario().getStatus() == ScenarioStornoStatus.FULL)
                .filter(entry -> entry.getStatus() == EntryStatus.STORNO)
                .count());
        assertEquals(49, calculatorTestForScenarioSourceDestination.getCalculatedStornoDeletedEntries().size());
        assertEquals(16, calculatorTestForScenarioSourceDestination.getCalculatedStornoDeletedEntries().stream()
                .filter(entry -> entry.getEntryID().getScenario().getStatus() == ScenarioStornoStatus.FULL)
                .filter(entry -> entry.getStatus() == EntryStatus.ACTUAL)
                .count());
        EntryComparator.compare(testEntitiesKeeper.getEntries_expected(),
                calculatedEntries.stream().filter(e -> e.getEntryID().getScenario().getStatus() == ScenarioStornoStatus.FULL & e.getStatus() == EntryStatus.ACTUAL).collect(Collectors.toList()), 0);
    }
}