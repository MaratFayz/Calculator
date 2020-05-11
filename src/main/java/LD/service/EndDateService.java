package LD.service;

import LD.model.EndDate.EndDate;
import LD.model.EndDate.EndDateDTO_in;
import LD.model.EndDate.EndDateID;

import java.util.List;

public interface EndDateService
{
	List<EndDateDTO_in> getAllEndDates();

	EndDate getEndDate(EndDateID id);

	EndDate saveEndDate(EndDate endDate);

	EndDate update(EndDateID id, EndDate endDate);

	boolean delete(EndDateID id);
}
