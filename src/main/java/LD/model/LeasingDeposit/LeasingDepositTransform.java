package LD.model.LeasingDeposit;

import LD.config.DateFormat;
import LD.service.CompanyService;
import LD.service.CounterpartnerService;
import LD.service.CurrencyService;
import LD.service.ScenarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static LD.config.DateFormat.parsingDate;

@Component
public class LeasingDepositTransform {

    @Autowired
    CompanyService companyService;
    @Autowired
    CounterpartnerService counterpartnerService;
    @Autowired
    CurrencyService currencyService;
    @Autowired
    ScenarioService scenarioService;

    public LeasingDeposit LeasingDepositDTO_in_to_LeasingDeposit(LeasingDepositDTO_in leasingDepositDTO_in) {
        return LeasingDeposit.builder()
                .company(companyService.getCompany(leasingDepositDTO_in.getCompany()))
                .counterpartner(counterpartnerService.getCounterpartner(leasingDepositDTO_in.getCounterpartner()))
                .currency(currencyService.getCurrency(leasingDepositDTO_in.getCurrency()))
                .start_date(parsingDate(leasingDepositDTO_in.getStart_date()))
                .deposit_sum_not_disc(leasingDepositDTO_in.getDeposit_sum_not_disc())
                .scenario(scenarioService.getScenario(leasingDepositDTO_in.getScenario()))
                .is_created(leasingDepositDTO_in.getIs_created())
                .is_deleted(leasingDepositDTO_in.getIs_deleted())
                .build();
    }

    public LeasingDepositDTO_out LeasingDeposit_to_LeasingDepositDTO_out(LeasingDeposit leasingDeposit) {
        return LeasingDepositDTO_out.builder()
                .id(leasingDeposit.getId())
                .company(leasingDeposit.getCompany().getId())
                .counterpartner(leasingDeposit.getCounterpartner().getId())
                .currency(leasingDeposit.getCurrency().getId())
                .start_date(DateFormat.formatDate(leasingDeposit.getStart_date()))
                .deposit_sum_not_disc(leasingDeposit.getDeposit_sum_not_disc())
                .scenario(leasingDeposit.getScenario().getId())
                .user(leasingDeposit.getUserLastChanged().getUsername())
                .lastChange(DateFormat.formatDate(leasingDeposit.getLastChange()))
                .is_created(leasingDeposit.getIs_created())
                .is_deleted(leasingDeposit.getIs_deleted())
                .build();
    }

    public LeasingDepositDTO_out_onPeriodFor2Scenarios LeasingDeposit_to_LeasingDepositDTO_out_onPeriodFor2Scenarios(LeasingDeposit leasingDeposit, LocalDate endDate) {
        return LeasingDepositDTO_out_onPeriodFor2Scenarios.builder()
                .id(leasingDeposit.getId())
                .company(leasingDeposit.getCompany().getName())
                .counterpartner(leasingDeposit.getCounterpartner().getName())
                .currency(leasingDeposit.getCurrency().getShort_name())
                .start_date(DateFormat.formatDate(leasingDeposit.getStart_date()))
                .deposit_sum_not_disc(leasingDeposit.getDeposit_sum_not_disc())
                .scenario(leasingDeposit.getScenario().getName())
                .is_created(leasingDeposit.getIs_created())
                .is_deleted(leasingDeposit.getIs_deleted())
                .username(leasingDeposit.getUserLastChanged().getUsername())
                .lastChange(DateFormat.formatDate(leasingDeposit.getLastChange()))
                .endDate(DateFormat.formatDate(endDate))
                .build();
    }
}
