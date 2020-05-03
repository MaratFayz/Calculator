package LD.model.DepositRate;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositRateDTO
{
	private Long company;
	private String START_PERIOD;
	private String END_PERIOD;
	private Long currency;
	private Long duration;
	private Long scenario;
	private BigDecimal RATE;
}
