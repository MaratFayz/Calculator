package TestsForLeasingDeposit.Calculator.SeveralTimesOnePeriod;

import LD.model.DepositRate.DepositRate;
import LD.model.Entry.Entry;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.repository.DepositRatesRepository;
import LD.service.Calculators.LeasingDeposits.EntryCalculator;
import LD.service.Calculators.LeasingDeposits.GeneralDataKeeper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import testEntityCreation.EntryComparator;
import testEntityCreation.TestDataKeeper;
import testEntityCreation.TestEntitiesKeeper;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static TestsForLeasingDeposit.Calculator.Builders.getDate;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
public class Calculator_shouldReturnThesameValueAsAlreadyReturned_WhenCalculatingTheSamePeriod100Times {

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

    @BeforeEach
    void initializeData() throws IOException, ExecutionException, InterruptedException {
        XmlMapper xmlMapper = new XmlMapper();
        FileInputStream fileInputStream = new FileInputStream("src/test/resources/testData_LeasingDeposits_1.xml");
        TestDataKeeper data = xmlMapper.readValue(fileInputStream, TestDataKeeper.class);
        testEntitiesKeeper = new TestEntitiesKeeper(data);

        Mockito.when(gdk.getTo()).thenReturn(testEntitiesKeeper.getScenarios().stream().filter(s -> s.getName().equals("FACT")).findFirst().get());
        Mockito.when(gdk.getFrom()).thenReturn(testEntitiesKeeper.getScenarios().stream().filter(s -> s.getName().equals("FACT")).findFirst().get());
        Mockito.when(gdk.getFirstOpenPeriod_ScenarioTo()).thenReturn(getDate(31, 7, 2020));
        Mockito.when(gdk.getAllExRates()).thenReturn(testEntitiesKeeper.getExRates());
        Mockito.when(gdk.getAllPeriods()).thenReturn(testEntitiesKeeper.getPeriods());

        threadExecutor = Executors.newFixedThreadPool(10);

        leasingDepositToCalculate = testEntitiesKeeper.getLeasingDeposits().get(0);
        Specification<DepositRate> dr = lec.getDepRateForLD(leasingDepositToCalculate, lec.getLDdurationMonths());
        Mockito.when(depositRatesRepository.findAll(Mockito.any(dr.getClass()))).thenReturn(testEntitiesKeeper.getDepositRates());
    }

    @Test
    void WhenCalculate100OnePeriod_ResultsAreTheSame() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        Entry expectedEntry = testEntitiesKeeper.getEntries_expected().get(0);

        for (int i = 0; i < 1000; i++) {
            assertDoesNotThrow(() -> {
                lec = new EntryCalculator(leasingDepositToCalculate, gdk, depositRatesRepository);
                Future<List<Entry>> entries = threadExecutor.submit(lec);
                calculatedEntries.addAll(entries.get());

                EntryComparator.compare(expectedEntry, calculatedEntries.get(0));
            });
        }

        threadExecutor.shutdown();
    }
}
