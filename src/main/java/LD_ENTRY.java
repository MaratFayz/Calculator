import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "entries")
@Getter
@Setter
@ToString
@NoArgsConstructor()
@IdClass(value = LD_ENTRY.KeyInTransaction.class)
public class LD_ENTRY
{
	@Id
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "ld_id", nullable = false)
	private LD ld;

	@Id
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "scenario_id")
	private SCENARIO scenario;

	@Id
	private ZonedDateTime CALCULATION_TIME;

	@Id
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "period_id", nullable = false)
	private PERIOD period;

	@Column(name = "end_date_at_this_period", nullable = false, columnDefinition = "DATE")
	private ZonedDateTime end_date_at_this_period;

	@Column(name = "transaction_status", columnDefinition = "enum('ACTUAL', 'STORNO', 'DELETED')")
	@Enumerated(value = EnumType.STRING)
	private TRAN_STATUS status;

	@Column(name = "Status_EntryMadeDuringOrAfterClosedPeriod", columnDefinition = "enum('CURRENT_PERIOD', 'AFTER_CLOSING_PERIOD')", nullable = false)
	@Enumerated(value = EnumType.STRING)
	private TRAN_PER Status_EntryMadeDuringOrAfterClosedPeriod;

	@Column(name = "DISCONT_AT_START_DATE_cur_REG_LD_1_K", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal DISCONT_AT_START_DATE_cur_REG_LD_1_K;

	@Column(name = "DISCONT_AT_START_DATE_RUB_REG_LD_1_L", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal DISCONT_AT_START_DATE_RUB_REG_LD_1_L;

	@Column(name = "DISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal DISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M;

	@Column(name = "deposit_sum_not_disc_RUB_REG_LD_1_N", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal deposit_sum_not_disc_RUB_REG_LD_1_N;

	@Column(name = "DISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal DISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P;

	@Column(name = "DISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal DISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q;

	@Column(name = "DISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal DISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R;

	@Column(name = "REVAL_CORR_DISC_rub_REG_LD_1_S", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal REVAL_CORR_DISC_rub_REG_LD_1_S;

	@Column(name = "CORR_ACC_AMORT_DISC_rub_REG_LD_1_T", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal CORR_ACC_AMORT_DISC_rub_REG_LD_1_T;

	@Column(name = "CORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal CORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X;

	@Column(name = "CORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal CORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U;

	@Column(name = "CORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal CORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V;

	@Column(name = "CORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal CORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W;

	@Column(name = "ACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal ACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H;

	@Column(name = "AMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal AMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I;

	@Column(name = "ACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal ACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J;

	@Column(name = "ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K;

	@Column(name = "AMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal AMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M;

	@Column(name = "ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N;

	@Column(name = "discountedSum_at_current_end_date_cur_REG_LD_3_G", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal discountedSum_at_current_end_date_cur_REG_LD_3_G;

	@Column(name = "INCOMING_LD_BODY_RUB_REG_LD_3_L", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal INCOMING_LD_BODY_RUB_REG_LD_3_L;

	@Column(name = "OUTCOMING_LD_BODY_REG_LD_3_M", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal OUTCOMING_LD_BODY_REG_LD_3_M;

	@Column(name = "REVAL_LD_BODY_PLUS_REG_LD_3_N", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal REVAL_LD_BODY_PLUS_REG_LD_3_N;

	@Column(name = "REVAL_LD_BODY_MINUS_REG_LD_3_O", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal REVAL_LD_BODY_MINUS_REG_LD_3_O;

	@Column(name = "ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R;

	@Column(name = "ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S;

	@Column(name = "REVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal REVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T;

	@Column(name = "REVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal REVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U;

	@Column(name = "SUM_PLUS_FOREX_DIFF_REG_LD_3_V", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal SUM_PLUS_FOREX_DIFF_REG_LD_3_V;

	@Column(name = "SUM_MINUS_FOREX_DIFF_REG_LD_W", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal SUM_MINUS_FOREX_DIFF_REG_LD_3_W;

	@Column(name = "DISPOSAL_BODY_RUB_REG_LD_3_X", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal DISPOSAL_BODY_RUB_REG_LD_3_X;

	@Column(name = "DISPOSAL_DISCONT_RUB_REG_LD_3_Y", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal DISPOSAL_DISCONT_RUB_REG_LD_3_Y;

	@Column(name = "LDTERM_REG_LD_3_Z", nullable = false, columnDefinition = "enum('ST','LT')")
	@Enumerated(value = EnumType.STRING)
	private TERM_ST_LT LDTERM_REG_LD_3_Z;

	@Column(name = "TERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal TERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA;

	@Column(name = "TERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal TERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB;

	@Column(name = "TERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal TERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC;

	@Column(name = "TERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal TERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD;

	@Column(name = "ADVANCE_CURRENTPERIOD_REG_LD_3_AE", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal ADVANCE_CURRENTPERIOD_REG_LD_3_AE;

	@Column(name = "ADVANCE_PREVPERIOD_REG_LD_3_AF", nullable = false, columnDefinition = "DECIMAL(30,10)")
	private BigDecimal ADVANCE_PREVPERIOD_REG_LD_3_AF;

	@Data
	public static class KeyInTransaction implements Serializable
	{
		static final long serialVersionUID = 6L;

		private LD ld;
		private SCENARIO scenario;
		private PERIOD period;
		private ZonedDateTime CALCULATION_TIME;
	}

}
