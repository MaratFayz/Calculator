package LD.dao;

import LD.model.Enums.STATUS_X;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.LeasingDeposit.LeasingDeposit_;
import LD.model.Scenario.Scenario_;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

@Component
public class LeasingDepositDaoImpl implements LeasingDepositDao {

    @Autowired
    private EntityManager entityManager;
    private Root<LeasingDeposit> root;
    private CriteriaBuilder cb;

    @Override
    public List<LeasingDeposit> getDepositsByScenario(long scenarioId) {
        cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<LeasingDeposit> criteriaQuery = cb.createQuery(LeasingDeposit.class);

        root = criteriaQuery.from(LeasingDeposit.class);
        criteriaQuery.select(root)
                .where(
                        cb.and(
                                cb.equal(root.get(LeasingDeposit_.IS_CREATED), STATUS_X.X),
                                cb.equal(root.get(LeasingDeposit_.SCENARIO).get(Scenario_.ID), scenarioId)
                        )
                );

        TypedQuery<LeasingDeposit> query = entityManager.createQuery(criteriaQuery);
        return query.getResultList();
    }
}
