package LD.model.DepositRate;

import LD.config.DataParsing;
import LD.service.CompanyService;
import LD.service.CurrencyService;
import LD.service.DurationService;
import LD.service.ScenarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DepositRateTransform
{
	@Autowired
	CompanyService companyService;
	@Autowired
	CurrencyService currencyService;
	@Autowired
	DurationService durationService;
	@Autowired
	ScenarioService scenarioService;

	public DepositRate DepositRatesDTO_to_DepositRates(DepositRateDTO depositRateDTO)
	{
		DepositRateID entryIFRSAccID = DepositRatesDTO_to_DepositRatesID(
				depositRateDTO.getCompany(),
				depositRateDTO.getCurrency(),
				depositRateDTO.getDuration(),
				depositRateDTO.getScenario(),
				depositRateDTO.getSTART_PERIOD(),
				depositRateDTO.getEND_PERIOD());

		return DepositRate.builder()
				.depositRateID(entryIFRSAccID)
				.RATE(depositRateDTO.getRATE())
				.build();
	}

	public DepositRateID DepositRatesDTO_to_DepositRatesID(Long company_id, Long currency_id, Long duration_id, Long scenario_id, String startDate, String endDate)
	{
		return DepositRateID.builder()
				.company(companyService.getCompany(company_id))
				.currency(currencyService.getCurrency(currency_id))
				.duration(durationService.getDuration(duration_id))
				.END_PERIOD(DataParsing.parsingDate(endDate))
				.scenario(scenarioService.getScenario(scenario_id))
				.START_PERIOD(DataParsing.parsingDate(startDate))
				.build();
	}
}
