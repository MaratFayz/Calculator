package LD.model.EndDate;

import LD.service.LeasingDepositService;
import LD.service.PeriodService;
import LD.service.ScenarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static LD.config.DataParsing.parsingDate;

@Component
public class EndDateTransform
{
	@Autowired
	LeasingDepositService leasingDepositService;
	@Autowired
	ScenarioService scenarioService;
	@Autowired
	PeriodService periodService;

	public EndDate EndDatesDTO_to_EndDates(EndDateDTO endDateDTO)
	{
		EndDateID edID = EndDatesDTO_to_EndDatesID(endDateDTO.getScenario(),
				endDateDTO.getLeasingDeposit(),
				endDateDTO.getPeriod());

		return EndDate.builder()
				.endDateID(edID)
				.End_Date(parsingDate(endDateDTO.getEnd_Date()))
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
