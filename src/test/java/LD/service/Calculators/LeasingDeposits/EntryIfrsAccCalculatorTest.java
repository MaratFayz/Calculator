package LD.service.Calculators.LeasingDeposits;

import LD.model.Entry.Entry;
import LD.model.EntryIFRSAcc.EntryIFRSAcc;
import Utils.Comparators.EntryIfrsAccountsComparator;
import Utils.TestEntitiesKeeper;
import Utils.XmlDataLoader.LoadXmlFileForLeasingDepositsTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = {EntryIfrsAccCalculatorTest.TestBeansFactory.class})
public class EntryIfrsAccCalculatorTest {

    TestEntitiesKeeper testEntitiesKeeper;

    @Configuration
    static class TestBeansFactory {

        @Bean
        @Scope("prototype")
        EntryIfrsAccCalculatorImpl getEntryIfrsAccCalculatorImpl(Entry[] allEntries, CalculationParametersSource calculationParametersSource) {
            return new EntryIfrsAccCalculatorImpl(allEntries, calculationParametersSource);
        }
    }

    @MockBean
    private CalculationParametersSource gdk;

    @Autowired
    EntryIfrsAccCalculatorTest.TestBeansFactory testBeansFactory;

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForEntriesOnIfrsAcc/testData_IfrsAcc_1.xml")
    public void compute_shouldReturnListOfIfrsEntries_whenInputsAreCorrect() {
        when(gdk.getAllIfrsAccounts()).thenReturn(testEntitiesKeeper.getIfrsAccounts());

        EntryIfrsAccCalculator ldeIFRSAcc = testBeansFactory.getEntryIfrsAccCalculatorImpl(List.of(testEntitiesKeeper.getEntryForEntryIfrsCalculation()).toArray(Entry[]::new), gdk);
        List<EntryIFRSAcc> calculatedIfrsEntries = ldeIFRSAcc.calculateEntryIfrsAcc();

        Assertions.assertDoesNotThrow(() -> {
            EntryIfrsAccountsComparator.compareExceptedAndCalculatedEntries(testEntitiesKeeper.getEntriesIfrsExcepted(), calculatedIfrsEntries);
        });
    }
}
