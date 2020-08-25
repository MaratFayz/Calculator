package testEntityCreation;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositRateTestData {

    Long companyCode;
    String start_PERIOD;
    String end_PERIOD;
    Long currencyCode;
    Long durationCode;
    Long scenarioCode;
    BigDecimal rate;
    Long userCode;
    String lastChange;
}
