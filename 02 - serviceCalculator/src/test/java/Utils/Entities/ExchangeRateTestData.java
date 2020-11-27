package Utils.Entities;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExchangeRateTestData {

    Long currencyCode;
    Long scenarioCode;
    String date;
    BigDecimal rate_at_date;
    BigDecimal average_rate_for_month;
}
