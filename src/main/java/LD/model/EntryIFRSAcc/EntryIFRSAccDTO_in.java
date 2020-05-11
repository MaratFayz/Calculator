package LD.model.EntryIFRSAcc;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EntryIFRSAccDTO_in
{
	private Long leasingDeposit;
	private Long scenario;
	private Long period;
	private String CALCULATION_TIME;

	private Long ifrsAccount;
	private BigDecimal sum;
}
