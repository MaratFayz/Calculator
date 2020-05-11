package LD.service;

import LD.model.ExchangeRate.ExchangeRate;
import LD.model.ExchangeRate.ExchangeRateDTO_in;
import LD.model.ExchangeRate.ExchangeRateID;
import LD.model.ExchangeRate.ExchangeRateTransform;
import LD.repository.ExchangeRateRepository;
import LD.rest.exceptions.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExchangeRateServiceImpl implements ExchangeRateService
{
	@Autowired
	ExchangeRateRepository exchangeRateRepository;
	@Autowired
	ExchangeRateTransform exchangeRateTransform;

	@Override
	public List<ExchangeRateDTO_in> getAllExchangeRates()
	{
		return exchangeRateRepository.findAll()
				.stream()
				.map(er -> exchangeRateTransform.ExchangeRate_to_ExchangeRateDTO(er))
				.collect(Collectors.toList());
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
