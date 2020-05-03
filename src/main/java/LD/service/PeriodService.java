package LD.service;

import LD.model.Period.Period;

import java.util.List;

public interface PeriodService
{
	List<Period> getAllPeriods();

	Period getPeriod(Long id);

	Period saveNewPeriod(Period period);

	Period updatePeriod(Long id, Period period);

	boolean delete(Long id);
}
