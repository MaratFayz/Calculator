package LD.model.DepositRate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositRateDTO_in
{
	private Long company;
	private String START_PERIOD;
	private String END_PERIOD;
	private Long currency;
	private Long duration;
	private Long scenario;
	private BigDecimal RATE;
}
