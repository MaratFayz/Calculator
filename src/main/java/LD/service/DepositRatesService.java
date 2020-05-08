package LD.service;

import LD.model.DepositRate.DepositRate;
import LD.model.DepositRate.DepositRateDTO;
import LD.model.DepositRate.DepositRateID;

import java.util.List;

public interface DepositRatesService
{
	List<DepositRateDTO> getAllDepositRates();

	DepositRate getDepositRate(DepositRateID id);

	DepositRate saveNewDepositRates(DepositRate depositRate);

	DepositRate updateDepositRates(DepositRateID id, DepositRate depositRate);

	boolean delete(DepositRateID id);
}
