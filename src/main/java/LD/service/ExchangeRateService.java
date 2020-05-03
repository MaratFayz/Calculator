package LD.service;

import LD.model.ExchangeRate.ExchangeRate;
import LD.model.ExchangeRate.ExchangeRateID;

import java.util.List;

public interface ExchangeRateService
{
	List<ExchangeRate> getAllExchangeRates();

	ExchangeRate getExchangeRate(ExchangeRateID id);

	ExchangeRate saveNewExchangeRate(ExchangeRate period);

	ExchangeRate updateExchangeRate(ExchangeRateID id, ExchangeRate period);

	boolean delete(ExchangeRateID id);
}
