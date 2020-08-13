package LD.repository;

import LD.config.Security.model.User.User;
import LD.model.Period.Period;
import LD.service.PeriodServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class PeriodsRepositoryTest {
    //Количество сгенерированных периодов (когда не пересекаются и когда пересекаются - без дубляжа)
    //все сгенерированные даты должны быть самыми последними днями месяца
    //перепутаны местами - боьшая и меньшая даты
    //null значения

    public static User user;
    @MockBean
    public PeriodRepository periodRepository;

    @BeforeAll
    public static void getMock() {

    }

    @Test
    public void test1_autoGeneratePeriods_noGenerated_dateFrom_less_dateTo() {
        //нет вообще периодов
        ArrayList<Period> result =
                PeriodServiceImpl.generatePeriods("08-09-1993", "20-05-2020", user, periodRepository);

        System.out.println("Всего сгенерировано периодов = " + result.size());

        assertEquals(321, result.size());
    }

    @Test
    public void test2_autoGeneratePeriods_noGenerated_dateFrom_bigger_dateTo() {
        //нет вообще периодов
        ArrayList<Period> result =
                PeriodServiceImpl.generatePeriods("20-05-2020", "08-09-1993", user, periodRepository);

        System.out.println("Всего сгенерировано периодов = " + result.size());

        assertEquals(321, result.size());

        ZonedDateTime testValue = result.get(0).getDate();
        ZonedDateTime controlValue = ZonedDateTime.of(LocalDate.of(1993, 9, 30),
                LocalTime.MIDNIGHT, ZoneId.of("UTC"));

        assertEquals(controlValue, testValue);
    }

    @Test
    public void test3_autoGeneratePeriods_noGenerated_dateFrom_equals_dateTo() {
        //нет вообще периодов
        ArrayList<Period> result =
                PeriodServiceImpl.generatePeriods("20-05-2020", "20-05-2020", user, periodRepository);

        System.out.println("Всего сгенерировано периодов = " + result.size());

        assertEquals(1, result.size());
    }

    @Test
    public void test4_autoGeneratePeriods_allAlreadyGenerated() {
        //все периоды уже имеются - не должно ничего генерироваться
        Period anyDate = Period.builder().date(ZonedDateTime.now()).build();
        when(periodRepository.findByDate(Mockito.any())).thenReturn(anyDate);

        ArrayList<Period> result =
                PeriodServiceImpl.generatePeriods("08-09-1993", "20-05-2020", user, periodRepository);

        System.out.println("Всего сгенерировано периодов = " + result.size());

        assertEquals(0, result.size());
    }
}
