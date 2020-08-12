package LD.service;

import LD.config.Security.Repository.UserRepository;
import LD.model.Counterpartner.Counterpartner;
import LD.model.Counterpartner.Counterpartner_out;
import LD.repository.CounterpartnerRepository;
import LD.rest.exceptions.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CounterpartnerServiceImpl implements CounterpartnerService
{
	@Autowired
	CounterpartnerRepository counterpartnerRepository;
	@Autowired
	UserRepository userRepository;

	@Override
	public Counterpartner getCounterpartner(Long id)
	{
		return counterpartnerRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public Counterpartner saveNewCounterpartner(Counterpartner counterpartner)
	{
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		counterpartner.setUser(userRepository.findByUsername(username));

		counterpartner.setLastChange(ZonedDateTime.now());

		return counterpartnerRepository.save(counterpartner);
	}

	@Override
	public Counterpartner updateCounterpartner(Long id, Counterpartner counterpartner)
	{
		counterpartner.setId(id);

		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		counterpartner.setUser(userRepository.findByUsername(username));

		counterpartner.setLastChange(ZonedDateTime.now());

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
	public List<Counterpartner_out> getAllCounterpartners()
	{
		List<Counterpartner> resultFormDB = counterpartnerRepository.findAll();
		List<Counterpartner_out> resultFormDB_out = new ArrayList<>();

		if(resultFormDB.size() == 0)
		{
			resultFormDB_out.add(new Counterpartner_out());
		}
		else
		{
			resultFormDB_out = resultFormDB.stream()
					.map(c -> Counterpartner_out.Counterpartner_to_CounterpartnerDTO_out(c))
					.collect(Collectors.toList());
		}

		return resultFormDB_out;
	}
}
