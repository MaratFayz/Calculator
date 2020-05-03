package LD.service;

import LD.model.EndDate.EndDate;
import LD.model.EndDate.EndDateID;
import LD.repository.EndDatesRepository;
import LD.rest.exceptions.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EndDateServiceImpl implements EndDateService
{
	@Autowired
	EndDatesRepository endDatesRepository;

	@Override
	public List<EndDate> getAllEndDates()
	{
		return endDatesRepository.findAll();
	}

	@Override
	public EndDate getEndDate(EndDateID id)
	{
		return endDatesRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public EndDate saveEndDate(EndDate endDate)
	{
		return endDatesRepository.saveAndFlush(endDate);
	}

	@Override
	public EndDate update(EndDateID id, EndDate endDate)
	{
		EndDate endDateToUpdate = getEndDate(id);

		BeanUtils.copyProperties(endDate, endDateToUpdate);

		endDatesRepository.saveAndFlush(endDateToUpdate);

		return endDateToUpdate;
	}

	@Override
	public boolean delete(EndDateID id)
	{
		try
		{
			endDatesRepository.deleteById(id);
		}
		catch (Exception e)
		{
			return false;
		}

		return true;
	}
}
