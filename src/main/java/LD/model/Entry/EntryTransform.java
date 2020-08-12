package LD.model.Entry;

import LD.config.DateFormat;
import LD.service.PeriodService;
import LD.service.ScenarioService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static LD.config.DateFormat.formatDate;
import static LD.config.DateFormat.parsingDate;

@Component
@Log4j2
public class EntryTransform
{
	@Autowired
	ScenarioService scenarioService;
	@Autowired
	PeriodService periodService;

	public Entry EntryDTO_in_to_Entry(EntryDTO_in entryDTO_in)
	{
		EntryID entryID = getEntryID(entryDTO_in.getScenario(),
				entryDTO_in.getLeasingDeposit(),
				entryDTO_in.getPeriod(),
				entryDTO_in.getCALCULATION_TIME());

		return Entry.builder()
				.entryID(entryID)
				.end_date_at_this_period(parsingDate(entryDTO_in.getEnd_date_at_this_period()))
				.status(entryDTO_in.getStatus())
				.Status_EntryMadeDuringOrAfterClosedPeriod(entryDTO_in.getStatus_EntryMadeDuringOrAfterClosedPeriod())
				.percentRateForPeriodForLD(entryDTO_in.getPercentRateForPeriodForLD())
				.DISCONT_AT_START_DATE_cur_REG_LD_1_K(entryDTO_in.getDISCONT_AT_START_DATE_cur_REG_LD_1_K())
				.DISCONT_AT_START_DATE_RUB_REG_LD_1_L(entryDTO_in.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L())
				.DISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M(entryDTO_in.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M())
				.deposit_sum_not_disc_RUB_REG_LD_1_N(entryDTO_in.getDeposit_sum_not_disc_RUB_REG_LD_1_N())
				.DISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P(entryDTO_in.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P())
				.DISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q(entryDTO_in.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q())
				.DISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(entryDTO_in.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R())
				.REVAL_CORR_DISC_rub_REG_LD_1_S(entryDTO_in.getREVAL_CORR_DISC_rub_REG_LD_1_S())
				.CORR_ACC_AMORT_DISC_rub_REG_LD_1_T(entryDTO_in.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T())
				.CORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X(entryDTO_in.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X())
				.CORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U(entryDTO_in.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U())
				.CORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V(entryDTO_in.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V())
				.CORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W(entryDTO_in.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W())
				.ACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H(entryDTO_in.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H())
				.AMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I(entryDTO_in.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I())
				.ACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(entryDTO_in.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J())
				.ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(entryDTO_in.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K())
				.AMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M(entryDTO_in.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M())
				.ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N(entryDTO_in.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N())
				.discountedSum_at_current_end_date_cur_REG_LD_3_G(entryDTO_in.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G())
				.INCOMING_LD_BODY_RUB_REG_LD_3_L(entryDTO_in.getINCOMING_LD_BODY_RUB_REG_LD_3_L())
				.OUTCOMING_LD_BODY_REG_LD_3_M(entryDTO_in.getOUTCOMING_LD_BODY_REG_LD_3_M())
				.REVAL_LD_BODY_PLUS_REG_LD_3_N(entryDTO_in.getREVAL_LD_BODY_PLUS_REG_LD_3_N())
				.REVAL_LD_BODY_MINUS_REG_LD_3_O(entryDTO_in.getREVAL_LD_BODY_MINUS_REG_LD_3_O())
				.ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R(entryDTO_in.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R())
				.ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S(entryDTO_in.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S())
				.REVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T(entryDTO_in.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T())
				.REVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U(entryDTO_in.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U())
				.SUM_PLUS_FOREX_DIFF_REG_LD_3_V(entryDTO_in.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V())
				.SUM_MINUS_FOREX_DIFF_REG_LD_3_W(entryDTO_in.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W())
				.DISPOSAL_BODY_RUB_REG_LD_3_X(entryDTO_in.getDISPOSAL_BODY_RUB_REG_LD_3_X())
				.DISPOSAL_DISCONT_RUB_REG_LD_3_Y(entryDTO_in.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y())
				.LDTERM_REG_LD_3_Z(entryDTO_in.getLDTERM_REG_LD_3_Z())
				.TERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA(entryDTO_in.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA())
				.TERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB(entryDTO_in.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB())
				.TERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC(entryDTO_in.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC())
				.TERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD(entryDTO_in.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD())
				.ADVANCE_CURRENTPERIOD_REG_LD_3_AE(entryDTO_in.getADVANCE_CURRENTPERIOD_REG_LD_3_AE())
				.ADVANCE_PREVPERIOD_REG_LD_3_AF(entryDTO_in.getADVANCE_PREVPERIOD_REG_LD_3_AF())
				.build();
	}

	public EntryID getEntryID(Long scenario_id, Long leasingDeposit_id, Long period_id, String date)
	{
		return EntryID.builder()
				.leasingDeposit_id(leasingDeposit_id)
				.scenario(scenarioService.getScenario(scenario_id))
				.period(periodService.getPeriod(period_id))
				.CALCULATION_TIME(ZonedDateTime.parse(date))
				.build();
	}

