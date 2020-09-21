package LD.dao;

import LD.model.Currency.Currency;
import LD.model.ExchangeRate.ExchangeRate;
import LD.model.ExchangeRate.ExchangeRateID_;
import LD.model.ExchangeRate.ExchangeRate_;
import LD.model.Scenario.Scenario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class ExchangeRateDaoImpl implements ExchangeRateDao {

    @Autowired
    private EntityManager entityManager;
    private Root<ExchangeRate> root;
    private CriteriaBuilder cb;

    @Override
    public BigDecimal getRateAtDate(LocalDate date, Scenario scenario, Currency currency) {
        cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> criteriaQuery = cb.createQuery(BigDecimal.class);

        root = criteriaQuery.from(ExchangeRate.class);
        criteriaQuery.select(root.get(ExchangeRate_.rate_at_date))
                .where(
                        cb.and(
                                cb.equal(root.get(ExchangeRate_.exchangeRateID).get(ExchangeRateID_.scenario), scenario),
                                cb.equal(root.get(ExchangeRate_.exchangeRateID).get(ExchangeRateID_.date), date),
                                cb.equal(root.get(ExchangeRate_.exchangeRateID).get(ExchangeRateID_.currency), currency)
                        )
                );

        TypedQuery<BigDecimal> query = entityManager.createQuery(criteriaQuery);
        return query.getSingleResult();
    }

    @Override
    public BigDecimal getAverageRateAtDate(LocalDate date, Scenario scenario, Currency currency) {
        cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> criteriaQuery = cb.createQuery(BigDecimal.class);

        root = criteriaQuery.from(ExchangeRate.class);
        criteriaQuery.select(root.get(ExchangeRate_.average_rate_for_month))
                .where(
                        cb.and(
                                cb.equal(root.get(ExchangeRate_.exchangeRateID).get(ExchangeRateID_.scenario), scenario),
                                cb.equal(root.get(ExchangeRate_.exchangeRateID).get(ExchangeRateID_.date), date),
                                cb.equal(root.get(ExchangeRate_.exchangeRateID).get(ExchangeRateID_.currency), currency)
                        )
                );

        TypedQuery<BigDecimal> query = entityManager.createQuery(criteriaQuery);
        return query.getSingleResult();
    }
}