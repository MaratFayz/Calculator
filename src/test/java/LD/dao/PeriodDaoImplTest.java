package LD.dao;

import LD.Application;
import LD.repository.PeriodRepository;
import Utils.TestEntitiesKeeper;
import Utils.XmlDataLoader.LoadXmlFileForLeasingDepositsTest;
import Utils.XmlDataLoader.SaveEntitiesIntoDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@DataJpaTest
@ContextConfiguration(classes = {Application.class, PeriodDaoImpl.class})
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto = create",
        "spring.flyway.baselineOnMigrate = false"})
public class PeriodDaoImplTest {

    @Autowired
    private TestEntityManager testEntityManager;
    @Autowired
    private PeriodRepository periodRepository;
    private TestEntitiesKeeper testEntitiesKeeper;

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/PeriodDaoImplTest/PeriodDaoImplTest1.xml")
    @SaveEntitiesIntoDatabase
    void findMaxPeriodDateInDatabase_shouldReturnMaxDate_whenValuesInDatabase() {
        LocalDate maxPeriodDateInDatabase = periodRepository.findMaxPeriodDateInDatabase();

        assertEquals(LocalDate.of(2021,12,31), maxPeriodDateInDatabase);
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/PeriodDaoImplTest/PeriodDaoImplTest1.xml")
    @SaveEntitiesIntoDatabase
    void findMinPeriodDateInDatabase_shouldReturnMinDate_whenValuesInDatabase() {
        LocalDate minPeriodDateInDatabase = periodRepository.findMinPeriodDateInDatabase();

        assertEquals(LocalDate.of(2019,1,31), minPeriodDateInDatabase);
    }
}
