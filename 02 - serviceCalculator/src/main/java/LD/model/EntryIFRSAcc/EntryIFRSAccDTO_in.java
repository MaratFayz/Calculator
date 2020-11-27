package LD.model.EntryIFRSAcc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EntryIFRSAccDTO_in
{
	private Long leasingDeposit;
	private Long scenario;
	private Long period;
	private String CALCULATION_TIME;

	private Long ifrsAccount;
	private BigDecimal sum;
}
