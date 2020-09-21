package LD.dao;

import LD.model.LeasingDeposit.LeasingDeposit;

import java.util.List;

public interface LeasingDepositDao {

    List<LeasingDeposit> getDepositsByScenario(long scenarioId);
}
