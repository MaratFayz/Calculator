package LD.service;

import LD.model.Duration.Duration;
import LD.model.Duration.DurationDTO_out;

import java.util.List;

public interface DurationService
{
	List<DurationDTO_out> getAllDurations();

	Duration getDuration(Long id);

	Duration saveNewDuration(Duration duration);

	Duration updateDuration(Long id, Duration duration);

	boolean delete(Long id);
}
