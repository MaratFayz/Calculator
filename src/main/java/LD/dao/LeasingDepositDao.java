package LD.dao;

import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.LeasingDeposit.LeasingDepositDTO_out_onPeriodFor2Scenarios;

import java.util.List;

public interface LeasingDepositDao {

    List<LeasingDeposit> getDepositsByScenario(long scenarioId);

    List<LeasingDepositDTO_out_onPeriodFor2Scenarios> getActualDepositsWithEndDatesForScenarios(Long scenarioIdFrom, Long scenarioIdTo);
}
