package LD.service;

import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.LeasingDeposit.LeasingDepositDTO;

import java.util.List;

public interface LeasingDepositService
{
	List<LeasingDepositDTO> getAllLeasingDeposits();

	LeasingDeposit getLeasingDeposit(Long id);

	LeasingDeposit saveNewLeasingDeposit(LeasingDeposit leasingDeposit);

	LeasingDeposit updateLeasingDeposit(Long id, LeasingDeposit leasingDeposit);

	boolean delete(Long id);
}
