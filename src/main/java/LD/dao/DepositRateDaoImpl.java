package LD.dao;

import LD.model.Company.Company;
import LD.model.Currency.Currency;
import LD.model.DepositRate.DepositRate;
import LD.model.DepositRate.DepositRateID;
import LD.model.DepositRate.DepositRateID_;
import LD.model.DepositRate.DepositRate_;
import LD.model.Duration.Duration;
import LD.model.Duration.Duration_;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.Scenario.Scenario;
import LD.repository.DepositRatesRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@Log4j2
public class DepositRateDaoImpl implements DepositRateDao {

    @Autowired
    private EntityManager entityManager;
    private Root<DepositRate> root;
    private CriteriaBuilder cb;

    @Override
    public BigDecimal getRateByCompanyMonthDurationCurrencyStartDateScenario(Company depositCompany,
                                                                             Integer depositDurationMonth,
                                                                             Currency depositCurrency,
                                                                             LocalDate depositStartDate,
                                                                             Scenario depositScenario) {
        log.info("depositCompany = {}", depositCompany);
        log.info("depositDurationMonth = {}", depositDurationMonth);
        log.info("depositCurrency = {}", depositCurrency);
        log.info("depositStartDate = {}", depositStartDate);
        log.info("depositScenario = {}", depositScenario);

        cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> criteriaQuery = cb.createQuery(BigDecimal.class);

        root = criteriaQuery.from(DepositRate.class);
        Join<DepositRateID, Duration> join = root.join(DepositRate_.depositRateID).join(DepositRateID_.duration);

        criteriaQuery.select(root.get(DepositRate_.RATE))
                .where(
                        cb.and(cb.equal(root.get(DepositRate_.depositRateID).get(DepositRateID_.company), depositCompany),
                                cb.equal(root.get(DepositRate_.depositRateID).get(DepositRateID_.scenario), depositScenario),
                                cb.equal(root.get(DepositRate_.depositRateID).get(DepositRateID_.currency), depositCurrency),
                                cb.lessThanOrEqualTo(root.get(DepositRate_.depositRateID).get(DepositRateID_.START_PERIOD), depositStartDate),
                                cb.greaterThanOrEqualTo(root.get(DepositRate_.depositRateID).get(DepositRateID_.END_PERIOD), depositStartDate),
                                cb.greaterThanOrEqualTo(join.get(Duration_.MAX_MONTH), depositDurationMonth),
                                cb.lessThanOrEqualTo(join.get(Duration_.MIN_MONTH), depositDurationMonth))
                );

        TypedQuery<BigDecimal> query = entityManager.createQuery(criteriaQuery);
        return query.getSingleResult();
    }

    public static Specification<DepositRate> getDepRateForLD(
            LeasingDeposit leasingDepositToCalculate, int durationOfLDInMonth) {
        return new Specification<DepositRate>() {
            @Override
            public Predicate toPredicate(Root<DepositRate> rootLDRates, CriteriaQuery<?> query,
                                         CriteriaBuilder cb) {
                return cb.and(cb.equal(rootLDRates.get("depositRateID")
                                .get("company"), leasingDepositToCalculate.getCompany()),
                        cb.lessThanOrEqualTo(rootLDRates.get("depositRateID")
                                .get("START_PERIOD"), leasingDepositToCalculate.getStart_date()),
                        cb.greaterThanOrEqualTo(rootLDRates.get("depositRateID")
                                .get("END_PERIOD"), leasingDepositToCalculate.getStart_date()),
                        cb.equal(rootLDRates.get("depositRateID")
                                .get("currency"), leasingDepositToCalculate.getCurrency()),
                        cb.lessThanOrEqualTo(rootLDRates.get("depositRateID")
                                .get("duration")
                                .get("MIN_MONTH"), durationOfLDInMonth), cb.greaterThanOrEqualTo(
                                rootLDRates.get("depositRateID")
                                        .get("duration")
                                        .get("MAX_MONTH"), durationOfLDInMonth), cb.equal(
                                rootLDRates.get("depositRateID")
                                        .get("scenario"), leasingDepositToCalculate.getScenario()));
            }
        };
    }
}