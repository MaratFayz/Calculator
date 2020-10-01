package LD.dao;

import LD.model.Period.Period;
import LD.model.Period.Period_;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDate;

@Component
public class PeriodDaoImpl implements PeriodDao {

    @Autowired
    private EntityManager entityManager;
    private Root<Period> root;
    private CriteriaBuilder cb;

    @Override
    public Period findPeriodByDate(LocalDate date) {
        cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Period> criteriaQuery = cb.createQuery(Period.class);

        root = criteriaQuery.from(Period.class);
        criteriaQuery
                .where(
                        periodDateEqualsTo(date)
                );

        TypedQuery<Period> query = entityManager.createQuery(criteriaQuery);
        return query.getSingleResult();
    }

    private Predicate periodDateEqualsTo(LocalDate date) {
        return cb.equal(root.get(Period_.DATE), date);
    }

    public LocalDate findMinPeriodDateInDatabase() {
        cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<LocalDate> criteriaQuery = cb.createQuery(LocalDate.class);

        root = criteriaQuery.from(Period.class);
        criteriaQuery.select(
                cb.least(root.get(Period_.date))
        );

        TypedQuery<LocalDate> query = entityManager.createQuery(criteriaQuery);
        return query.getSingleResult();
    }

    public LocalDate findMaxPeriodDateInDatabase() {
        cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<LocalDate> criteriaQuery = cb.createQuery(LocalDate.class);

        root = criteriaQuery.from(Period.class);
        criteriaQuery.select(
                cb.greatest(root.get(Period_.date))
        );

        TypedQuery<LocalDate> query = entityManager.createQuery(criteriaQuery);
        return query.getSingleResult();
    }
}
