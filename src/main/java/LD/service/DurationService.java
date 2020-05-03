package LD.service;

import LD.model.Duration.Duration;

import java.util.List;

public interface DurationService
{
	List<Duration> getAllDurations();

	Duration getDuration(Long id);

	Duration saveNewDuration(Duration duration);

	Duration updateDuration(Long id, Duration duration);

	boolean delete(Long id);
}
