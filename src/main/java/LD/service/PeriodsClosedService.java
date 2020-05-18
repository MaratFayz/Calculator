package LD.service;

import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.PeriodsClosed.PeriodsClosedDTO_in;
import LD.model.PeriodsClosed.PeriodsClosedDTO_out;
import LD.model.PeriodsClosed.PeriodsClosedID;

import java.util.List;

public interface PeriodsClosedService
{
	List<PeriodsClosedDTO_out> getAllPeriodsClosed();

	PeriodsClosed getPeriodsClosed(PeriodsClosedID id);

	String getDateFirstOpenPeriodForScenario(Long scenario_id);

	PeriodsClosed saveNewPeriodsClosed(PeriodsClosed periodClosed);

	PeriodsClosed updatePeriodsClosed(PeriodsClosedID id, PeriodsClosed periodClosed);

	boolean delete(PeriodsClosedID id);

	void autoClosePeriods(String dateBeforeToClose, long scenario_id);
}
