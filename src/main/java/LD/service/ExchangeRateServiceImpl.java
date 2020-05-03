package LD.service;

import LD.model.ExchangeRate.ExchangeRate;
import LD.model.ExchangeRate.ExchangeRateID;
import LD.repository.ExchangeRateRepository;
import LD.rest.exceptions.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExchangeRateServiceImpl implements ExchangeRateService
{
	@Autowired
	ExchangeRateRepository exchangeRateRepository;

	@Override
	public List<ExchangeRate> getAllExchangeRates()
	{
		return exchangeRateRepository.findAll();
	}

	@Override
	public ExchangeRate getExchangeRate(ExchangeRateID id)
	{
		return exchangeRateRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public ExchangeRate saveNewExchangeRate(ExchangeRate exchangeRate)
	{
		return exchangeRateRepository.save(exchangeRate);
	}

	@Override
	public ExchangeRate updateExchangeRate(ExchangeRateID id, ExchangeRate exchangeRate)
	{
		ExchangeRate exchangeRateToUpdate = getExchangeRate(id);

		BeanUtils.copyProperties(exchangeRate, exchangeRateToUpdate);

		exchangeRateRepository.saveAndFlush(exchangeRateToUpdate);

		return exchangeRateToUpdate;
	}

	@Override
	public boolean delete(ExchangeRateID id)
	{
		try
		{
			exchangeRateRepository.deleteById(id);
		}
		catch (Exception e)
		{
			return false;
		}

		return true;
	}
}
