package LD.service;

import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.LeasingDeposit.LeasingDepositDTO_out;
import LD.model.LeasingDeposit.LeasingDepositDTO_out_onPeriodFor2Scenarios;

import java.util.List;

public interface LeasingDepositService
{
	List<LeasingDepositDTO_out> getAllLeasingDeposits();

	List<LeasingDepositDTO_out_onPeriodFor2Scenarios> getAllLeasingDepositsOnPeriodFor2Scenarios(Long scenarioFromId,
																								 Long scenarioToId);

	LeasingDeposit getLeasingDeposit(Long id);

	LeasingDeposit saveNewLeasingDeposit(LeasingDeposit leasingDeposit);

	LeasingDeposit updateLeasingDeposit(Long id, LeasingDeposit leasingDeposit);

	void delete(Long id);
}
