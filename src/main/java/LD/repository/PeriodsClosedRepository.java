package LD.repository;

import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.PeriodsClosed.PeriodsClosedID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface PeriodsClosedRepository extends JpaRepository<PeriodsClosed, PeriodsClosedID>, JpaSpecificationExecutor<PeriodsClosed>
{
////	@Query(nativeQuery = true,
////			value = "Select pc.periodsClosedID.period.date from PeriodsClosed pc " +
////			"where pc.ISCLOSED = null and pc.periodsClosedID.scenario.name = ?1 " +
////			"ORDER BY pc.periodsClosedID.period.date " +
////			"LIMIT 1")
//
////	@Query(nativeQuery = true,
////			value = "Select date from PeriodsClosed pc " +
////			"where pc.ISCLOSED = null and pc.periodsClosedID.scenario.name = ?1 " +
////			"ORDER BY pc.periodsClosedID.period.date " +
////			"LIMIT 1")
//
////	@Query(nativeQuery = true,
////			value = "Select date from PeriodsClosed pc " +
////			"where pc.ISCLOSED = null and pc.periodsClosedID.scenario.name = ?1 ")
//
//		@Query(nativeQuery = true,
//			value = "Select pc.periodsClosedID.period.date from PeriodsClosed pc " +
//			"where pc.ISCLOSED = null and pc.periodsClosedID.scenario.name = ?1 " +
//			"ORDER BY pc.periodsClosedID.period.date " +
//			"LIMIT 1;")
//	ZonedDateTime findFirstOpenPeriodByScenarioId(String scenario_name);
}
