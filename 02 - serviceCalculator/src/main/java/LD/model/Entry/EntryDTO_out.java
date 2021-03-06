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
public class EntryDTO_out {

    private Long leasingDeposit;
    private Long scenario;
    private Long period;
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
    private BigDecimal ACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H;
    private BigDecimal AMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I;
    private BigDecimal ACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J;
    private BigDecimal ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K;
    private BigDecimal AMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M;
    private BigDecimal ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N;
    private BigDecimal discountedSum_at_current_end_date_cur_REG_LD_3_G;
    private BigDecimal INCOMING_LD_BODY_RUB_REG_LD_3_L;
    private BigDecimal OUTCOMING_LD_BODY_REG_LD_3_M;
    private BigDecimal REVAL_LD_BODY_PLUS_REG_LD_3_N;
    private BigDecimal REVAL_LD_BODY_MINUS_REG_LD_3_O;
    private BigDecimal ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R;
    private BigDecimal ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S;
    private BigDecimal REVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T;
    private BigDecimal REVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U;
    private BigDecimal SUM_PLUS_FOREX_DIFF_REG_LD_3_V;
    private BigDecimal SUM_MINUS_FOREX_DIFF_REG_LD_3_W;
    private BigDecimal DISPOSAL_BODY_RUB_REG_LD_3_X;
    private BigDecimal DISPOSAL_DISCONT_RUB_REG_LD_3_Y;
    private LeasingDepositDuration LDTERM_REG_LD_3_Z;
    private BigDecimal TERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA;
    private BigDecimal TERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB;
    private BigDecimal TERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC;
    private BigDecimal TERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD;
    private BigDecimal ADVANCE_CURRENTPERIOD_REG_LD_3_AE;
    private BigDecimal ADVANCE_PREVPERIOD_REG_LD_3_AF;
}
