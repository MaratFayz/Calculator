package LD.model.PeriodsClosed;

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

	public PeriodsClosed PeriodsClosedDTO_to_PeriodsClosed(PeriodsClosedDTO periodsClosedDTO)
	{
		PeriodsClosedID pcID = PeriodsClosedDTO_to_PeriodsClosedID(periodsClosedDTO.getScenario(), periodsClosedDTO.getPeriod());

		return PeriodsClosed.builder()
				.periodsClosedID(pcID)
				.ISCLOSED(periodsClosedDTO.getISCLOSED())
				.build();
	}

	public PeriodsClosedDTO PeriodsClosed_to_PeriodsClosedDTO(PeriodsClosed periodsClosed)
	{
		return PeriodsClosedDTO.builder()
				.period(periodsClosed.getPeriodsClosedID().getPeriod().getId())
				.scenario(periodsClosed.getPeriodsClosedID().getScenario().getId())
				.ISCLOSED(periodsClosed.getISCLOSED())
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
