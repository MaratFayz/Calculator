package LD.model.LeasingDeposit;

import LD.config.DateFormat;
import LD.service.CompanyService;
import LD.service.CounterpartnerService;
import LD.service.CurrencyService;
import LD.service.ScenarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static LD.config.DateFormat.parsingDate;

@Component
public class LeasingDepositTransform
{
	@Autowired
	CompanyService companyService;
	@Autowired
	CounterpartnerService counterpartnerService;
	@Autowired
	CurrencyService currencyService;
	@Autowired
	ScenarioService scenarioService;

	public LeasingDeposit LeasingDepositDTO_to_LeasingDeposit(LeasingDepositDTO leasingDepositDTO)
	{
		return LeasingDeposit.builder()
				.company(companyService.getCompany(leasingDepositDTO.getCompany()))
				.counterpartner(counterpartnerService.getCounterpartner(leasingDepositDTO.getCounterpartner()))
				.currency(currencyService.getCurrency(leasingDepositDTO.getCurrency()))
				.start_date(parsingDate(leasingDepositDTO.getStart_date()))
				.deposit_sum_not_disc(leasingDepositDTO.getDeposit_sum_not_disc())
				.scenario(scenarioService.getScenario(leasingDepositDTO.getScenario()))
				.is_created(leasingDepositDTO.getIs_created())
				.is_deleted(leasingDepositDTO.getIs_deleted())
				.build();
	}

	public LeasingDepositDTO LeasingDeposit_to_LeasingDepositDTO(LeasingDeposit leasingDeposit)
	{
		return LeasingDepositDTO.builder()
				.company(leasingDeposit.getCompany().getId())
				.counterpartner(leasingDeposit.getCounterpartner().getId())
				.currency(leasingDeposit.getCurrency().getId())
				.start_date(DateFormat.formatDate(leasingDeposit.getStart_date()))
				.deposit_sum_not_disc(leasingDeposit.getDeposit_sum_not_disc())
				.scenario(leasingDeposit.getScenario().getId())
				.is_created(leasingDeposit.getIs_created())
				.is_deleted(leasingDeposit.getIs_deleted())
				.build();
	}
}