	public EntryDTO_out_RegLD1 Entry_to_EntryDTO_RegLD1(Entry entry)
	{
		return EntryDTO_out_RegLD1.builder()
				.scenario(entry.entryID.getScenario().getId())
				.leasingDeposit(entry.entryID.getLeasingDeposit_id())
				.period(entry.entryID.getPeriod().getId())
				.CALCULATION_TIME(formatDate(entry.entryID.getCALCULATION_TIME()))
				.percentRateForPeriodForLD(entry.getPercentRateForPeriodForLD())
				.user(entry.getUser().getUsername())
				.lastChange(DateFormat.formatDate(entry.getLastChange()))
				.end_date_at_this_period(formatDate(entry.getEnd_date_at_this_period()))
				.status(entry.getStatus())
				.Status_EntryMadeDuringOrAfterClosedPeriod(entry.getStatus_EntryMadeDuringOrAfterClosedPeriod())
				.DISCONT_AT_START_DATE_cur_REG_LD_1_K(entry.getDISCONT_AT_START_DATE_cur_REG_LD_1_K())
				.DISCONT_AT_START_DATE_RUB_REG_LD_1_L(entry.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L())
				.DISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M(entry.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M())
				.deposit_sum_not_disc_RUB_REG_LD_1_N(entry.getDeposit_sum_not_disc_RUB_REG_LD_1_N())
				.DISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P(entry.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P())
				.DISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q(entry.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q())
				.DISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(entry.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R())
				.REVAL_CORR_DISC_rub_REG_LD_1_S(entry.getREVAL_CORR_DISC_rub_REG_LD_1_S())
				.CORR_ACC_AMORT_DISC_rub_REG_LD_1_T(entry.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T())
				.CORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X(entry.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X())
				.CORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U(entry.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U())
				.CORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V(entry.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V())
				.CORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W(entry.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W())
				.build();
	}

	public EntryDTO_out_RegLD2 Entry_to_EntryDTO_RegLD2(Entry entry)
	{
		return EntryDTO_out_RegLD2.builder()
				.scenario(entry.entryID.getScenario().getId())
				.leasingDeposit(entry.entryID.getLeasingDeposit_id())
				.period(entry.entryID.getPeriod().getId())
				.CALCULATION_TIME(formatDate(entry.entryID.getCALCULATION_TIME()))
				.percentRateForPeriodForLD(entry.getPercentRateForPeriodForLD())
				.user(entry.getUser().getUsername())
				.lastChange(DateFormat.formatDate(entry.getLastChange()))
				.end_date_at_this_period(formatDate(entry.getEnd_date_at_this_period()))
				.status(entry.getStatus())
				.Status_EntryMadeDuringOrAfterClosedPeriod(entry.getStatus_EntryMadeDuringOrAfterClosedPeriod())
				.ACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H(entry.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H())
				.AMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I(entry.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I())
				.ACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(entry.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J())
				.ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(entry.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K())
				.AMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M(entry.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M())
				.ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N(entry.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N())
				.build();
	}

	public EntryDTO_out_RegLD3 Entry_to_EntryDTO_RegLD3(Entry entry)
	{
		return EntryDTO_out_RegLD3.builder()
				.scenario(entry.entryID.getScenario().getId())
				.leasingDeposit(entry.entryID.getLeasingDeposit_id())
				.period(entry.entryID.getPeriod().getId())
				.percentRateForPeriodForLD(entry.getPercentRateForPeriodForLD())
				.CALCULATION_TIME(formatDate(entry.entryID.getCALCULATION_TIME()))
				.user(entry.getUser().getUsername())
				.lastChange(DateFormat.formatDate(entry.getLastChange()))
				.end_date_at_this_period(formatDate(entry.getEnd_date_at_this_period()))
				.status(entry.getStatus())
				.Status_EntryMadeDuringOrAfterClosedPeriod(entry.getStatus_EntryMadeDuringOrAfterClosedPeriod())
				.discountedSum_at_current_end_date_cur_REG_LD_3_G(entry.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G())
				.INCOMING_LD_BODY_RUB_REG_LD_3_L(entry.getINCOMING_LD_BODY_RUB_REG_LD_3_L())
				.OUTCOMING_LD_BODY_REG_LD_3_M(entry.getOUTCOMING_LD_BODY_REG_LD_3_M())
				.REVAL_LD_BODY_PLUS_REG_LD_3_N(entry.getREVAL_LD_BODY_PLUS_REG_LD_3_N())
				.REVAL_LD_BODY_MINUS_REG_LD_3_O(entry.getREVAL_LD_BODY_MINUS_REG_LD_3_O())
				.ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R(entry.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R())
				.ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S(entry.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S())
				.REVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T(entry.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T())
				.REVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U(entry.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U())
				.SUM_PLUS_FOREX_DIFF_REG_LD_3_V(entry.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V())
				.SUM_MINUS_FOREX_DIFF_REG_LD_3_W(entry.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W())
				.DISPOSAL_BODY_RUB_REG_LD_3_X(entry.getDISPOSAL_BODY_RUB_REG_LD_3_X())
				.DISPOSAL_DISCONT_RUB_REG_LD_3_Y(entry.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y())
				.LDTERM_REG_LD_3_Z(entry.getLDTERM_REG_LD_3_Z())
				.TERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA(entry.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA())
				.TERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB(entry.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB())
				.TERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC(entry.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC())
				.TERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD(entry.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD())
				.ADVANCE_CURRENTPERIOD_REG_LD_3_AE(entry.getADVANCE_CURRENTPERIOD_REG_LD_3_AE())
				.ADVANCE_PREVPERIOD_REG_LD_3_AF(entry.getADVANCE_PREVPERIOD_REG_LD_3_AF())
				.build();
	}

