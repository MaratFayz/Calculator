package LD.dao;

import LD.config.Security.model.User.User;
import LD.config.Security.model.User.User_;
import LD.model.Entry.*;
import LD.model.Enums.EntryStatus;
import LD.model.Period.Period;
import LD.model.Period.Period_;
import LD.model.Scenario.Scenario;
import LD.model.Scenario.Scenario_;
import LD.repository.PeriodsClosedRepository;
import LD.repository.ScenarioRepository;
import LD.rest.exceptions.NotFoundException;
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
    public List<EntryDTO_out_RegLD1> getActiveEntriesForScenarioAndFirstOpenPeriodRegLd1(Long scenarioId) {
        List<EntryDTO_out_RegLD1> entries = getActiveEntriesForScenarioAndFirstOpenPeriod(scenarioId, this::formRegLd1, EntryDTO_out_RegLD1.class);

        if (entries.isEmpty()) {
            entries.add(new EntryDTO_out_RegLD1());
        }

        return entries;
    }

    @Override
    public List<EntryDTO_out_RegLD2> getActiveEntriesForScenarioAndFirstOpenPeriodRegLd2(Long scenarioId) {
        List<EntryDTO_out_RegLD2> entries = getActiveEntriesForScenarioAndFirstOpenPeriod(scenarioId, this::formRegLd2, EntryDTO_out_RegLD2.class);

        if (entries.isEmpty()) {
            entries.add(new EntryDTO_out_RegLD2());
        }

        return entries;
    }

    @Override
    public List<EntryDTO_out_RegLD3> getActiveEntriesForScenarioAndFirstOpenPeriodRegLd3(Long scenarioId) {
        List<EntryDTO_out_RegLD3> entries = getActiveEntriesForScenarioAndFirstOpenPeriod(scenarioId, this::formRegLd3, EntryDTO_out_RegLD3.class);

        if (entries.isEmpty()) {
            entries.add(new EntryDTO_out_RegLD3());
        }

        return entries;
    }

    public <R> List<R> getActiveEntriesForScenarioAndFirstOpenPeriod(Long scenarioId, Function<Root<Entry>, Selection<?>[]> fields,
                                                                     Class<R> resultClass) {
        final Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new NotFoundException("Значение сценария " + scenarioId + " отсутствует в базе данных"));

        LocalDate firstOpenPeriod = periodClosedRepository.findFirstOpenPeriodDateByScenario(scenario);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<R> cq = cb.createQuery(resultClass);
        root = cq.from(Entry.class);

        cq.select(
                cb.construct(resultClass,
                        fields.apply(root))
        );

        cq.where(cb.and(
                cb.equal(root.get(Entry_.entryID).get(EntryID_.period).get(Period_.date), firstOpenPeriod),
                cb.equal(root.get(Entry_.status), EntryStatus.ACTUAL)
        ));

        return entityManager.createQuery(cq).getResultList();
    }

    private Selection<?>[] formRegLd1(Root<Entry> root) {
        Join<EntryID, Scenario> joinEntryIdScenario = root.join(Entry_.entryID).join(EntryID_.scenario);
        Join<EntryID, Period> joinEntryIdDate = root.join(Entry_.entryID).join(EntryID_.period);
        Join<Entry, User> joinEntryUser = root.join(Entry_.user);

        return new Selection[]{
                root.get(Entry_.entryID).get(EntryID_.leasingDeposit_id).alias("leasingDeposit"),
                joinEntryIdScenario.get(Scenario_.name).alias("scenario"),
                joinEntryIdDate.get(Period_.date).as(String.class).alias("period"),
                joinEntryUser.get(User_.username).alias("user"),
                root.get(Entry_.lastChange).as(String.class),
                root.get(Entry_.entryID).get(EntryID_.CALCULATION_TIME).as(String.class),
                root.get(Entry_.end_date_at_this_period).as(String.class),
                root.get(Entry_.percentRateForPeriodForLD),
                root.get(Entry_.status),
                root.get(Entry_.Status_EntryMadeDuringOrAfterClosedPeriod),
                root.get(Entry_.DISCONT_AT_START_DATE_cur_REG_LD_1_K),
                root.get(Entry_.DISCONT_AT_START_DATE_RUB_REG_LD_1_L),
                root.get(Entry_.DISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M),
                root.get(Entry_.deposit_sum_not_disc_RUB_REG_LD_1_N),
                root.get(Entry_.DISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P),
                root.get(Entry_.DISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q),
                root.get(Entry_.DISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R),
                root.get(Entry_.REVAL_CORR_DISC_rub_REG_LD_1_S),
                root.get(Entry_.CORR_ACC_AMORT_DISC_rub_REG_LD_1_T),
                root.get(Entry_.CORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X),
                root.get(Entry_.CORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U),
                root.get(Entry_.CORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V),
                root.get(Entry_.CORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W),
        };
    }

    private Selection<?>[] formRegLd2(Root<Entry> entryRoot) {
        Join<EntryID, Scenario> joinEntryIdScenario = root.join(Entry_.entryID).join(EntryID_.scenario);
        Join<EntryID, Period> joinEntryIdDate = root.join(Entry_.entryID).join(EntryID_.period);
        Join<Entry, User> joinEntryUser = root.join(Entry_.user);

        return new Selection[]{
                root.get(Entry_.entryID).get(EntryID_.leasingDeposit_id).alias("leasingDeposit"),
                joinEntryIdScenario.get(Scenario_.name).alias("scenario"),
                joinEntryIdDate.get(Period_.date).as(String.class).alias("period"),
                joinEntryUser.get(User_.username).alias("user"),
                root.get(Entry_.lastChange).as(String.class),
                root.get(Entry_.entryID).get(EntryID_.CALCULATION_TIME).as(String.class),
                root.get(Entry_.end_date_at_this_period).as(String.class),
                root.get(Entry_.percentRateForPeriodForLD),
                root.get(Entry_.status),
                root.get(Entry_.Status_EntryMadeDuringOrAfterClosedPeriod),
                root.get(Entry_.ACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H),
                root.get(Entry_.AMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I),
                root.get(Entry_.ACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J),
                root.get(Entry_.ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K),
                root.get(Entry_.AMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M),
                root.get(Entry_.ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N)
        };
    }

    private Selection<?>[] formRegLd3(Root<Entry> entryRoot) {
        Join<EntryID, Scenario> joinEntryIdScenario = root.join(Entry_.entryID).join(EntryID_.scenario);
        Join<EntryID, Period> joinEntryIdDate = root.join(Entry_.entryID).join(EntryID_.period);
        Join<Entry, User> joinEntryUser = root.join(Entry_.user);

        return new Selection[]{
                root.get(Entry_.entryID).get(EntryID_.leasingDeposit_id).alias("leasingDeposit"),
                joinEntryIdScenario.get(Scenario_.name).alias("scenario"),
                joinEntryIdDate.get(Period_.date).as(String.class).alias("period"),
                joinEntryUser.get(User_.username).alias("user"),
                root.get(Entry_.lastChange).as(String.class),
                root.get(Entry_.entryID).get(EntryID_.CALCULATION_TIME).as(String.class),
                root.get(Entry_.end_date_at_this_period).as(String.class),
                root.get(Entry_.percentRateForPeriodForLD),
                root.get(Entry_.status),
                root.get(Entry_.Status_EntryMadeDuringOrAfterClosedPeriod),
                root.get(Entry_.discountedSum_at_current_end_date_cur_REG_LD_3_G),
                root.get(Entry_.INCOMING_LD_BODY_RUB_REG_LD_3_L),
                root.get(Entry_.OUTCOMING_LD_BODY_REG_LD_3_M),
                root.get(Entry_.REVAL_LD_BODY_PLUS_REG_LD_3_N),
                root.get(Entry_.REVAL_LD_BODY_MINUS_REG_LD_3_O),
                root.get(Entry_.ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R),
                root.get(Entry_.ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S),
                root.get(Entry_.REVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T),
                root.get(Entry_.REVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U),
                root.get(Entry_.SUM_PLUS_FOREX_DIFF_REG_LD_3_V),
                root.get(Entry_.SUM_MINUS_FOREX_DIFF_REG_LD_3_W),
                root.get(Entry_.DISPOSAL_BODY_RUB_REG_LD_3_X),
                root.get(Entry_.DISPOSAL_DISCONT_RUB_REG_LD_3_Y),
                root.get(Entry_.LDTERM_REG_LD_3_Z),
                root.get(Entry_.TERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA),
                root.get(Entry_.TERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB),
                root.get(Entry_.TERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC),
                root.get(Entry_.TERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD),
                root.get(Entry_.ADVANCE_CURRENTPERIOD_REG_LD_3_AE),
                root.get(Entry_.ADVANCE_PREVPERIOD_REG_LD_3_AF)
        };
    }
}