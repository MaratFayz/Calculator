package LD.model.Entry;

import LD.model.Enums.EntryPeriodCreation;
import LD.model.Enums.EntryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EntryDTO_out_RegLD1 {

    private Long leasingDeposit;
    private String scenario;
    private String period;
    private String user;
    private String lastChange;
    private String CALCULATION_TIME;
    private String end_date_at_this_period;
    private BigDecimal percentRateForPeriodForLD;
    private EntryStatus status;
    private EntryPeriodCreation Status_EntryMadeDuringOrAfterClosedPeriod;
    private BigDecimal DISCONT_AT_START_DATE_cur_REG_LD_1_K;
    private BigDecimal DISCONT_AT_START_DATE_RUB_REG_LD_1_L;
    private BigDecimal DISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M;
    private BigDecimal deposit_sum_not_disc_RUB_REG_LD_1_N;
    private BigDecimal DISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P;
    private BigDecimal DISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q;
    private BigDecimal DISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R;
    private BigDecimal REVAL_CORR_DISC_rub_REG_LD_1_S;
    private BigDecimal CORR_ACC_AMORT_DISC_rub_REG_LD_1_T;
    private BigDecimal CORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X;
    private BigDecimal CORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U;
    private BigDecimal CORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V;
    private BigDecimal CORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W;
}
