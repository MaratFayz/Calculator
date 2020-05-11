package LD.service;

import LD.model.EndDate.EndDate;
import LD.model.EndDate.EndDateDTO_in;
import LD.model.EndDate.EndDateID;
import LD.model.EndDate.EndDateTransform;
import LD.repository.EndDatesRepository;
import LD.rest.exceptions.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EndDateServiceImpl implements EndDateService
{
	@Autowired
	EndDatesRepository endDatesRepository;
	@Autowired
	EndDateTransform endDateTransform;

	@Override
	public List<EndDateDTO_in> getAllEndDates()
	{
		return endDatesRepository.findAll()
				.stream()
				.map(ed -> endDateTransform.EndDates_to_EndDatesDTO(ed))
				.collect(Collectors.toList());
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
