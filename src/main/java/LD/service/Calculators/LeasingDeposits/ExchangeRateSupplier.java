package LD.service.Calculators.LeasingDeposits;

import LD.model.Currency.Currency;
import LD.model.ExchangeRate.ExchangeRate;
import LD.model.ExchangeRate.ExchangeRate_;
import LD.model.Scenario.Scenario;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

public class ExchangeRateSupplier {
    EntityManager em;
    Root<ExchangeRate> root;

    public BigDecimal findExchangeRateOrThrowExceptionFor(Scenario scenario, Currency currency, ZonedDateTime date) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> criteriaQuery = cb.createQuery(BigDecimal.class);

        root = criteriaQuery.from(ExchangeRate.class);
        criteriaQuery.select(rateAtDate())
                .where(
                        cb.and(
                                currencyExchangeRateEqualsTo(currency),
                                scenarioExchangeRateEqualsTo(scenario),
                                dateExchangeRateEqualsTo(date)
                        )
                );

        Query<BigDecimal> query = em.createQuery(criteriaQuery);
        BigDecimal rate = query.getResultList();
    }

    public BigDecimal findAverageExchangeRateOrThrowExceptionFor(Scenario scenario, Currency currency, ZonedDateTime date) {
//...
    }

    rateAtDate() {
        return root.get(ExchangeRate_.rate_at_date);
    }

    Predicate currencyExchangeRateEqualsTo(Currency currency) {
        return cb.equal(root.get(ExchangeRate_.exchangeRateID.currency), currency)
    }

    Predicate scenarioExchangeRateEqualsTo(Scenario scenario) {
        return cb.equal(root.get(ExchangeRate_.exchangeRateID.scenario), scenario)
    }

    Predicate dateExchangeRateEqualsTo(ZonedDateTime date) {
        return cb.equal(root.get(ExchangeRate_.exchangeRateID.date), date)
    }
}
