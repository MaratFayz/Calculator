package LD.model.PeriodsClosed;

import LD.config.DateFormat;
import LD.service.PeriodService;
import LD.service.ScenarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PeriodsClosedTransform
{
	@Autowired
	PeriodService periodService;
	@Autowired
	ScenarioService scenarioService;

	public PeriodsClosed PeriodsClosedDTO_in_to_PeriodsClosed(PeriodsClosedDTO_in periodsClosedDTO_in)
	{
		PeriodsClosedID pcID = PeriodsClosedDTO_to_PeriodsClosedID(periodsClosedDTO_in.getScenario(), periodsClosedDTO_in.getPeriod());

		return PeriodsClosed.builder()
				.periodsClosedID(pcID)
				.ISCLOSED(periodsClosedDTO_in.getISCLOSED())
				.build();
	}

	public PeriodsClosedDTO_out PeriodsClosed_to_PeriodsClosedDTO_out(PeriodsClosed periodsClosed)
	{
		return PeriodsClosedDTO_out.builder()
				.period(periodsClosed.getPeriodsClosedID().getPeriod().getId())
				.scenario(periodsClosed.getPeriodsClosedID().getScenario().getId())
				.ISCLOSED(periodsClosed.getISCLOSED())
				.user(periodsClosed.getUser().getUsername())
				.lastChange(DateFormat.formatDate(periodsClosed.getLastChange()))
				.build();
	}

	public PeriodsClosedID PeriodsClosedDTO_to_PeriodsClosedID(Long scenario_id, Long period_id)
	{
		return PeriodsClosedID.builder()
				.period(periodService.getPeriod(period_id))
				.scenario(scenarioService.getScenario(scenario_id))
				.build();
	}
}
