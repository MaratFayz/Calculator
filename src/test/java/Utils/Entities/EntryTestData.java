package Utils.Entities;

import LD.model.Enums.EntryPeriodCreation;
import LD.model.Enums.EntryStatus;
import LD.model.Enums.LeasingDepositDuration;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EntryTestData {

    private Long leasingDepositCode;
    private Long scenarioCode;
    private String period;
    private String calculation_time;
    private String end_date_at_this_period;
    private EntryStatus status;
    private String lastChange;
    private BigDecimal percentRateForPeriodForLD;
    private EntryPeriodCreation Status_EntryMadeDuringOrAfterClosedPeriod;
    private BigDecimal discont_at_start_date_cur_reg_ld_1_k;
    private BigDecimal discont_at_start_date_rub_reg_ld_1_l;
    private BigDecimal discont_at_start_date_rub_forifrsacc_reg_ld_1_m;
    private BigDecimal deposit_sum_not_disc_rub_reg_ld_1_n;
    private BigDecimal discont_sum_at_new_end_date_cur_reg_ld_1_p;
    private BigDecimal disc_sum_at_new_end_date_rub_reg_ld_1_q;
    private BigDecimal disc_diff_betw_disconts_rub_reg_ld_1_r;
    private BigDecimal reval_corr_disc_rub_reg_ld_1_s;
    private BigDecimal corr_acc_amort_disc_rub_reg_ld_1_t;
    private BigDecimal corr_new_date_less_corr_acc_amort_disc_rub_reg_ld_1_x;
    private BigDecimal corr_new_date_higher_discont_rub_reg_ld_1_u;
    private BigDecimal corr_new_date_higher_corr_acc_amort_disc_rub_reg_ld_1_v;
    private BigDecimal corr_new_date_less_discont_rub_reg_ld_1_w;
    private BigDecimal accum_amort_discont_start_period_cur_reg_ld_2_h;
    private BigDecimal amort_discont_current_period_cur_reg_ld_2_i;
    private BigDecimal accum_amort_discont_end_period_cur_reg_ld_2_j;
    private BigDecimal accum_amort_discont_start_period_rub_reg_ld_2_k;
    private BigDecimal amort_discont_current_period_rub_reg_ld_2_m;
    private BigDecimal accum_amort_discont_end_period_rub_reg_ld_2_n;
    private BigDecimal discountedsum_at_current_end_date_cur_reg_ld_3_g;
    private BigDecimal incoming_ld_body_rub_reg_ld_3_l;
    private BigDecimal outcoming_ld_body_reg_ld_3_m;
    private BigDecimal reval_ld_body_plus_reg_ld_3_n;
    private BigDecimal reval_ld_body_minus_reg_ld_3_o;
    private BigDecimal accum_amort_discont_start_period_rub_reg_ld_3_r;
    private BigDecimal accum_amort_discont_end_period_rub_reg_ld_3_s;
    private BigDecimal reval_acc_amort_plus_rub_reg_ld_3_t;
    private BigDecimal reval_acc_amort_minus_rub_reg_ld_3_u;
    private BigDecimal sum_plus_forex_diff_reg_ld_3_v;
    private BigDecimal sum_minus_forex_diff_reg_ld_3_w;
    private BigDecimal disposal_body_rub_reg_ld_3_x;
    private BigDecimal disposal_discont_rub_reg_ld_3_y;
    private LeasingDepositDuration ldterm_reg_ld_3_z;
    private BigDecimal termreclass_body_currentperiod_reg_ld_3_aa;
    private BigDecimal termreclass_percent_currentperiod_reg_ld_3_ab;
    private BigDecimal termreclass_body_prevperiod_reg_ld_3_ac;
    private BigDecimal termreclass_percent_prevperiod_reg_ld_3_ad;
    private BigDecimal advance_currentperiod_reg_ld_3_ae;
    private BigDecimal advance_prevperiod_reg_ld_3_af;
}
