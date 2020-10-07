package LD.dao;

import LD.model.Currency.Currency;
import LD.model.Currency.Currency_;
import LD.model.ExchangeRate.ExchangeRate;
import LD.model.ExchangeRate.ExchangeRateID;
import LD.model.ExchangeRate.ExchangeRateID_;
import LD.model.ExchangeRate.ExchangeRate_;
import LD.model.Scenario.Scenario;
import LD.model.Scenario.Scenario_;
import LD.repository.CurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class ExchangeRateDaoImpl implements ExchangeRateDao {

    @PersistenceContext
    private EntityManager entityManager;
    private Root<ExchangeRate> root;
    private CriteriaBuilder cb;
    @Autowired
    private CurrencyRepository currencyRepository;

    @Override
    public BigDecimal getRateAtDate(LocalDate date, Scenario scenario, Currency currency) {
        return getRateByDateScenarioCurrencyType(date, scenario, currency, ExchangeRate_.rate_at_date);
    }

    @Override
    public BigDecimal getAverageRateAtDate(LocalDate date, Scenario scenario, Currency currency) {
        return getRateByDateScenarioCurrencyType(date, scenario, currency, ExchangeRate_.average_rate_for_month);
    }

    private BigDecimal getRateByDateScenarioCurrencyType(LocalDate date, Scenario scenario, Currency currency,
                                                         SingularAttribute<ExchangeRate, BigDecimal> rateType) {
        cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> criteriaQuery = cb.createQuery(BigDecimal.class);

        root = criteriaQuery.from(ExchangeRate.class);
        criteriaQuery.select(root.get(rateType))
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
    public LocalDate findMaxDateWithExchangeRateByCurrencyIdAndScenarioId(Long currencyId, Long scenarioId) {
        cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<LocalDate> criteriaQuery = cb.createQuery(LocalDate.class);

        root = criteriaQuery.from(ExchangeRate.class);
        Join<ExchangeRate, ExchangeRateID> join = root.join(ExchangeRate_.exchangeRateID);

        criteriaQuery.select(
                cb.greatest(join.get(ExchangeRateID_.date))
        )
                .where(
                        cb.and(
                                cb.equal(root.get(ExchangeRate_.exchangeRateID).get(ExchangeRateID_.scenario).get(Scenario_.id), scenarioId),
                                cb.equal(root.get(ExchangeRate_.exchangeRateID).get(ExchangeRateID_.currency).get(Currency_.id), currencyId)
                        )
                );

        TypedQuery<LocalDate> query = entityManager.createQuery(criteriaQuery);
        return query.getSingleResult();
    }
}