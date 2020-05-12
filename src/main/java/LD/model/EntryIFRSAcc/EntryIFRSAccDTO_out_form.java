package LD.model.EntryIFRSAcc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class EntryIFRSAccDTO_out_form
{
	private Long leasingDeposit;
	private Long scenario;
	private Long period;
	private String CALCULATION_TIME;

	private Long ifrsAccount;
	private BigDecimal sum;
}
