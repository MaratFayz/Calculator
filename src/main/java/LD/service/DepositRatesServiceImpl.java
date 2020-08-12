package LD.service;

import LD.config.Security.Repository.UserRepository;
import LD.model.Company.Company;
import LD.model.DepositRate.DepositRate;
import LD.model.DepositRate.DepositRateDTO_out;
import LD.model.DepositRate.DepositRateID;
import LD.model.DepositRate.DepositRateTransform;
import LD.repository.DepositRatesRepository;
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
public class DepositRatesServiceImpl implements DepositRatesService
{
	@Autowired
	DepositRatesRepository depositRatesRepository;
	@Autowired
	DepositRateTransform depositRateTransform;
	@Autowired
	UserRepository userRepository;

	@Override
	public List<DepositRateDTO_out> getAllDepositRates()
	{
		List<DepositRate> resultFromDB = depositRatesRepository.findAll();
		List<DepositRateDTO_out> resultFormDB_out = new ArrayList<>();

		if(resultFromDB.size() == 0)
		{
			resultFormDB_out.add(new DepositRateDTO_out());
		}
		else
		{
			resultFormDB_out = resultFromDB.stream()
					.map(dr -> depositRateTransform.DepositRates_to_DepositRatesDTO_out(dr))
					.collect(Collectors.toList());
		}

		return resultFormDB_out;
	}

	@Override
	public DepositRate getDepositRate(DepositRateID id)
	{
		return depositRatesRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public DepositRate saveNewDepositRates(DepositRate depositRate)
	{
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		depositRate.setUser(userRepository.findByUsername(username));

		depositRate.setLastChange(ZonedDateTime.now());

		log.info("Ставка депозита для сохранения = {}", depositRate);

		return depositRatesRepository.saveAndFlush(depositRate);
	}

	@Override
	public DepositRate updateDepositRates(DepositRateID id, DepositRate depositRate)
	{
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		depositRate.setUser(userRepository.findByUsername(username));

		depositRate.setLastChange(ZonedDateTime.now());

		DepositRate depositRateToUpdate = getDepositRate(id);

		BeanUtils.copyProperties(depositRate, depositRateToUpdate);

		depositRatesRepository.saveAndFlush(depositRateToUpdate);

		return depositRateToUpdate;
	}

	@Override
	public boolean delete(DepositRateID id)
	{
		try
		{
			depositRatesRepository.deleteById(id);
		}
		catch (Exception e)
		{
			return false;
		}

		return true;
	}
}
