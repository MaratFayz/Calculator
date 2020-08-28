package LD.repository;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
@Disabled("Need implementation")
public class EntryRepositoryTest {

    @Autowired
    TestEntityManager testEntityManager;

    @Autowired
    EntryRepository entryRepository;

    @Test
    void findBystatus_shouldReturnListEntries_whenStatusIsCorrect() {

    }

}