	public EntryDTO_out Entry_to_EntryDTO_out(Entry entry)
	{
		return EntryDTO_out.builder()
				.scenario(entry.entryID.getScenario().getId())
				.leasingDeposit(entry.entryID.getLeasingDeposit_id())
				.period(entry.entryID.getPeriod().getId())
				.CALCULATION_TIME(entry.entryID.getCALCULATION_TIME().withZoneSameInstant(ZoneId.of("UTC")).toString())
				.percentRateForPeriodForLD(entry.getPercentRateForPeriodForLD())
				.user(entry.getUser().getUsername())
				.lastChange(DateFormat.formatDate(entry.getLastChange()))
				.end_date_at_this_period(formatDate(entry.getEnd_date_at_this_period()))
				.status(entry.getStatus())
				.Status_EntryMadeDuringOrAfterClosedPeriod(entry.getStatus_EntryMadeDuringOrAfterClosedPeriod())
				.DISCONT_AT_START_DATE_cur_REG_LD_1_K(entry.getDISCONT_AT_START_DATE_cur_REG_LD_1_K())
				.DISCONT_AT_START_DATE_RUB_REG_LD_1_L(entry.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L())
				.DISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M(entry.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M())
				.deposit_sum_not_disc_RUB_REG_LD_1_N(entry.getDeposit_sum_not_disc_RUB_REG_LD_1_N())
				.DISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P(entry.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P())
				.DISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q(entry.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q())
				.DISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(entry.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R())
				.REVAL_CORR_DISC_rub_REG_LD_1_S(entry.getREVAL_CORR_DISC_rub_REG_LD_1_S())
				.CORR_ACC_AMORT_DISC_rub_REG_LD_1_T(entry.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T())
				.CORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X(entry.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X())
				.CORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U(entry.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U())
				.CORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V(entry.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V())
				.CORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W(entry.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W())
				.ACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H(entry.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H())
				.AMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I(entry.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I())
				.ACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(entry.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J())
				.ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(entry.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K())
				.AMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M(entry.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M())
				.ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N(entry.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N())
				.discountedSum_at_current_end_date_cur_REG_LD_3_G(entry.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G())
				.INCOMING_LD_BODY_RUB_REG_LD_3_L(entry.getINCOMING_LD_BODY_RUB_REG_LD_3_L())
				.OUTCOMING_LD_BODY_REG_LD_3_M(entry.getOUTCOMING_LD_BODY_REG_LD_3_M())
				.REVAL_LD_BODY_PLUS_REG_LD_3_N(entry.getREVAL_LD_BODY_PLUS_REG_LD_3_N())
				.REVAL_LD_BODY_MINUS_REG_LD_3_O(entry.getREVAL_LD_BODY_MINUS_REG_LD_3_O())
				.ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R(entry.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R())
				.ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S(entry.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S())
				.REVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T(entry.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T())
				.REVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U(entry.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U())
				.SUM_PLUS_FOREX_DIFF_REG_LD_3_V(entry.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V())
				.SUM_MINUS_FOREX_DIFF_REG_LD_3_W(entry.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W())
				.DISPOSAL_BODY_RUB_REG_LD_3_X(entry.getDISPOSAL_BODY_RUB_REG_LD_3_X())
				.DISPOSAL_DISCONT_RUB_REG_LD_3_Y(entry.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y())
				.LDTERM_REG_LD_3_Z(entry.getLDTERM_REG_LD_3_Z())
				.TERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA(entry.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA())
				.TERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB(entry.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB())
				.TERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC(entry.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC())
				.TERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD(entry.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD())
				.ADVANCE_CURRENTPERIOD_REG_LD_3_AE(entry.getADVANCE_CURRENTPERIOD_REG_LD_3_AE())
				.ADVANCE_PREVPERIOD_REG_LD_3_AF(entry.getADVANCE_PREVPERIOD_REG_LD_3_AF())
				.build();
	}
}
