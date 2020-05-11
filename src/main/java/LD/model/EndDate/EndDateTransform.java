package LD.model.EndDate;

import LD.config.DateFormat;
import LD.service.LeasingDepositService;
import LD.service.PeriodService;
import LD.service.ScenarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static LD.config.DateFormat.parsingDate;

@Component
public class EndDateTransform
{
	@Autowired
	LeasingDepositService leasingDepositService;
	@Autowired
	ScenarioService scenarioService;
	@Autowired
	PeriodService periodService;

	public EndDate EndDatesDTO_to_EndDates(EndDateDTO_in endDateDTO_in)
	{
		EndDateID edID = EndDatesDTO_to_EndDatesID(endDateDTO_in.getScenario(),
				endDateDTO_in.getLeasingDeposit(),
				endDateDTO_in.getPeriod());

		return EndDate.builder()
				.endDateID(edID)
				.End_Date(parsingDate(endDateDTO_in.getEnd_Date()))
				.build();
	}

	public EndDateDTO_in EndDates_to_EndDatesDTO(EndDate endDate)
	{
		return EndDateDTO_in.builder()
				.End_Date(DateFormat.formatDate(endDate.getEnd_Date()))
				.leasingDeposit(endDate.getLeasingDeposit().getId())
				.scenario(endDate.endDateID.getScenario().getId())
				.period(endDate.endDateID.getPeriod().getId())
				.build();
	}

	public EndDateID EndDatesDTO_to_EndDatesID(Long scenario_id, Long leasingDeposit_id, Long period_id)
	{
		return EndDateID.builder()
				//.leasingDeposit(leasingDepositService.getLeasingDeposit(leasingDeposit_id))
				.leasingDeposit_id(leasingDeposit_id)
				.scenario(scenarioService.getScenario(scenario_id))
				.period(periodService.getPeriod(period_id))
				.build();
	}
}
