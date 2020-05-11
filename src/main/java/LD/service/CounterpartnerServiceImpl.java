package LD.service;

import LD.model.Counterpartner.Counterpartner;
import LD.repository.CounterpartnerRepository;
import LD.rest.exceptions.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CounterpartnerServiceImpl implements CounterpartnerService
{
	@Autowired
	CounterpartnerRepository counterpartnerRepository;

	@Override
	public Counterpartner getCounterpartner(Long id)
	{
		return counterpartnerRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public Counterpartner saveNewCounterpartner(Counterpartner counterpartner)
	{
		return counterpartnerRepository.save(counterpartner);
	}

	@Override
	public Counterpartner updateCounterpartner(Long id, Counterpartner counterpartner)
	{
		counterpartner.setId(id);

		Counterpartner counterpartnerToUpdate = getCounterpartner(id);

		BeanUtils.copyProperties(counterpartner, counterpartnerToUpdate);

		counterpartnerRepository.saveAndFlush(counterpartnerToUpdate);

		return counterpartnerToUpdate;
	}

	@Override
	public boolean delete(Long id)
	{
		try
		{
			counterpartnerRepository.deleteById(id);
		}
		catch (Exception e)
		{
			return false;
		}

		return true;
	}

	@Override
	public List<Counterpartner> getAllCounterpartners()
	{
		return counterpartnerRepository.findAll();
	}
}
