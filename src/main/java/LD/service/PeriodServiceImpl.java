package LD.service;

import LD.model.Period.Period;
import LD.model.Period.PeriodDTO;
import LD.repository.PeriodRepository;
import LD.rest.exceptions.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PeriodServiceImpl implements PeriodService
{
	@Autowired
	PeriodRepository periodRepository;

	@Override
	public List<PeriodDTO> getAllPeriods()
	{
		return periodRepository.findAll().stream()
				.map(per -> PeriodDTO.Period_to_PeriodDTO(per))
				.collect(Collectors.toList());
	}

	@Override
	public Period getPeriod(Long id)
	{
		return periodRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public Period saveNewPeriod(Period period)
	{
		return periodRepository.save(period);
	}

	@Override
	public Period updatePeriod(Long id, Period period)
	{
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
