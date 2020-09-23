package LD.dao;

import LD.config.Security.model.User.User;
import LD.config.Security.model.User.User_;
import LD.model.Entry.Entry;
import LD.model.Entry.EntryID;
import LD.model.Entry.EntryID_;
import LD.model.Entry.Entry_;
import LD.model.Enums.EntryStatus;
import LD.model.Period.Period;
import LD.model.Period.Period_;
import LD.model.Scenario.Scenario;
import LD.model.Scenario.Scenario_;
import LD.repository.PeriodsClosedRepository;
import LD.repository.ScenarioRepository;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

public class EntryDaoImpl implements EntryDao {

    @Autowired
    private PeriodsClosedRepository periodClosedRepository;
    @Autowired
    private ScenarioRepository scenarioRepository;
    @Autowired
    private EntityManager entityManager;
    private Root<Entry> root;
    private CriteriaBuilder cb;

    @Override
    public List<Object[]> getActiveEntriesForScenarioAndFirstOpenPeriodRegLd1(Scenario scenario) {
        return getActiveEntriesForScenarioAndFirstOpenPeriod(scenario, this::formRegLd1);
    }

    @Override
    public List<Object[]> getActiveEntriesForScenarioAndFirstOpenPeriodRegLd2(Scenario scenario) {
        return getActiveEntriesForScenarioAndFirstOpenPeriod(scenario, this::formRegLd2);
    }

    @Override
    public List<Object[]> getActiveEntriesForScenarioAndFirstOpenPeriodRegLd3(Scenario scenario) {
        return getActiveEntriesForScenarioAndFirstOpenPeriod(scenario, this::formRegLd3);
    }

    public List<Object[]> getActiveEntriesForScenarioAndFirstOpenPeriod(Scenario scenario, Function<Root<Entry>, List<Selection<?>>> multiselect) {
        LocalDate firstOpenPeriod = periodClosedRepository.findFirstOpenPeriodDateByScenario(scenario);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        root = cq.from(Entry.class);

        cq.multiselect(multiselect.apply(root));

        cq.where(cb.and(
                cb.equal(root.get(Entry_.entryID).get(EntryID_.period).get(Period_.date), firstOpenPeriod),
                cb.equal(root.get(Entry_.status), EntryStatus.ACTUAL)
        ));

        return entityManager.createQuery(cq).getResultList();
    }

    List<Selection<?>> formRegLd1(Root<Entry> root) {
        Join<EntryID, Scenario> joinEntryIdScenario = root.join(Entry_.entryID).join(EntryID_.scenario);
        Join<EntryID, Period> joinEntryIdDate = root.join(Entry_.entryID).join(EntryID_.period);
        Join<Entry, User> joinEntryUser = root.join(Entry_.user);

        return List.of(
                joinEntryIdScenario.get(Scenario_.name).alias("scenario"),
                root.get(Entry_.entryID).get(EntryID_.leasingDeposit_id).alias("№ deposit"),
                joinEntryIdDate.get(Period_.date).alias("date"),
                root.get(Entry_.entryID).get(EntryID_.CALCULATION_TIME).alias("calcTime"),
                root.get(Entry_.percentRateForPeriodForLD).alias("yearPercent"),
                root.get(Entry_.end_date_at_this_period).alias("endDate"),
                root.get(Entry_.status).alias("entryStatus"),
                root.get(Entry_.Status_EntryMadeDuringOrAfterClosedPeriod).alias("whenEntCalc"),
                joinEntryUser.get(User_.username).alias("user"),
                root.get(Entry_.lastChange).alias("lastChangetime"),
                root.get(Entry_.DISCONT_AT_START_DATE_cur_REG_LD_1_K).alias("discontAtFirst"),
                root.get(Entry_.DISCONT_AT_START_DATE_RUB_REG_LD_1_L).alias("discontAtFirstRub"),
                root.get(Entry_.DISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M).alias("discontAtFirstRub(entry)"),
                root.get(Entry_.deposit_sum_not_disc_RUB_REG_LD_1_N).alias("nominalRub"),
                root.get(Entry_.DISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P).alias("discontAtNewEndDate"),
                root.get(Entry_.DISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q).alias("discontAtNewEndDateRub"),
                root.get(Entry_.DISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R).alias("diffBetwDiscontRub"),
                root.get(Entry_.REVAL_CORR_DISC_rub_REG_LD_1_S).alias("revalNewDate"),
                root.get(Entry_.CORR_ACC_AMORT_DISC_rub_REG_LD_1_T).alias("discontAtFirstRub"),
                root.get(Entry_.CORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U).alias("discontAtFirstRub"),
                root.get(Entry_.CORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V).alias("discontAtFirstRub"),
                root.get(Entry_.CORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W).alias("discontAtFirstRub"),
                root.get(Entry_.CORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X).alias("discontAtFirstRub"));
    }

