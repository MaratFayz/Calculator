package LD.service.Calculators.LeasingDeposits;

import LD.model.Entry.Entry;
import LD.model.Enums.EntryStatus;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.repository.DepositRatesRepository;
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
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldReturn10SameEntries_whenInputsAreCorrectAnd10Calculations.xml")
    void calculate_shouldReturn10SameEntries_whenInputsAreCorrectAnd10Calculations() {
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().stream().filter(s -> s.getName().equals("FACT")).findFirst().get());
        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().stream().filter(s -> s.getName().equals("FACT")).findFirst().get());
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(gdk.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        Mockito.when(gdk.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        Mockito.when(depositRatesRepository.findAll(Mockito.any(Specification.class))).thenReturn(testEntitiesKeeper.getDepositRates());

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
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldReturnCorrectEntries_whenDepositEndDateIsLessFirstOpenPeriodAndEntryExists.xml")
    public void calculate_shouldReturnCorrectEntries_whenDepositEndDateIsLessFirstOpenPeriodAndEntryExists() throws ExecutionException, InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());

        Mockito.when(gdk.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        Mockito.when(gdk.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);
        Mockito.when(depositRatesRepository.findAll(Mockito.any(Specification.class))).thenReturn(testEntitiesKeeper.getDepositRates());

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(BigDecimal.valueOf(100000.0), leasingDepositToCalculate.getDeposit_sum_not_disc());
        assertEquals(BigDecimal.valueOf(88027.34), lec.getDepositSumDiscountedOnFirstEndDate().setScale(2, RoundingMode.HALF_UP));
        assertEquals(Builders.getDate(30, 4, 2017), lec.getFirstPeriodWithoutEntryUTC());
        assertEquals(32, lec.getCalculatedStornoDeletedEntries().size());
        EntryComparator.compare(testEntitiesKeeper.getEntries_expected(), calculatedEntries, 0);
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldReturnAllDeletedEntries_whenDepositIsDeleted.xml")
    public void calculate_shouldReturnAllDeletedEntries_whenDepositIsDeleted() throws ExecutionException, InterruptedException {
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertNull(lec.getFirstPeriodWithoutEntryUTC());
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
        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);

        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(gdk.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        Mockito.when(gdk.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        threadExecutor = Executors.newFixedThreadPool(10);

        lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);
        Mockito.when(depositRatesRepository.findAll(Mockito.any(Specification.class))).thenReturn(testEntitiesKeeper.getDepositRates());

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(Builders.getDate(31, 12, 2019), lec.getFirstPeriodWithoutEntryUTC());
        assertEquals(0, lec.getCalculatedStornoDeletedEntries().size());
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldReturnOneNewEntryAndOneStornoEntry_whenEntryForCalculationPeriodExistsAndScenarioIsAddition.xml")
    public void calculate_shouldReturnOneNewEntryAndOneStornoEntry_whenEntryForCalculationPeriodExistsAndScenarioIsAddition() throws ExecutionException, InterruptedException {
        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);

        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(gdk.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        Mockito.when(gdk.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        threadExecutor = Executors.newFixedThreadPool(10);

        lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);
        Mockito.when(depositRatesRepository.findAll(Mockito.any(Specification.class))).thenReturn(testEntitiesKeeper.getDepositRates());

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(getDate(30, 11, 2019), lec.getFirstPeriodWithoutEntryUTC());
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
        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);

        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(gdk.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        Mockito.when(gdk.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        threadExecutor = Executors.newFixedThreadPool(10);

        lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);
        Mockito.when(depositRatesRepository.findAll(Mockito.any(Specification.class))).thenReturn(testEntitiesKeeper.getDepositRates());

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(getDate(31, 3, 2017), lec.getFirstPeriodWithoutEntryUTC());

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
    @LoadXmlFileForLeasingDepositsTest(file =

            "src/test/resources/testDataForCalculator/calculate_shouldReturnCorrectEntries_whenDepositEndDateIsLessFirstOpenPeriodAndNoEntries.xml")
    public void calculate_shouldReturnCorrectEntries_whenDepositEndDateIsLessFirstOpenPeriodAndNoEntries() throws ExecutionException, InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(gdk.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        Mockito.when(gdk.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);
        Mockito.when(depositRatesRepository.findAll(Mockito.any(Specification.class))).thenReturn(testEntitiesKeeper.getDepositRates());

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(BigDecimal.valueOf(100000.0), leasingDepositToCalculate.getDeposit_sum_not_disc());
        assertEquals(BigDecimal.valueOf(88027.34), lec.getDepositSumDiscountedOnFirstEndDate().setScale(2, RoundingMode.HALF_UP));
        assertEquals(Builders.getDate(31, 3, 2017), lec.getFirstPeriodWithoutEntryUTC());
        assertEquals(33, lec.getCalculatedStornoDeletedEntries().size());
        EntryComparator.compare(testEntitiesKeeper.getEntries_expected(), calculatedEntries, 0);
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldReturnCorrectEntries_whenDepositEndDateIsHigherFirstOpenPeriodAndNoEntries.xml")
    public void calculate_shouldReturnCorrectEntries_whenDepositEndDateIsHigherFirstOpenPeriodAndNoEntries() throws ExecutionException, InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(gdk.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        Mockito.when(gdk.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);
        Mockito.when(depositRatesRepository.findAll(Mockito.any(Specification.class))).thenReturn(testEntitiesKeeper.getDepositRates());

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(BigDecimal.valueOf(100000.0), leasingDepositToCalculate.getDeposit_sum_not_disc());
        assertEquals(BigDecimal.valueOf(88027.34), lec.getDepositSumDiscountedOnFirstEndDate().setScale(2, RoundingMode.HALF_UP));
        assertEquals(Builders.getDate(31, 3, 2017), lec.getFirstPeriodWithoutEntryUTC());
        assertEquals(30, lec.getCalculatedStornoDeletedEntries().size());
        EntryComparator.compare(testEntitiesKeeper.getEntries_expected(), calculatedEntries, 0);
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldReturnCorrectEntries_whenScenarioFromAdditionAndScenarioToFull.xml")
    public void calculate_shouldReturnCorrectEntries_whenScenarioFromAdditionAndScenarioToFull() throws

            ExecutionException, InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
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
        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().get(0));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());
        Mockito.when(gdk.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        Mockito.when(gdk.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);

        Mockito.when(depositRatesRepository.findAll(Mockito.any(Specification.class))).thenReturn(testEntitiesKeeper.getDepositRates());

        Future<List<Entry>> entries = threadExecutor.submit(lec);
        calculatedEntries.addAll(entries.get());

        threadExecutor.shutdown();

        assertEquals(BigDecimal.valueOf(100000).setScale(0, RoundingMode.HALF_UP), leasingDepositToCalculate.getDeposit_sum_not_disc().setScale(0, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(100000).setScale(0, RoundingMode.HALF_UP), lec.getDepositSumDiscountedOnFirstEndDate().setScale(0, RoundingMode.HALF_UP));
        assertEquals(Builders.getDate(31, 1, 2019), lec.getFirstPeriodWithoutEntryUTC());
        assertEquals(2, lec.getCalculatedStornoDeletedEntries().size());
        EntryComparator.compare(testEntitiesKeeper.getEntries_expected(), calculatedEntries, 0);
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldThrowException_whenFirstOpenPeriodOfScenarioIsNotEqualToOrOneMonthMoreThanPeriodOfLastEntry.xml")
    public void calculate_shouldThrowException_whenFirstOpenPeriodOfScenarioIsNotEqualToOrOneMonthMoreThanPeriodOfLastEntry() {
        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getStatus() == ScenarioStornoStatus.ADDITION).collect(Collectors.toList()).get(0));
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getStatus() == ScenarioStornoStatus.FULL).collect(Collectors.toList()).get(0));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioFrom()).thenReturn(getDate(31, 3, 2020));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(getDate(31, 3, 2020));
        Mockito.when(gdk.getPeriod_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo()).thenReturn(ZonedDateTime.of(LocalDateTime.MIN, ZoneId.of("UTC")));

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);
        Mockito.when(depositRatesRepository.findAll(Mockito.any(Specification.class))).thenReturn(testEntitiesKeeper.getDepositRates());

        Throwable e = Assertions.assertThrows(ExecutionException.class, () -> {
            Future<List<Entry>> entries = threadExecutor.submit(lec);
            calculatedEntries.addAll(entries.get());

            threadExecutor.shutdown();
        });

        assertEquals(e.getCause().getMessage(), "Транзакции лизингового депозита не соответствуют закрытому периоду: " +
                "период последней рассчитанной транзакции должен быть или равен первому открытому периоду или должен быть меньше строго на один период");
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldNotThrowException_whenFirstOpenPeriodOfScenarioIsEqualToPeriodOfLastEntry.xml")
    public void calculate_shouldNotThrowException_whenFirstOpenPeriodOfScenarioIsEqualToPeriodOfLastEntry() {
        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getStatus() == ScenarioStornoStatus.ADDITION).collect(Collectors.toList()).get(0));
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getStatus() == ScenarioStornoStatus.FULL).collect(Collectors.toList()).get(0));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioFrom()).thenReturn(getDate(31, 3, 2017));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(getDate(31, 3, 2017));
        Mockito.when(gdk.getPeriod_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo()).thenReturn(ZonedDateTime.of(LocalDateTime.MIN, ZoneId.of("UTC")));
        Mockito.when(gdk.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        Mockito.when(gdk.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);
        Mockito.when(depositRatesRepository.findAll(Mockito.any(Specification.class))).thenReturn(testEntitiesKeeper.getDepositRates());

        Assertions.assertDoesNotThrow(() -> {
            Future<List<Entry>> entries = threadExecutor.submit(lec);
            calculatedEntries.addAll(entries.get());

            threadExecutor.shutdown();
        });
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForCalculator/calculate_shouldNotThrowException_whenFirstOpenPeriodOfScenarioIsOneMonthMoreThanPeriodOfLastEntry.xml")
    public void calculate_shouldNotThrowException_whenFirstOpenPeriodOfScenarioIsOneMonthMoreThanPeriodOfLastEntry() {
        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getStatus() == ScenarioStornoStatus.ADDITION).collect(Collectors.toList()).get(0));
        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().stream().filter(sc -> sc.getStatus() == ScenarioStornoStatus.FULL).collect(Collectors.toList()).get(0));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioFrom()).thenReturn(getDate(30, 4, 2017));
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(getDate(30, 4, 2017));
        Mockito.when(gdk.getPeriod_in_ScenarioFrom_ForCopyingEntries_to_ScenarioTo()).thenReturn(ZonedDateTime.of(LocalDateTime.MIN, ZoneId.of("UTC")));
        Mockito.when(gdk.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        Mockito.when(gdk.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);
        Mockito.when(depositRatesRepository.findAll(Mockito.any(Specification.class))).thenReturn(testEntitiesKeeper.getDepositRates());

        Assertions.assertDoesNotThrow(() -> {
            Future<List<Entry>> entries = threadExecutor.submit(lec);
            calculatedEntries.addAll(entries.get());

            threadExecutor.shutdown();
        });

    }
}