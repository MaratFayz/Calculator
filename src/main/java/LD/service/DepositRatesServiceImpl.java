package LD.service;

import LD.model.DepositRate.DepositRate;
import LD.model.DepositRate.DepositRateID;
import LD.repository.DepositRatesRepository;
import LD.rest.exceptions.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepositRatesServiceImpl implements DepositRatesService
{
	@Autowired
	DepositRatesRepository depositRatesRepository;

	@Override
	public List<DepositRate> getAllDepositRates()
	{
		return depositRatesRepository.findAll();
	}

	@Override
	public DepositRate getDepositRate(DepositRateID id)
	{
		return depositRatesRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public DepositRate saveNewDepositRates(DepositRate depositRate)
	{
		return depositRatesRepository.saveAndFlush(depositRate);
	}

	@Override
	public DepositRate updateDepositRates(DepositRateID id, DepositRate depositRate)
	{
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
