package LD.service;

import LD.config.Security.Repository.UserRepository;
import LD.model.Currency.Currency;
import LD.model.ExchangeRate.ExchangeRate;
import LD.model.ExchangeRate.ExchangeRateDTO_out;
import LD.model.ExchangeRate.ExchangeRateID;
import LD.model.ExchangeRate.ExchangeRateTransform;
import LD.model.Period.Period;
import LD.model.PeriodsClosed.PeriodsClosed;
import LD.repository.CurrencyRepository;
import LD.repository.ExchangeRateRepository;
import LD.repository.PeriodRepository;
import LD.repository.PeriodsClosedRepository;
import LD.rest.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
public class ExchangeRateServiceImpl implements ExchangeRateService
{
	@Autowired
	ExchangeRateRepository exchangeRateRepository;
	@Autowired
	ExchangeRateTransform exchangeRateTransform;
	@Autowired
	UserRepository userRepository;
	@Autowired
	CurrencyRepository currencyRepository;
	@Autowired
	PeriodRepository periodRepository;
	@Autowired
	PeriodsClosedRepository periodsClosedRepository;

	@Override
	public List<ExchangeRateDTO_out> getAllExchangeRates()
	{
		List<ExchangeRate> resultFormDB = exchangeRateRepository.findAll();
		List<ExchangeRateDTO_out> resultFormDB_out = new ArrayList<>();

		if(resultFormDB.size() == 0)
		{
			resultFormDB_out.add(new ExchangeRateDTO_out());
		}
		else
		{
			resultFormDB_out = resultFormDB.stream()
					.map(er -> exchangeRateTransform.ExchangeRate_to_ExchangeRateDTO_out(er))
					.collect(Collectors.toList());
		}

		return resultFormDB_out;
	}

	@Override
	public ExchangeRate getExchangeRate(ExchangeRateID id)
	{
		return exchangeRateRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public ExchangeRate saveNewExchangeRate(ExchangeRate exchangeRate)
	{
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		exchangeRate.setUser(userRepository.findByUsername(username));

		exchangeRate.setLastChange(ZonedDateTime.now());

		log.info("Валютный курс для сохранения = {}", exchangeRate);

		return exchangeRateRepository.save(exchangeRate);
	}

	@Override
	public ExchangeRate updateExchangeRate(ExchangeRateID id, ExchangeRate exchangeRate)
	{
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		exchangeRate.setUser(userRepository.findByUsername(username));

		exchangeRate.setLastChange(ZonedDateTime.now());

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

	@Override
	public void importExchangeRatesFormCBR()//long scenario_id, boolean isAddOnlyNewestRates)
	{
		//перечень валют с кодом ЦБ
		List<Currency> currenciesWithCBRCode = currencyRepository.findByCBRCurrencyCodeNotNull();

		log.info("Перечень валют, которые имеют код ЦБ => {}", currenciesWithCBRCode);

		//самая ранняя дата периодов в базе данных (с неё будет загружаться информация с ЦБ)
		Optional<Period> minPeriod = periodRepository.findAll().stream().min(Comparator.comparing(Period::getDate));
		ZonedDateTime minDate;

		if(minPeriod.isPresent())
			minDate = minPeriod.get().getDate().withDayOfMonth(1);
		else
			return;

		//самая поздняя дата периодов в базе данных (по неё будет загружаться информация с ЦБ)
		Optional<Period> maxPeriod = periodRepository.findAll().stream().max(Comparator.comparing(Period::getDate));
		ZonedDateTime maxDate;

		if(maxPeriod.isPresent())
			maxDate = maxPeriod.get().getDate().plusDays(1);
		else
			return;

		//определим, за какие даты в базе данных есть курсы
/*		TreeMap<Currency, List<ExchangeRate>> CurExRates= new HashMap<>();

		currenciesWithCBRCode.stream().forEach(currency -> {
			List<ExchangeRate> exchangeRates = exchangeRateRepository.findAll().stream()
					.filter(er -> er.getExchangeRateID().getScenario().getId().equals(scenario_id))
					.filter(er -> er.getExchangeRateID().getCurrency().equals(currency))
					.collect(Collectors.toList());

			if(exchangeRates.size() > 0)
			{
				exchangeRates.stream().min(Comparator.comparing(Period::getDate));
				exchangeRates.stream().max(Comparator.comparing(Period::getDate));
			}

		});*/

	}
}
