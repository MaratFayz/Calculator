package LD.model.ExchangeRate;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExchangeRateDTO
{
	private Long currency;
	private Long scenario;
	private String date;
	private BigDecimal rate_at_date;
	private BigDecimal average_rate_for_month;
}
