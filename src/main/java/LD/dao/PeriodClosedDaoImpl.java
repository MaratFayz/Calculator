package LD.dao;

import LD.model.Period.Period;
import LD.model.Period.Period_;
import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.PeriodsClosed.PeriodsClosedID_;
import LD.model.PeriodsClosed.PeriodsClosed_;
import LD.model.Scenario.Scenario;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.time.LocalDate;

public class PeriodClosedDaoImpl implements PeriodClosedDao {

    @Autowired
    private EntityManager entityManager;
    private Root<PeriodsClosed> root;
    private CriteriaBuilder cb;

    @Override
    public LocalDate findFirstOpenPeriodByScenario(Scenario scenario) {
        cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<LocalDate> criteriaQuery = cb.createQuery(LocalDate.class);

        root = criteriaQuery.from(PeriodsClosed.class);
        Join<PeriodsClosed, Period> join = root.join(PeriodsClosed_.periodsClosedID).join(PeriodsClosedID_.PERIOD);

        criteriaQuery.select(cb.least(join.get(Period_.date)))
                .where(
                        cb.and(
                                cb.equal(root.get(PeriodsClosed_.periodsClosedID).get(PeriodsClosedID_.scenario), scenario),
                                cb.isNull(root.get(PeriodsClosed_.ISCLOSED))
                        )
                );

        TypedQuery<LocalDate> query = entityManager.createQuery(criteriaQuery);
        return query.getSingleResult();
    }
}
