package LD.service;

import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.PeriodsClosed.PeriodsClosedID;

import java.util.List;

public interface PeriodsClosedService
{
	List<PeriodsClosed> getAllPeriodsClosed();

	PeriodsClosed getPeriodsClosed(PeriodsClosedID id);

	PeriodsClosed saveNewPeriodsClosed(PeriodsClosed periodClosed);

	PeriodsClosed updatePeriodsClosed(PeriodsClosedID id, PeriodsClosed periodClosed);

	boolean delete(PeriodsClosedID id);
}