    private List<Selection<?>> formRegLd2(Root<Entry> entryRoot) {
        Join<EntryID, Scenario> joinEntryIdScenario = root.join(Entry_.entryID).join(EntryID_.scenario);
        Join<EntryID, Period> joinEntryIdDate = root.join(Entry_.entryID).join(EntryID_.period);
        Join<Entry, User> joinEntryUser = root.join(Entry_.user);

        return List.of(
                joinEntryIdScenario.get(Scenario_.name).alias("scenario"),
                root.get(Entry_.entryID).get(EntryID_.leasingDeposit_id).alias("№ deposit"),
                joinEntryIdDate.get(Period_.date).alias("date"),
                root.get(Entry_.entryID).get(EntryID_.CALCULATION_TIME).alias("calcTime"),
                root.get(Entry_.percentRateForPeriodForLD).alias("yearPercent"),
                root.get(Entry_.end_date_at_this_period).alias("endDate"),
                root.get(Entry_.status).alias("entryStatus"),
                root.get(Entry_.Status_EntryMadeDuringOrAfterClosedPeriod).alias("whenEntCalc"),
                joinEntryUser.get(User_.username).alias("user"),
                root.get(Entry_.lastChange).alias("lastChangetime"),
                root.get(Entry_.ACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H).alias("accumAmortStartPeriod"),
                root.get(Entry_.AMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I).alias("amortDiscontCurrentPeriod"),
                root.get(Entry_.ACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J).alias("accumAmortEndPeriod"),
                root.get(Entry_.ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K).alias("accumAmortStartPeriodRub"),
                root.get(Entry_.AMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M).alias("amortDiscontCurrentPeriodRub"),
                root.get(Entry_.ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N).alias("accumAmortEndPeriodRub")
        );
    }

    private List<Selection<?>> formRegLd3(Root<Entry> entryRoot) {
        Join<EntryID, Scenario> joinEntryIdScenario = root.join(Entry_.entryID).join(EntryID_.scenario);
        Join<EntryID, Period> joinEntryIdDate = root.join(Entry_.entryID).join(EntryID_.period);
        Join<Entry, User> joinEntryUser = root.join(Entry_.user);

        return List.of(
                joinEntryIdScenario.get(Scenario_.name).alias("scenario"),
                root.get(Entry_.entryID).get(EntryID_.leasingDeposit_id).alias("№ deposit"),
                joinEntryIdDate.get(Period_.date).alias("date"),
                root.get(Entry_.entryID).get(EntryID_.CALCULATION_TIME).alias("calcTime"),
                root.get(Entry_.percentRateForPeriodForLD).alias("yearPercent"),
                root.get(Entry_.end_date_at_this_period).alias("endDate"),
                root.get(Entry_.status).alias("entryStatus"),
                root.get(Entry_.Status_EntryMadeDuringOrAfterClosedPeriod).alias("whenEntCalc"),
                joinEntryUser.get(User_.username).alias("user"),
                root.get(Entry_.lastChange).alias("lastChangetime"),
                root.get(Entry_.discountedSum_at_current_end_date_cur_REG_LD_3_G).alias("discountedDeposit"),
                root.get(Entry_.INCOMING_LD_BODY_RUB_REG_LD_3_L).alias("incomingBodyRub"),
                root.get(Entry_.OUTCOMING_LD_BODY_REG_LD_3_M).alias("outcomingBodyRub"),
                root.get(Entry_.REVAL_LD_BODY_PLUS_REG_LD_3_N).alias("bodyRevaluationPositiveRub"),
                root.get(Entry_.REVAL_LD_BODY_MINUS_REG_LD_3_O).alias("bodyRevaluationNegativeRub"),
                root.get(Entry_.ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R).alias("accumAmortEndPeriodRub"),
                root.get(Entry_.ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S).alias("accumAmortEndPeriodRub"),
                root.get(Entry_.REVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T).alias("revaluationPositiveAccumDiscontRub"),
                root.get(Entry_.REVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U).alias("revaluationNegativeAccumDiscontRub"),
                root.get(Entry_.SUM_PLUS_FOREX_DIFF_REG_LD_3_V).alias("totalPositiveForexRub"),
                root.get(Entry_.SUM_MINUS_FOREX_DIFF_REG_LD_3_W).alias("totalNegativeForexRub"),
                root.get(Entry_.DISPOSAL_BODY_RUB_REG_LD_3_X).alias("bodyDisposalRub"),
                root.get(Entry_.DISPOSAL_DISCONT_RUB_REG_LD_3_Y).alias("discontDisposalRub"),
                root.get(Entry_.LDTERM_REG_LD_3_Z).alias("depositDuration"),
                root.get(Entry_.TERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA).alias("termReclassBodyCurrentPeriodRub"),
                root.get(Entry_.TERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB).alias("termReclassPercentCurrentPeriodRub"),
                root.get(Entry_.TERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC).alias("termReclassBodyPreviousPeriodRub"),
                root.get(Entry_.TERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD).alias("termReclassPercentPreviousPeriodRub"),
                root.get(Entry_.ADVANCE_CURRENTPERIOD_REG_LD_3_AE).alias("advanceCurrentPeriodRub"),
                root.get(Entry_.ADVANCE_PREVPERIOD_REG_LD_3_AF).alias("advancePreviousPeriodRub")
        );
    }
}
