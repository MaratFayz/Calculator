package LD.model.LeasingDeposit;

import LD.model.Enums.STATUS_X;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LeasingDepositDTO_out
{
	private Long id;

	private Long company;

	private Long counterpartner;

	private Long currency;

	private String start_date;

	private BigDecimal deposit_sum_not_disc;

	private Long scenario;

	private STATUS_X is_created;

	private STATUS_X is_deleted;
}
