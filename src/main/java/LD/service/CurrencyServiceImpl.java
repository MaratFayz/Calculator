package LD.service;

import LD.config.Security.Repository.UserRepository;
import LD.model.Currency.Currency;
import LD.model.Currency.Currency_out;
import LD.repository.CurrencyRepository;
import LD.rest.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
public class CurrencyServiceImpl implements CurrencyService
{
	@Autowired
	CurrencyRepository currencyRepository;
	@Autowired
	UserRepository userRepository;

	@Override
	public List<Currency_out> getAllCurrencies()
	{
		List<Currency> resultFormDB = currencyRepository.findAll();
		List<Currency_out> resultFormDB_out = new ArrayList<>();

		if(resultFormDB.size() == 0)
		{
			resultFormDB_out.add(new Currency_out());
		}
		else
		{
			resultFormDB_out = resultFormDB.stream()
					.map(c -> Currency_out.Currency_to_CurrencyDTO(c))
					.collect(Collectors.toList());
		}

		return resultFormDB_out;
	}

	@Override
	public Currency getCurrency(Long id)
	{
		return currencyRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public Currency saveNewCurrency(Currency currency)
	{
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		currency.setUser(userRepository.findByUsername(username));

		currency.setLastChange(ZonedDateTime.now());

		return currencyRepository.save(currency);
	}

	@Override
	public Currency updateCurrency(Long id, Currency currency)
	{
		currency.setId(id);

		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		currency.setUser(userRepository.findByUsername(username));

		currency.setLastChange(ZonedDateTime.now());

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
			log.info("При удалении валюты произошла ошибка. Возврат значения false");
			return false;
		}

		return true;
	}
}
