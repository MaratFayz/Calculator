package LD.model.Entry;

import LD.model.Enums.EntryPeriodCreation;
import LD.model.Enums.EntryStatus;
import LD.model.Enums.LeasingDepositDuration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EntryDTO_out_RegLD2
{
	private Long leasingDeposit;
	private Long scenario;
	private Long period;
	private String CALCULATION_TIME;
	private String end_date_at_this_period;
	private EntryStatus status;
	private EntryPeriodCreation Status_EntryMadeDuringOrAfterClosedPeriod;
	private BigDecimal ACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H;
	private BigDecimal AMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I;
	private BigDecimal ACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J;
	private BigDecimal ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K;
	private BigDecimal AMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M;
	private BigDecimal ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N;
}
