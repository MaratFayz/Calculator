package LD.service;

import LD.model.LeasingDeposit.LeasingDeposit;

import java.util.List;

public interface LeasingDepositService
{
	List<LeasingDeposit> getAllLeasingDeposits();

	LeasingDeposit getLeasingDeposit(Long id);

	LeasingDeposit saveNewLeasingDeposit(LeasingDeposit leasingDeposit);

	LeasingDeposit updateLeasingDeposit(Long id, LeasingDeposit leasingDeposit);

	boolean delete(Long id);
}
