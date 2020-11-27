package LD.repository;

import LD.dao.LeasingDepositDao;
import LD.model.LeasingDeposit.LeasingDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LeasingDepositRepository extends JpaSpecificationExecutor<LeasingDeposit>,
        JpaRepository<LeasingDeposit, Long>, LeasingDepositDao {
}
