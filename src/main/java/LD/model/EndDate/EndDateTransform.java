package LD.model.EndDate;

import LD.config.DateFormat;
import LD.service.PeriodService;
import LD.service.ScenarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static LD.config.DateFormat.parsingDate;

@Component
public class EndDateTransform
{
	@Autowired
	ScenarioService scenarioService;
	@Autowired
	PeriodService periodService;

	public EndDate EndDatesDTO_in_to_EndDates(EndDateDTO_in endDateDTO_in)
	{
		EndDateID edID = EndDatesDTO_to_EndDatesID(endDateDTO_in.getScenario(),
				endDateDTO_in.getLeasingDeposit(),
				endDateDTO_in.getPeriod());

		return EndDate.builder()
				.endDateID(edID)
				.End_Date(parsingDate(endDateDTO_in.getEnd_Date()))
				.build();
	}

	public EndDateDTO_out EndDates_to_EndDatesDTO_out(EndDate endDate)
	{
		return EndDateDTO_out.builder()
				.End_Date(DateFormat.formatDate(endDate.getEnd_Date()))
				.leasingDeposit_id(endDate.getLeasingDeposit().getId())
				.scenario(endDate.endDateID.getScenario().getId())
				.period(endDate.endDateID.getPeriod().getId())
				.user(endDate.getUser().getUsername())
				.lastChange(DateFormat.formatDate(endDate.getLastChange()))
				.build();
	}

	public EndDateID EndDatesDTO_to_EndDatesID(Long scenario_id, Long leasingDeposit_id, Long period_id)
	{
		return EndDateID.builder()
				.leasingDeposit_id(leasingDeposit_id)
				.scenario(scenarioService.getScenario(scenario_id))
				.period(periodService.getPeriod(period_id))
				.build();
	}
}
