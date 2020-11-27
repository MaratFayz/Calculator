package LD.repository;

import LD.model.EndDate.EndDate;
import LD.model.EndDate.EndDateID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EndDatesRepository extends JpaRepository<EndDate, EndDateID>
{
}
