package LD.model.EndDate;

import LD.config.DateFormat;
import LD.service.LeasingDepositService;
import LD.service.PeriodService;
import LD.service.ScenarioService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static LD.config.DateFormat.parsingDate;

@Component
@Log4j2
public class EndDateTransform
{
	@Autowired
	ScenarioService scenarioService;
	@Autowired
	PeriodService periodService;
	@Autowired
	LeasingDepositService leasingDepositService;

	public EndDate EndDatesDTO_in_to_EndDates(EndDateDTO_in endDateDTO_in)
	{
		log.info("endDateDTO_in = {}", endDateDTO_in);

		EndDateID edID = EndDatesDTO_to_EndDatesID(endDateDTO_in.getScenario(),
				endDateDTO_in.getLeasingDeposit_id(),
				endDateDTO_in.getPeriod());

		log.info("edID = {}", edID);

		return EndDate.builder()
				.endDateID(edID)
				.endDate(parsingDate(endDateDTO_in.getEnd_Date()))
				.leasingDeposit(leasingDepositService.getLeasingDeposit(endDateDTO_in.getLeasingDeposit_id()))
				.build();
	}

	public EndDateDTO_out EndDates_to_EndDatesDTO_out(EndDate endDate)
	{
		return EndDateDTO_out.builder()
				.End_Date(DateFormat.formatDate(endDate.getEndDate()))
				.leasingDeposit_id(endDate.getLeasingDeposit().getId())
				.scenario(endDate.endDateID.getScenario().getId())
				.period(endDate.endDateID.getPeriod().getId())
				.user(endDate.getUserLastChanged().getUsername())
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
