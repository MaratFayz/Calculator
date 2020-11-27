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
public class EntryDTO_out_RegLD3 {

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
