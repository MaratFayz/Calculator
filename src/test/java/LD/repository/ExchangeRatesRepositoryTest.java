package LD.repository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Disabled("Need implementation")
@ExtendWith(SpringExtension.class)
public class ExchangeRatesRepositoryTest {
    //Дата не позже текущей
    //

    @BeforeAll
    public static void getMock() {

    }

    @Test
    public void test1_importExchangeRatesFromCBR() {
        //PeriodServiceImpl.generatePeriods("08-09-1993", "20-05-2020");

        //assertEquals(periodRepository.findAll().size(), 321);
    }
}
