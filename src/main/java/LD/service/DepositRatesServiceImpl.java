package LD.service;

import LD.model.DepositRate.DepositRate;
import LD.model.DepositRate.DepositRateDTO_out;
import LD.model.DepositRate.DepositRateID;
import LD.model.DepositRate.DepositRateTransform;
import LD.repository.DepositRatesRepository;
import LD.rest.exceptions.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepositRatesServiceImpl implements DepositRatesService
{
	@Autowired
	DepositRatesRepository depositRatesRepository;
	@Autowired
	DepositRateTransform depositRateTransform;

	@Override
	public List<DepositRateDTO_out> getAllDepositRates()
	{
		return depositRatesRepository.findAll()
				.stream()
				.map(dr -> depositRateTransform.DepositRates_to_DepositRatesDTO_out(dr))
				.collect(Collectors.toList());
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
