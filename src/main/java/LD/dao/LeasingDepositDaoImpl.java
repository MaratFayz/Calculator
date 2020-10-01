package LD.dao;

import LD.config.Security.model.User.User_;
import LD.model.Company.Company_;
import LD.model.Counterpartner.Counterpartner_;
import LD.model.Currency.Currency_;
import LD.model.EndDate.EndDate;
import LD.model.EndDate.EndDateID;
import LD.model.EndDate.EndDateID_;
import LD.model.EndDate.EndDate_;
import LD.model.Enums.STATUS_X;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.LeasingDeposit.LeasingDepositDTO_out_onPeriodFor2Scenarios;
import LD.model.LeasingDeposit.LeasingDeposit_;
import LD.model.Period.Period;
import LD.model.Period.Period_;
import LD.model.Scenario.Scenario_;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.time.LocalDate;
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

    @Override
    public List<LeasingDepositDTO_out_onPeriodFor2Scenarios> getActualDepositsWithEndDatesForScenarios(Long scenarioIdFrom, Long scenarioIdTo) {
        cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<LeasingDepositDTO_out_onPeriodFor2Scenarios> criteriaQuery = cb.createQuery(LeasingDepositDTO_out_onPeriodFor2Scenarios.class);

        Root<EndDate> rootEndDate = criteriaQuery.from(EndDate.class);
        Join<EndDate, LeasingDeposit> joinLd = rootEndDate.join(EndDate_.leasingDeposit);
        Join<EndDateID, Period> joinPeriod = rootEndDate.join(EndDate_.endDateID).join(EndDateID_.period);

        Subquery<LocalDate> subQMaxDateWithEndDate = getMaxDateWithEndDateForScenarioFrom(scenarioIdFrom, criteriaQuery, rootEndDate);

        criteriaQuery.select(
                cb.construct(
                        LeasingDepositDTO_out_onPeriodFor2Scenarios.class,
                        joinLd.get(LeasingDeposit_.id),
                        joinLd.get(LeasingDeposit_.deposit_sum_not_disc),
                        rootEndDate.get(EndDate_.endDateID).get(EndDateID_.scenario).get(Scenario_.name),
                        joinLd.get(LeasingDeposit_.currency).get(Currency_.name),
                        joinLd.get(LeasingDeposit_.start_date).as(String.class),
                        joinPeriod.get(Period_.date).as(String.class),
                        rootEndDate.get(EndDate_.endDate).as(String.class),
                        joinLd.get(LeasingDeposit_.company).get(Company_.name),
                        joinLd.get(LeasingDeposit_.counterpartner).get(Counterpartner_.name),
                        joinLd.get(LeasingDeposit_.is_created),
                        joinLd.get(LeasingDeposit_.is_deleted),
                        joinLd.get(LeasingDeposit_.user).get(User_.username),
                        joinLd.get(LeasingDeposit_.lastChange).as(String.class)
                )
        )
                .where(
                        cb.and(
                                cb.equal(rootEndDate.get(EndDate_.leasingDeposit).get(LeasingDeposit_.is_created), STATUS_X.X),
                                cb.or(cb.equal(joinLd.get(LeasingDeposit_.scenario).get(Scenario_.id), scenarioIdFrom),
                                        cb.equal(joinLd.get(LeasingDeposit_.scenario).get(Scenario_.id), scenarioIdTo)
                                ),
                                cb.or(
                                        cb.equal(rootEndDate.get(EndDate_.endDateID).get(EndDateID_.scenario).get(Scenario_.id), scenarioIdTo),
                                        cb.and(
                                                cb.equal(rootEndDate.get(EndDate_.endDateID).get(EndDateID_.scenario).get(Scenario_.id), scenarioIdFrom),
                                                cb.equal(rootEndDate.get(EndDate_.endDateID).get(EndDateID_.period).get(Period_.date), subQMaxDateWithEndDate)
                                        )
                                )
                        )
                );


        criteriaQuery.groupBy(
                rootEndDate.get(EndDate_.leasingDeposit).get(LeasingDeposit_.id),
                rootEndDate.get(EndDate_.endDateID).get(EndDateID_.scenario).get(Scenario_.id),
                rootEndDate.get(EndDate_.endDateID).get(EndDateID_.period).get(Period_.date));

        TypedQuery<LeasingDepositDTO_out_onPeriodFor2Scenarios> query = entityManager.createQuery(criteriaQuery);
        return query.getResultList();
    }

    private Subquery<LocalDate> getMaxDateWithEndDateForScenarioFrom(Long scenarioFromId, CriteriaQuery<LeasingDepositDTO_out_onPeriodFor2Scenarios> criteriaQuery, Root<EndDate> root) {
        Subquery<LocalDate> subQMaxDateWithEndDate = criteriaQuery.subquery(LocalDate.class);
        Root<EndDate> subRootEndDate = subQMaxDateWithEndDate.from(EndDate.class);
        Join<EndDateID, Period> subJoinPeriod = subRootEndDate.join(EndDate_.endDateID).join(EndDateID_.period);

        subQMaxDateWithEndDate.select(
                cb.greatest(subJoinPeriod.get(Period_.date))
        )
                .where(
                        cb.and(
                                cb.equal(subRootEndDate.get(EndDate_.endDateID).get(EndDateID_.scenario).get(Scenario_.id), scenarioFromId),
                                cb.equal(root.get(EndDate_.endDateID).get(EndDateID_.leasingDeposit_id), subRootEndDate.get(EndDate_.leasingDeposit))
                        )
                );

        return subQMaxDateWithEndDate;
    }
}