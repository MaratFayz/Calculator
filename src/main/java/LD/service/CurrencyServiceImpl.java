package LD.service;

import LD.model.Currency.Currency;
import LD.repository.CurrencyRepository;
import LD.rest.exceptions.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CurrencyServiceImpl implements CurrencyService
{
	@Autowired
	CurrencyRepository currencyRepository;

	@Override
	public List<Currency> getAllCurrencies()
	{
		return currencyRepository.findAll();
	}

	@Override
	public Currency getCurrency(Long id)
	{
		return currencyRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public Currency saveNewCurrency(Currency currency)
	{
		return currencyRepository.save(currency);
	}

	@Override
	public Currency updateCurrency(Long id, Currency currency)
	{
		Currency currencyToUpdate = getCurrency(id);

		BeanUtils.copyProperties(currency, currencyToUpdate);

		currencyRepository.saveAndFlush(currencyToUpdate);

		return currencyToUpdate;
	}

	@Override
	public boolean delete(Long id)
	{
		try
		{
			currencyRepository.deleteById(id);
		}
		catch (Exception e)
		{
			return false;
		}

		return true;
	}
}
