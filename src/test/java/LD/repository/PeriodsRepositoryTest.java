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
    public void generatePeriods_shouldGeneratePeriods_whenNoPeriodsDateFromLessThanDateTo() {
        //нет вообще периодов
        ArrayList<Period> result =
                PeriodServiceImpl.generatePeriods("08-09-1993", "20-05-2020", user, periodRepository);

        System.out.println("Всего сгенерировано периодов = " + result.size());

        assertEquals(321, result.size());
    }

    @Test
    public void generatePeriods_shouldGeneratePeriods_whenNoPeriodsDateFromBiggerThanDateTo() {
        //нет вообще периодов
        ArrayList<Period> result =
                PeriodServiceImpl.generatePeriods("20-05-2020", "08-09-1993", user, periodRepository);

        System.out.println("Всего сгенерировано периодов = " + result.size());

        assertEquals(321, result.size());

        LocalDate testValue = result.get(0).getDate();
        LocalDate controlValue = LocalDate.of(1993, 9, 30);

        assertEquals(controlValue, testValue);
    }

    @Test
    public void generatePeriods_shouldGenerateOnePeriod_whenNoPeriodsDateFromEqualsDateTo() {
        //нет вообще периодов
        ArrayList<Period> result =
                PeriodServiceImpl.generatePeriods("20-05-2020", "20-05-2020", user, periodRepository);

        System.out.println("Всего сгенерировано периодов = " + result.size());

        assertEquals(1, result.size());
    }

    @Test
    public void generatePeriods_shouldNotGeneratePeriods_whenPeriodsAlreadyExist() {
        //все периоды уже имеются - не должно ничего генерироваться
        Period anyDate = Period.builder().date(LocalDate.now()).build();
        when(periodRepository.findByDate(Mockito.any())).thenReturn(anyDate);

        ArrayList<Period> result =
                PeriodServiceImpl.generatePeriods("08-09-1993", "20-05-2020", user, periodRepository);

        System.out.println("Всего сгенерировано периодов = " + result.size());

        assertEquals(0, result.size());
    }
}