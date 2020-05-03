package LD.model.Entry;

import LD.service.LeasingDepositService;
import LD.service.PeriodService;
import LD.service.ScenarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static LD.config.DataParsing.parsingDate;

@Component
public class EntryTransform
{
	@Autowired
	LeasingDepositService leasingDepositService;
	@Autowired
	ScenarioService scenarioService;
	@Autowired
	PeriodService periodService;

	public Entry EntryDTO_to_Entry(EntryDTO entryDTO)
	{
		EntryID entryID = EntryDTO_to_EntryID(entryDTO.getScenario(),
				entryDTO.getLeasingDeposit(),
				entryDTO.getPeriod(),
				entryDTO.getCALCULATION_TIME());

		return Entry.builder()
				.entryID(entryID)
				.end_date_at_this_period(parsingDate(entryDTO.getEnd_date_at_this_period()))
				.status(entryDTO.getStatus())
				.Status_EntryMadeDuringOrAfterClosedPeriod(entryDTO.getStatus_EntryMadeDuringOrAfterClosedPeriod())
				.DISCONT_AT_START_DATE_cur_REG_LD_1_K(entryDTO.getDISCONT_AT_START_DATE_cur_REG_LD_1_K())
				.DISCONT_AT_START_DATE_RUB_REG_LD_1_L(entryDTO.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L())
				.DISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M(entryDTO.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M())
				.deposit_sum_not_disc_RUB_REG_LD_1_N(entryDTO.getDeposit_sum_not_disc_RUB_REG_LD_1_N())
				.DISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P(entryDTO.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P())
				.DISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q(entryDTO.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q())
				.DISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(entryDTO.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R())
				.REVAL_CORR_DISC_rub_REG_LD_1_S(entryDTO.getREVAL_CORR_DISC_rub_REG_LD_1_S())
				.CORR_ACC_AMORT_DISC_rub_REG_LD_1_T(entryDTO.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T())
				.CORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X(entryDTO.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X())
				.CORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U(entryDTO.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U())
				.CORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V(entryDTO.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V())
				.CORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W(entryDTO.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W())
				.ACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H(entryDTO.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H())
				.AMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I(entryDTO.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I())
				.ACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(entryDTO.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J())
				.ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(entryDTO.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K())
				.AMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M(entryDTO.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M())
				.ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N(entryDTO.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N())
				.discountedSum_at_current_end_date_cur_REG_LD_3_G(entryDTO.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G())
				.INCOMING_LD_BODY_RUB_REG_LD_3_L(entryDTO.getINCOMING_LD_BODY_RUB_REG_LD_3_L())
				.OUTCOMING_LD_BODY_REG_LD_3_M(entryDTO.getOUTCOMING_LD_BODY_REG_LD_3_M())
				.REVAL_LD_BODY_PLUS_REG_LD_3_N(entryDTO.getREVAL_LD_BODY_PLUS_REG_LD_3_N())
				.REVAL_LD_BODY_MINUS_REG_LD_3_O(entryDTO.getREVAL_LD_BODY_MINUS_REG_LD_3_O())
				.ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R(entryDTO.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R())
				.ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S(entryDTO.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S())
				.REVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T(entryDTO.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T())
				.REVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U(entryDTO.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U())
				.SUM_PLUS_FOREX_DIFF_REG_LD_3_V(entryDTO.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V())
				.SUM_MINUS_FOREX_DIFF_REG_LD_3_W(entryDTO.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W())
				.DISPOSAL_BODY_RUB_REG_LD_3_X(entryDTO.getDISPOSAL_BODY_RUB_REG_LD_3_X())
				.DISPOSAL_DISCONT_RUB_REG_LD_3_Y(entryDTO.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y())
				.LDTERM_REG_LD_3_Z(entryDTO.getLDTERM_REG_LD_3_Z())
				.TERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA(entryDTO.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA())
				.TERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB(entryDTO.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB())
				.TERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC(entryDTO.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC())
				.TERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD(entryDTO.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD())
				.ADVANCE_CURRENTPERIOD_REG_LD_3_AE(entryDTO.getADVANCE_CURRENTPERIOD_REG_LD_3_AE())
				.ADVANCE_PREVPERIOD_REG_LD_3_AF(entryDTO.getADVANCE_PREVPERIOD_REG_LD_3_AF())
				.build();
	}

	public EntryID EntryDTO_to_EntryID(Long scenario_id, Long leasingDeposit_id, Long period_id, String date)
	{
		return EntryID.builder()
				.leasingDeposit_id(leasingDeposit_id)
				.scenario(scenarioService.getScenario(scenario_id))
				.period(periodService.getPeriod(period_id))
				.CALCULATION_TIME(parsingDate(date))
				.build();
	}
}
