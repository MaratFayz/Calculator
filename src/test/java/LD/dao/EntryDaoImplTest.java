package LD.dao;

import LD.Application;
import LD.model.Entry.EntryDTO_out_RegLD1;
import LD.model.Entry.EntryDTO_out_RegLD2;
import LD.model.Entry.EntryDTO_out_RegLD3;
import LD.model.Enums.EntryPeriodCreation;
import LD.model.Enums.EntryStatus;
import LD.model.Enums.LeasingDepositDuration;
import LD.model.Scenario.Scenario;
import LD.repository.EntryRepository;
import LD.repository.PeriodsClosedRepository;
import Utils.TestEntitiesKeeper;
import Utils.XmlDataLoader.LoadXmlFileForLeasingDepositsTest;
import Utils.XmlDataLoader.SaveEntitiesIntoDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@DataJpaTest
@ContextConfiguration(classes = {Application.class, EntryDaoImpl.class})
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto = create",
        "spring.flyway.baselineOnMigrate = false"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class EntryDaoImplTest {

    @Autowired
    private TestEntityManager testEntityManager;
    @Autowired
    private EntryRepository entryRepository;

    private TestEntitiesKeeper testEntitiesKeeper;

    @MockBean
    private PeriodsClosedRepository periodClosedRepository;

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/EntryDaoImplTest/entryDaoImplTest.xml")
    @SaveEntitiesIntoDatabase
    void getActiveEntriesForScenarioAndFirstOpenPeriodRegLd1_shouldReturnNRowsOfRegLd1_whenNEntries() {
        Scenario scenario = testEntitiesKeeper.getScenarios().get(0);

        System.out.println("Scenario = " + scenario);

        when(periodClosedRepository.findFirstOpenPeriodDateByScenario(any())).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());

        EntryDTO_out_RegLD1 entry_RegLd1_1 = EntryDTO_out_RegLD1.builder()
                .CORR_ACC_AMORT_DISC_rub_REG_LD_1_T(BigDecimal.ZERO.setScale(10))
                .CORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V(BigDecimal.ZERO.setScale(10))
                .CORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U(BigDecimal.ZERO.setScale(10))
                .CORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X(BigDecimal.ZERO.setScale(10))
                .CORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W(BigDecimal.ZERO.setScale(10))
                .user("User-1")
                .period("2020-09-30")
                .scenario("FACT")
                .leasingDeposit(1L)
                .deposit_sum_not_disc_RUB_REG_LD_1_N(BigDecimal.ZERO.setScale(10))
                .DISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(BigDecimal.ZERO.setScale(10))
                .DISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q(BigDecimal.ZERO.setScale(10))
                .DISCONT_AT_START_DATE_cur_REG_LD_1_K(BigDecimal.valueOf(-17333).setScale(10))
                .DISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M(BigDecimal.ZERO.setScale(10))
                .DISCONT_AT_START_DATE_RUB_REG_LD_1_L(BigDecimal.valueOf(-1204188).setScale(10))
                .DISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P(BigDecimal.ZERO.setScale(10))
                .end_date_at_this_period("2020-12-30")
                .percentRateForPeriodForLD(BigDecimal.valueOf(10).setScale(2))
                .REVAL_CORR_DISC_rub_REG_LD_1_S(BigDecimal.ZERO.setScale(10))
                .status(EntryStatus.ACTUAL)
                .Status_EntryMadeDuringOrAfterClosedPeriod(EntryPeriodCreation.CURRENT_PERIOD)
                .build();

        EntryDTO_out_RegLD1 entry_RegLd1_2 = EntryDTO_out_RegLD1.builder()
                .CORR_ACC_AMORT_DISC_rub_REG_LD_1_T(BigDecimal.ZERO.setScale(10))
                .CORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V(BigDecimal.ZERO.setScale(10))
                .CORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U(BigDecimal.ZERO.setScale(10))
                .CORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X(BigDecimal.ZERO.setScale(10))
                .CORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W(BigDecimal.ZERO.setScale(10))
                .user("User-1")
                .period("2020-09-30")
                .scenario("FACT")
                .leasingDeposit(2L)
                .deposit_sum_not_disc_RUB_REG_LD_1_N(BigDecimal.ZERO.setScale(10))
                .DISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(BigDecimal.ZERO.setScale(10))
                .DISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q(BigDecimal.ZERO.setScale(10))
                .DISCONT_AT_START_DATE_cur_REG_LD_1_K(BigDecimal.valueOf(-17333).setScale(10))
                .DISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M(BigDecimal.ZERO.setScale(10))
                .DISCONT_AT_START_DATE_RUB_REG_LD_1_L(BigDecimal.valueOf(-1204188).setScale(10))
                .DISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P(BigDecimal.ZERO.setScale(10))
                .end_date_at_this_period("2020-12-30")
                .percentRateForPeriodForLD(BigDecimal.valueOf(10).setScale(2))
                .REVAL_CORR_DISC_rub_REG_LD_1_S(BigDecimal.ZERO.setScale(10))
                .status(EntryStatus.ACTUAL)
                .Status_EntryMadeDuringOrAfterClosedPeriod(EntryPeriodCreation.CURRENT_PERIOD)
                .build();

        EntryDTO_out_RegLD1 entry_RegLd1_3 = EntryDTO_out_RegLD1.builder()
                .CORR_ACC_AMORT_DISC_rub_REG_LD_1_T(BigDecimal.ZERO.setScale(10))
                .CORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V(BigDecimal.ZERO.setScale(10))
                .CORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U(BigDecimal.ZERO.setScale(10))
                .CORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X(BigDecimal.ZERO.setScale(10))
                .CORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W(BigDecimal.ZERO.setScale(10))
                .user("User-1")
                .period("2020-09-30")
                .scenario("FACT")
                .leasingDeposit(3L)
                .deposit_sum_not_disc_RUB_REG_LD_1_N(BigDecimal.ZERO.setScale(10))
                .DISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(BigDecimal.ZERO.setScale(10))
                .DISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q(BigDecimal.ZERO.setScale(10))
                .DISCONT_AT_START_DATE_cur_REG_LD_1_K(BigDecimal.valueOf(-17333).setScale(10))
                .DISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M(BigDecimal.ZERO.setScale(10))
                .DISCONT_AT_START_DATE_RUB_REG_LD_1_L(BigDecimal.valueOf(-1204188).setScale(10))
                .DISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P(BigDecimal.ZERO.setScale(10))
                .end_date_at_this_period("2020-12-30")
                .percentRateForPeriodForLD(BigDecimal.valueOf(10).setScale(2))
                .REVAL_CORR_DISC_rub_REG_LD_1_S(BigDecimal.ZERO.setScale(10))
                .status(EntryStatus.ACTUAL)
                .Status_EntryMadeDuringOrAfterClosedPeriod(EntryPeriodCreation.CURRENT_PERIOD)
                .build();

        List<EntryDTO_out_RegLD1> entriesRegLd1 = entryRepository.
                getActiveEntriesForScenarioAndFirstOpenPeriodRegLd1(scenario.getId());

        System.out.println("Test value 1 = " + entry_RegLd1_1);
        System.out.println("Test value 2 = " + entry_RegLd1_2);
        System.out.println("Test value 3 = " + entry_RegLd1_3);

        System.out.println("\n" + "Values after query => ");
        entriesRegLd1.stream().forEach(e -> e.setLastChange(null));
        entriesRegLd1.stream().forEach(e -> e.setCALCULATION_TIME(null));
        entriesRegLd1.stream().forEach(e -> System.out.println(e));

        assertEquals(2, testEntitiesKeeper.getPeriods().size());
        assertEquals(3, entriesRegLd1.size());
        assertTrue(entriesRegLd1.contains(entry_RegLd1_1));
        assertTrue(entriesRegLd1.contains(entry_RegLd1_2));
        assertTrue(entriesRegLd1.contains(entry_RegLd1_3));
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/EntryDaoImplTest/entryDaoImplTest.xml")
    @SaveEntitiesIntoDatabase
    void getActiveEntriesForScenarioAndFirstOpenPeriodRegLd2_shouldReturnNRowsOfRegLd2_whenNEntries() {
        Scenario scenario = testEntitiesKeeper.getScenarios().get(0);

        System.out.println("Scenario = " + scenario);

        when(periodClosedRepository.findFirstOpenPeriodDateByScenario(any())).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());

        EntryDTO_out_RegLD2 entry_RegLd2_1 = EntryDTO_out_RegLD2.builder()
                .ACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H(BigDecimal.valueOf(12667).setScale(10))
                .AMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I(BigDecimal.valueOf(774).setScale(10))
                .ACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(BigDecimal.valueOf(13442).setScale(10))
                .ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(BigDecimal.valueOf(840506).setScale(10))
                .AMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M(BigDecimal.valueOf(55234).setScale(10))
                .ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N(BigDecimal.valueOf(8957412).setScale(10))
                .user("User-1")
                .period("2020-09-30")
                .scenario("FACT")
                .leasingDeposit(1L)
                .end_date_at_this_period("2020-12-30")
                .percentRateForPeriodForLD(BigDecimal.valueOf(10).setScale(2))
                .status(EntryStatus.ACTUAL)
                .Status_EntryMadeDuringOrAfterClosedPeriod(EntryPeriodCreation.CURRENT_PERIOD)
                .build();

        EntryDTO_out_RegLD2 entry_RegLd2_2 = EntryDTO_out_RegLD2.builder()
                .ACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H(BigDecimal.valueOf(12667).setScale(10))
                .AMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I(BigDecimal.valueOf(774).setScale(10))
                .ACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(BigDecimal.valueOf(13442).setScale(10))
                .ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(BigDecimal.valueOf(840506).setScale(10))
                .AMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M(BigDecimal.valueOf(55234).setScale(10))
                .ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N(BigDecimal.valueOf(8957412).setScale(10))
                .user("User-1")
                .period("2020-09-30")
                .scenario("FACT")
                .leasingDeposit(2L)
                .end_date_at_this_period("2020-12-30")
                .percentRateForPeriodForLD(BigDecimal.valueOf(10).setScale(2))
                .status(EntryStatus.ACTUAL)
                .Status_EntryMadeDuringOrAfterClosedPeriod(EntryPeriodCreation.CURRENT_PERIOD)
                .build();

        EntryDTO_out_RegLD2 entry_RegLd2_3 = EntryDTO_out_RegLD2.builder()
                .ACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H(BigDecimal.valueOf(12667).setScale(10))
                .AMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I(BigDecimal.valueOf(774).setScale(10))
                .ACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(BigDecimal.valueOf(13442).setScale(10))
                .ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(BigDecimal.valueOf(840506).setScale(10))
                .AMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M(BigDecimal.valueOf(55234).setScale(10))
                .ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N(BigDecimal.valueOf(8957412).setScale(10))
                .user("User-1")
                .period("2020-09-30")
                .scenario("FACT")
                .leasingDeposit(3L)
                .end_date_at_this_period("2020-12-30")
                .percentRateForPeriodForLD(BigDecimal.valueOf(10).setScale(2))
                .status(EntryStatus.ACTUAL)
                .Status_EntryMadeDuringOrAfterClosedPeriod(EntryPeriodCreation.CURRENT_PERIOD)
                .build();

        List<EntryDTO_out_RegLD2> entriesRegLd2 = entryRepository.getActiveEntriesForScenarioAndFirstOpenPeriodRegLd2(scenario.getId());

        System.out.println("Test value 1 = " + entry_RegLd2_1);
        System.out.println("Test value 2 = " + entry_RegLd2_2);
        System.out.println("Test value 3 = " + entry_RegLd2_3);

        System.out.println("\n" + "Values after query => ");
        entriesRegLd2.stream().forEach(e -> e.setLastChange(null));
        entriesRegLd2.stream().forEach(e -> e.setCALCULATION_TIME(null));
        entriesRegLd2.stream().forEach(e -> System.out.println(e));

        assertEquals(2, testEntitiesKeeper.getPeriods().size());
        assertEquals(3, entriesRegLd2.size());
        assertTrue(entriesRegLd2.contains(entry_RegLd2_1));
        assertTrue(entriesRegLd2.contains(entry_RegLd2_2));
        assertTrue(entriesRegLd2.contains(entry_RegLd2_3));
    }

    @Test
    @LoadXmlFileForLeasingDepositsTest(file = "src/test/resources/EntryDaoImplTest/entryDaoImplTest.xml")
    @SaveEntitiesIntoDatabase
    void getActiveEntriesForScenarioAndFirstOpenPeriodRegLd3_shouldReturnNRowsOfRegLd3_whenNEntries() {
        Scenario scenario = testEntitiesKeeper.getScenarios().get(0);

        System.out.println("Scenario = " + scenario);

        when(periodClosedRepository.findFirstOpenPeriodDateByScenario(any())).thenReturn(testEntitiesKeeper.getFirstOpenPeriodScenarioTo());

        EntryDTO_out_RegLD3 entry_RegLd3_1 = EntryDTO_out_RegLD3.builder()
                .discountedSum_at_current_end_date_cur_REG_LD_3_G(BigDecimal.valueOf(82666).setScale(10))
                .INCOMING_LD_BODY_RUB_REG_LD_3_L(BigDecimal.valueOf(5782608).setScale(10))
                .OUTCOMING_LD_BODY_REG_LD_3_M(BigDecimal.valueOf(6064666).setScale(10))
                .ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R(BigDecimal.valueOf(886115).setScale(10))
                .ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S(BigDecimal.valueOf(986182).setScale(10))
                .REVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T(BigDecimal.valueOf(44831).setScale(10))
                .SUM_PLUS_FOREX_DIFF_REG_LD_3_V(BigDecimal.valueOf(-326889).setScale(10))
                .TERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA(BigDecimal.valueOf(6064666).setScale(10))
                .TERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB(BigDecimal.valueOf(986182).setScale(10))
                .TERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC(BigDecimal.valueOf(5782608).setScale(10))
                .TERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD(BigDecimal.valueOf(886115).setScale(10))
                .ADVANCE_PREVPERIOD_REG_LD_3_AF(BigDecimal.valueOf(5742871).setScale(10))
                .ADVANCE_CURRENTPERIOD_REG_LD_3_AE(BigDecimal.valueOf(5742871).setScale(10))
                .LDTERM_REG_LD_3_Z(LeasingDepositDuration.ST)
                .REVAL_LD_BODY_PLUS_REG_LD_3_N(BigDecimal.valueOf(282057).setScale(10))
                .REVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U(BigDecimal.ZERO.setScale(10))
                .REVAL_LD_BODY_MINUS_REG_LD_3_O(BigDecimal.ZERO.setScale(10))
                .SUM_MINUS_FOREX_DIFF_REG_LD_3_W(BigDecimal.ZERO.setScale(10))
                .DISPOSAL_BODY_RUB_REG_LD_3_X(BigDecimal.ZERO.setScale(10))
                .DISPOSAL_DISCONT_RUB_REG_LD_3_Y(BigDecimal.ZERO.setScale(10))
                .user("User-1")
                .period("2020-09-30")
                .scenario("FACT")
                .leasingDeposit(1L)
                .end_date_at_this_period("2020-12-30")
                .percentRateForPeriodForLD(BigDecimal.valueOf(10).setScale(2))
                .status(EntryStatus.ACTUAL)
                .Status_EntryMadeDuringOrAfterClosedPeriod(EntryPeriodCreation.CURRENT_PERIOD)
                .build();

        EntryDTO_out_RegLD3 entry_RegLd3_2 = EntryDTO_out_RegLD3.builder()
                .discountedSum_at_current_end_date_cur_REG_LD_3_G(BigDecimal.valueOf(82666).setScale(10))
                .INCOMING_LD_BODY_RUB_REG_LD_3_L(BigDecimal.valueOf(5782608).setScale(10))
                .OUTCOMING_LD_BODY_REG_LD_3_M(BigDecimal.valueOf(6064666).setScale(10))
                .ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R(BigDecimal.valueOf(886115).setScale(10))
                .ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S(BigDecimal.valueOf(986182).setScale(10))
                .REVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T(BigDecimal.valueOf(44831).setScale(10))
                .SUM_PLUS_FOREX_DIFF_REG_LD_3_V(BigDecimal.valueOf(-326889).setScale(10))
                .TERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA(BigDecimal.valueOf(6064666).setScale(10))
                .TERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB(BigDecimal.valueOf(986182).setScale(10))
                .TERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC(BigDecimal.valueOf(5782608).setScale(10))
                .TERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD(BigDecimal.valueOf(886115).setScale(10))
                .ADVANCE_PREVPERIOD_REG_LD_3_AF(BigDecimal.valueOf(5742871).setScale(10))
                .ADVANCE_CURRENTPERIOD_REG_LD_3_AE(BigDecimal.valueOf(5742871).setScale(10))
                .LDTERM_REG_LD_3_Z(LeasingDepositDuration.ST)
                .REVAL_LD_BODY_PLUS_REG_LD_3_N(BigDecimal.valueOf(282057).setScale(10))
                .REVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U(BigDecimal.ZERO.setScale(10))
                .REVAL_LD_BODY_MINUS_REG_LD_3_O(BigDecimal.ZERO.setScale(10))
                .SUM_MINUS_FOREX_DIFF_REG_LD_3_W(BigDecimal.ZERO.setScale(10))
                .DISPOSAL_BODY_RUB_REG_LD_3_X(BigDecimal.ZERO.setScale(10))
                .DISPOSAL_DISCONT_RUB_REG_LD_3_Y(BigDecimal.ZERO.setScale(10))
                .user("User-1")
                .period("2020-09-30")
                .scenario("FACT")
                .leasingDeposit(2L)
                .end_date_at_this_period("2020-12-30")
                .percentRateForPeriodForLD(BigDecimal.valueOf(10).setScale(2))
                .status(EntryStatus.ACTUAL)
                .Status_EntryMadeDuringOrAfterClosedPeriod(EntryPeriodCreation.CURRENT_PERIOD)
                .build();

        EntryDTO_out_RegLD3 entry_RegLd3_3 = EntryDTO_out_RegLD3.builder()
                .discountedSum_at_current_end_date_cur_REG_LD_3_G(BigDecimal.valueOf(82666).setScale(10))
                .INCOMING_LD_BODY_RUB_REG_LD_3_L(BigDecimal.valueOf(5782608).setScale(10))
                .OUTCOMING_LD_BODY_REG_LD_3_M(BigDecimal.valueOf(6064666).setScale(10))
                .ACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R(BigDecimal.valueOf(886115).setScale(10))
                .ACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S(BigDecimal.valueOf(986182).setScale(10))
                .REVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T(BigDecimal.valueOf(44831).setScale(10))
                .SUM_PLUS_FOREX_DIFF_REG_LD_3_V(BigDecimal.valueOf(-326889).setScale(10))
                .TERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA(BigDecimal.valueOf(6064666).setScale(10))
                .TERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB(BigDecimal.valueOf(986182).setScale(10))
                .TERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC(BigDecimal.valueOf(5782608).setScale(10))
                .TERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD(BigDecimal.valueOf(886115).setScale(10))
                .ADVANCE_PREVPERIOD_REG_LD_3_AF(BigDecimal.valueOf(5742871).setScale(10))
                .ADVANCE_CURRENTPERIOD_REG_LD_3_AE(BigDecimal.valueOf(5742871).setScale(10))
                .LDTERM_REG_LD_3_Z(LeasingDepositDuration.ST)
                .REVAL_LD_BODY_PLUS_REG_LD_3_N(BigDecimal.valueOf(282057).setScale(10))
                .REVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U(BigDecimal.ZERO.setScale(10))
                .REVAL_LD_BODY_MINUS_REG_LD_3_O(BigDecimal.ZERO.setScale(10))
                .SUM_MINUS_FOREX_DIFF_REG_LD_3_W(BigDecimal.ZERO.setScale(10))
                .DISPOSAL_BODY_RUB_REG_LD_3_X(BigDecimal.ZERO.setScale(10))
                .DISPOSAL_DISCONT_RUB_REG_LD_3_Y(BigDecimal.ZERO.setScale(10))
                .user("User-1")
                .period("2020-09-30")
                .scenario("FACT")
                .leasingDeposit(3L)
                .end_date_at_this_period("2020-12-30")
                .percentRateForPeriodForLD(BigDecimal.valueOf(10).setScale(2))
                .status(EntryStatus.ACTUAL)
                .Status_EntryMadeDuringOrAfterClosedPeriod(EntryPeriodCreation.CURRENT_PERIOD)
                .build();

        List<EntryDTO_out_RegLD3> entriesRegLd3 = entryRepository.getActiveEntriesForScenarioAndFirstOpenPeriodRegLd3(scenario.getId());

        System.out.println("Test value 1 = " + entry_RegLd3_1);
        System.out.println("Test value 2 = " + entry_RegLd3_2);
        System.out.println("Test value 3 = " + entry_RegLd3_3);

        System.out.println("\n" + "Values after query => ");
        entriesRegLd3.stream().forEach(e -> e.setLastChange(null));
        entriesRegLd3.stream().forEach(e -> e.setCALCULATION_TIME(null));
        entriesRegLd3.stream().forEach(e -> System.out.println(e));

        assertEquals(2, testEntitiesKeeper.getPeriods().size());
        assertEquals(3, entriesRegLd3.size());
        assertTrue(entriesRegLd3.contains(entry_RegLd3_1));
        assertTrue(entriesRegLd3.contains(entry_RegLd3_2));
        assertTrue(entriesRegLd3.contains(entry_RegLd3_3));
    }
}