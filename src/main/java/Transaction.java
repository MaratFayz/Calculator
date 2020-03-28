import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "transactions")
@Data
@Builder(toBuilder = true)
public class Transaction
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private int id;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "ld_id", nullable = false)
	private LD ld;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "scenario_id")
	private SCENARIO scenario;

//	CALCULATION_TIME datetime PK
//	STATUS enum('DELETED CUR PER','ACTUAL','STORNO CUR PER','STORNO AFT CLOSED PER') PK
//	DISC_SUM_AT_START_DATE double
//	USER varchar(45)
//	CHANGE_NEW_DISCONT double
//	CHANGE_NEW_DISCONT_RUB double
//	CHANGE_NEW_CHANGE_IN_DISCONT_RUB double
//	CHANGE_NEW_CHANGE_CUR_IN_DISCONT_RUB double
//	CHANGE_NEW_CHANGE_IN_CUMAMORT_RUB double
//	SUM_CORR_NEW_DATE_bigger_DISC double
//	SUM_CORR_NEW_DATE_bigger_CH_IN_CUMAMORT double
//	SUM_CORR_NEW_DATE_lower_DISC double
//	SUM_CORR_NEW_DATE_lower_CH_IN_CUMAMORT double
//	SUM_ACC_AMORT_START_PERIOD double
//	SUM_ACC_AMORT_END_PERIOD double
//	SUM_AMORT_CURRENT_PERIOD double
//	SUM_ACC_AMORT_START_PERIOD_RUB double
//	SUM_ACC_AMORT_END_PERIOD_RUB double
//	SUM_AMORT_CURRENT_PERIOD_RUB double
//	AVERAGE_CURRENCY_RATE_CURR_PER double
//	REVAL_BODY_START_RUB double
//	REVAL_BODY_END_RUB double
//	REVAL_BODY_POSITIVE double
//	REVAL_BODY_NEGATIVE double
//	REVAL_AMORT_DISC_START_RUB double
//	REVA_AMORT_DISC_END_RUB double
//	REVAL_AMORT_DISC_START double
//	REVA_AMORT_DISC_END double
//	REVAL_AMORT_DISC_POSITIVE double
//	REVAL_AMORT_DISC_NEGATIVE double
//	SUM_REVAL_POSITIVE_RUB double
//	SUM_REVAL_NEGATIVE_RUB double
//	SUM_DISPOSAL_BODY_RUB double
//	SUM_DISPOSAL_AMORT_DISC_RUB double
//	DEPOSIT_SHORT_LONG_TERM enum('ST','LT')
//	SUM_RECLASS_BODY_CURR_PERIOD double
//	SUM_RECLASS_PERCENT_CURR_PERIOD double
//	SUM_RECLASS_BODY_PREV_PERIOD double
//	SUM_RECLASS_PERCENT_PREV_PERIOD double
//	SUM_ADVANCE_PREV_PERIOD double
//	SUM_ADVANCE_CURR_PERIOD double

}
