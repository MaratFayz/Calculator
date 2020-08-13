package LD.service.Calculators.LeasingDeposits;

import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.Scenario.Scenario;
import LD.repository.DepositRatesRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

//@Disabled("Add implementation")
@ExtendWith(MockitoExtension.class)
public class EntryCalculatorTest {

    @Mock
    LeasingDeposit leasingDepositToCalculate;
    @Mock
    GeneralDataKeeper GeneralDataKeeper;
    @Mock
    DepositRatesRepository depositRatesRepository;

    @Test
    void calculateAccumDiscountRUB_RegLD2_shouldThrowException_whenStartCalculatingInclusiveAnddateUntilCountExclusiveAreEqual() {
        EntryCalculator entryCalculator = new EntryCalculator(leasingDepositToCalculate, GeneralDataKeeper, depositRatesRepository);
        LocalDate startCalculatingInclusive = LocalDate.now();
        ZonedDateTime dateUntilCountExclusive = ZonedDateTime.of(startCalculatingInclusive, LocalTime.MIDNIGHT, ZoneId.of("UTC"));

        Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            entryCalculator.calculateAccumDiscountRUB_RegLD2(startCalculatingInclusive,
                    dateUntilCountExclusive, List.of(), new Scenario(), null);
        });

        assertEquals("Wrong argument values: startCalculatingInclusive equals dateUntilCountExclusive", exception.getMessage());
    }
}
