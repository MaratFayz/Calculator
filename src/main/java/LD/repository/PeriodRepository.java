package LD.repository;

import LD.dao.PeriodDao;
import LD.model.Period.Period;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PeriodRepository extends JpaRepository<Period, Long>, JpaSpecificationExecutor<Period>, PeriodDao {

    Period findByDate(LocalDate dateTime);

    List<Period> findByDateLessThanEqual(LocalDate dateTime);

    List<Period> findByDateGreaterThan(LocalDate dateTime);
}
