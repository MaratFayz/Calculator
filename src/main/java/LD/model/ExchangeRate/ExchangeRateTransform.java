package LD.model.ExchangeRate;

import LD.config.DateFormat;
import LD.service.CurrencyService;
import LD.service.ScenarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static LD.config.DateFormat.parsingDate;

@Component
public class ExchangeRateTransform
{
	@Autowired
	CurrencyService currencyService;
	@Autowired
	ScenarioService scenarioService;

	public ExchangeRate ExchangeRateDTO_to_ExchangeRate(ExchangeRateDTO exchangeRateDTO)
	{
		ExchangeRateID erID = ExchangeRateDTO_to_ExchangeRateKeyInER(exchangeRateDTO.getScenario(),
																	exchangeRateDTO.getCurrency(),
																	exchangeRateDTO.getDate());

		return ExchangeRate.builder()
				.exchangeRateID(erID)
				.rate_at_date(exchangeRateDTO.getRate_at_date())
				.average_rate_for_month(exchangeRateDTO.getAverage_rate_for_month())
				.build();
	}

	public ExchangeRateDTO ExchangeRate_to_ExchangeRateDTO(ExchangeRate exchangeRate)
	{
		return ExchangeRateDTO.builder()
				.currency(exchangeRate.getExchangeRateID().getCurrency().getId())
				.scenario(exchangeRate.getExchangeRateID().getScenario().getId())
				.date(DateFormat.formatDate(exchangeRate.getExchangeRateID().getDate()))
				.rate_at_date(exchangeRate.getRate_at_date())
				.average_rate_for_month(exchangeRate.getAverage_rate_for_month())
				.build();
	}

	public ExchangeRateID ExchangeRateDTO_to_ExchangeRateKeyInER(Long scenario_id, Long currency_id, String date)
	{
		return ExchangeRateID.builder()
				.currency(currencyService.getCurrency(currency_id))
				.scenario(scenarioService.getScenario(scenario_id))
				.date(parsingDate(date)) //format yyyy-mm-dd
				.build();
	}
}
