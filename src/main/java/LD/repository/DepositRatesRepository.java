package LD.repository;

import LD.model.DepositRate.DepositRate;
import LD.model.DepositRate.DepositRateID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DepositRatesRepository extends JpaRepository<DepositRate, DepositRateID>, JpaSpecificationExecutor<DepositRate>
{
}
