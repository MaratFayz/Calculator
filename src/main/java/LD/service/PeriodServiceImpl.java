package LD.service;

import LD.config.Security.Repository.UserRepository;
import LD.model.Period.Period;
import LD.model.Period.PeriodDTO_out;
import LD.repository.PeriodRepository;
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
public class PeriodServiceImpl implements PeriodService
{
	@Autowired
	PeriodRepository periodRepository;
	@Autowired
	UserRepository userRepository;

	@Override
	public List<PeriodDTO_out> getAllPeriods()
	{
		List<Period> resultFormDB = periodRepository.findAll();
		List<PeriodDTO_out> resultFormDB_out = new ArrayList<>();

		if(resultFormDB.size() == 0)
		{
			resultFormDB_out.add(new PeriodDTO_out());
		}
		else
		{
			resultFormDB_out = resultFormDB.stream()
					.map(per -> PeriodDTO_out.Period_to_PeriodDTO_out(per))
					.collect(Collectors.toList());
		}

		return resultFormDB_out;
	}

	@Override
	public Period getPeriod(Long id)
	{
		return periodRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public Period saveNewPeriod(Period period)
	{
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		period.setUser(userRepository.findByUsername(username));

		period.setLastChange(ZonedDateTime.now());

		log.info("Период для сохранения = {}", period);

		return periodRepository.save(period);
	}

	@Override
	public Period updatePeriod(Long id, Period period)
	{
		period.setId(id);

		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		period.setUser(userRepository.findByUsername(username));

		period.setLastChange(ZonedDateTime.now());

		Period periodToUpdate = getPeriod(id);

		BeanUtils.copyProperties(period, periodToUpdate);

		periodRepository.saveAndFlush(periodToUpdate);

		return periodToUpdate;
	}

	@Override
	public boolean delete(Long id)
	{
		try
		{
			periodRepository.deleteById(id);
		}
		catch (Exception e)
		{
			return false;
		}

		return true;
	}
}
