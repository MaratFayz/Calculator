package LD.service;

import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.PeriodsClosed.PeriodsClosedID;
import LD.repository.PeriodsClosedRepository;
import LD.rest.exceptions.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PeriodsClosedServiceImpl implements PeriodsClosedService
{
	@Autowired
	PeriodsClosedRepository periodsClosedRepository;

	@Override
	public List<PeriodsClosed> getAllPeriodsClosed()
	{
		return periodsClosedRepository.findAll();
	}

	@Override
	public PeriodsClosed getPeriodsClosed(PeriodsClosedID id)
	{
		return periodsClosedRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public PeriodsClosed saveNewPeriodsClosed(PeriodsClosed periodClosed)
	{
		return periodsClosedRepository.saveAndFlush(periodClosed);
	}

	@Override
	public PeriodsClosed updatePeriodsClosed(PeriodsClosedID id, PeriodsClosed periodClosed)
	{
		PeriodsClosed periodsClosedToUpdate = getPeriodsClosed(id);
		BeanUtils.copyProperties(periodClosed, periodsClosedToUpdate);
		periodsClosedRepository.saveAndFlush(periodsClosedToUpdate);

		return periodsClosedToUpdate;
	}

	@Override
	public boolean delete(PeriodsClosedID id)
	{
		try
		{
			periodsClosedRepository.deleteById(id);
		}
		catch(Exception e)
		{
			return false;
		}

		return true;
	}
}
