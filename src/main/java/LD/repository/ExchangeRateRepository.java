package LD.repository;

import LD.dao.ExchangeRateDao;
import LD.model.ExchangeRate.ExchangeRate;
import LD.model.ExchangeRate.ExchangeRateID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, ExchangeRateID>,
        JpaSpecificationExecutor<ExchangeRate>, ExchangeRateDao {
}
