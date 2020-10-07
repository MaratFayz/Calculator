package LD.service.Calculators.LeasingDeposits;

import LD.model.Entry.Entry;
import LD.model.EntryIFRSAcc.EntryIFRSAcc;
import Utils.Comparators.EntryIfrsAccountsComparator;
import Utils.TestEntitiesKeeper;
import Utils.XmlDataLoader.LoadXmlFileForLeasingDepositsTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EntryIfrsAccCalculatorTest {

    TestEntitiesKeeper testEntitiesKeeper;

    @Mock
    private CalculationParametersSourceImpl gdk;

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/testDataForEntriesOnIfrsAcc/testData_IfrsAcc_1.xml")
    public void compute_shouldReturnListOfIfrsEntries_whenInputsAreCorrect() {
        when(gdk.getAllIfrsAccounts()).thenReturn(testEntitiesKeeper.getIfrsAccounts());

        EntryIfrsAccCalculator ldeIFRSAcc = new EntryIfrsAccCalculator(List.of(testEntitiesKeeper.getEntryForEntryIfrsCalculation()).toArray(Entry[]::new), gdk);
        List<EntryIFRSAcc> calculatedIfrsEntries = ldeIFRSAcc.compute();

        Assertions.assertDoesNotThrow(() -> {
            EntryIfrsAccountsComparator.compareExceptedAndCalculatedEntries(testEntitiesKeeper.getEntriesIfrsExcepted(), calculatedIfrsEntries);
        });
    }
}
