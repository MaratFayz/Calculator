package LD.repository;

import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.PeriodsClosed.PeriodsClosedID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PeriodsClosedRepository extends JpaRepository<PeriodsClosed, PeriodsClosedID>, JpaSpecificationExecutor<PeriodsClosed>
{
}
