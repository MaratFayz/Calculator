package LD.repository;

import LD.model.Period.Period;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface PeriodRepository extends JpaRepository<Period, Long>, JpaSpecificationExecutor<Period>
{
	Period findByDate(ZonedDateTime dateTime);
	List<Period> findByDateLessThanEqual(ZonedDateTime dateTime);
}
