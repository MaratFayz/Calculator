package LD.service;

import LD.model.Duration.Duration;
import LD.repository.DurationRepository;
import LD.rest.exceptions.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DurationServiceImpl implements DurationService
{
	@Autowired
	DurationRepository durationRepository;

	@Override
	public List<Duration> getAllDurations()
	{
		return durationRepository.findAll();
	}

	@Override
	public Duration getDuration(Long id)
	{
		return durationRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public Duration saveNewDuration(Duration duration)
	{
		return durationRepository.save(duration);
	}

	@Override
	public Duration updateDuration(Long id, Duration duration)
	{
		Duration durationToUpdate = getDuration(id);

		BeanUtils.copyProperties(duration, durationToUpdate);

		durationRepository.saveAndFlush(durationToUpdate);

		return durationToUpdate;
	}

	@Override
	public boolean delete(Long id)
	{
		try
		{
			durationRepository.deleteById(id);
		}
		catch (Exception e)
		{
			return false;
		}

		return true;
	}

}
