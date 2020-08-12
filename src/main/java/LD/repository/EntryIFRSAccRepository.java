package LD.repository;

import LD.model.EntryIFRSAcc.EntryIFRSAcc;
import LD.model.EntryIFRSAcc.EntryIFRSAccID;
import LD.model.Enums.EntryStatus;
import LD.model.Period.Period;
import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.Scenario.Scenario;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.*;
import java.util.List;

@Repository
public interface EntryIFRSAccRepository extends JpaRepository<EntryIFRSAcc, EntryIFRSAccID>
{
/*	default Specification<EntryIFRSAcc> findEntriesByScenarioPeriodStatus(Scenario scenario,
																		   Period period,
																		   EntryStatus entryStatus)
	{
		//https://stackoverflow.com/questions/38934549/spring-specification-with-sum-function
		//https://yandex.ru/search/?text=specification%20java%20spring%20sum&lr=213&noreask=1&nomisspell=1

		return (entryIFRSAccRoot, criteriaQuery, criteriaBuilder) ->
		{
			Path entryPath = entryIFRSAccRoot.get("entryIFRSAccID").get("entry");

			Predicate predicate = criteriaBuilder.and(
					criteriaBuilder.equal(entryPath.get("status"), entryStatus),
					criteriaBuilder.equal(entryPath.get("entryID").get("scenario"), scenario),
					criteriaBuilder.equal(entryPath.get("entryID").get("period"), period));

			criteriaQuery.where(predicate);

			Path sum = entryIFRSAccRoot.get("sum");
			criteriaQuery.select(criteriaBuilder.sum(sum));

			//criteriaBuilder.ge.sum();

			return predicate;
		};
	}*/
}
