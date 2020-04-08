import lombok.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@Entity
@Table(name = "leasing_deposits")
@ToString(exclude = {"statuses", "transactions", "end_dates"})
@NoArgsConstructor()
public class LD implements Runnable
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private int id;

	@ManyToOne
	@JoinColumn(name = "entity_id", nullable = false)
	private ENTITY entity;

	@ManyToOne
	@JoinColumn(name = "counterpartner_id", nullable = false)
	private COUNTERPARTNER counterpartner;

	@ManyToOne
	@JoinColumn(name = "currency_id", nullable = false)
	private CURRENCY currency;

	@Column(name = "start_date", nullable = false, columnDefinition = "DATE")
	private ZonedDateTime start_date;

	@Column(name = "deposit_sum_not_disc", nullable = false)
	private BigDecimal deposit_sum_not_disc;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "scenario_id", nullable = false)
	private SCENARIO scenario;

	@Enumerated(value = EnumType.STRING)
	@Column(columnDefinition = "enum('X')")
	private STATUS_X is_created;

	@Enumerated(value = EnumType.STRING)
	@Column(columnDefinition = "enum('X')")
	private STATUS_X is_deleted;

	@OneToMany(mappedBy = "ld", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private Set<LD_ENTRY> transactions;

	@OneToMany(mappedBy = "ld", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private Set<END_DATES> end_dates;

	@Transient
	private BigDecimal deposit_sum_discounted_on_firstEndDate;
	@Transient
	private ZonedDateTime firstEndDate;
	@Transient
	private BigDecimal percentPerDay;
	@Transient
	private int LDdurationDays;
	@Transient
	private int LDdurationMonths;
	@Transient
	private ZonedDateTime LastPeriodWithTransactionUTC;
	@Transient
	private ZonedDateTime firstOpenPeriodUTC;
	@Transient
	private ZonedDateTime min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month;
	@Transient
	private TreeMap<ZonedDateTime, ZonedDateTime> TMEndDate;
	@Transient
	Comparator<ZonedDateTime> ZDTcomp = (date1, date2) -> (int) (date1.toEpochSecond() - date2.toEpochSecond());
	@Transient
	private LocalDate startDateWithlastDayOfStartingMonth;
	@Transient
	private GeneralDataKeeper GeneralDataKeeper;
	@Transient
	private CyclicBarrier cyclicBarrier;
	@Transient
	private SessionFactory sessionFactory;
	@Transient
	private SCENARIO scenarioTo;
	@Transient
	List<LD_ENTRY> CalculatedTransactions;


	public void registerGeneralData(GeneralDataKeeper GeneralDataKeeper, CyclicBarrier cyclicBarrier, SessionFactory sessionFactory, SCENARIO scenarioTo)
	{
		this.GeneralDataKeeper = GeneralDataKeeper;
		this.cyclicBarrier = cyclicBarrier;
		this.sessionFactory = sessionFactory;
		this.scenarioTo = scenarioTo;
	}

	public void calculate(ZonedDateTime firstOpenPeriod, Session session)
	{
		if(this.getIs_deleted() != STATUS_X.X)
		{
			final int numberDaysInYear = 365;

			this.TMEndDate = this.createPeriodsWithEndDatesForAllsLDLife(this.scenario, this.scenarioTo);
			this.countFirstEndData();

			this.setLDdurationDays(countLDDurationInDays(this.start_date, this.firstEndDate));
			this.setLDdurationMonths(countLDDurationInMonth(this.start_date, this.firstEndDate));

			BigDecimal LDYearPercent = this.getLDRate(session, this.scenario.getName());
			this.firstOpenPeriodUTC = firstOpenPeriod.withHour(0).withMinute(0).withSecond(0).withZoneSameLocal(ZoneId.of("UTC"));

			this.min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month = this.countActualClosingDateForTheFirstOpenPeriod(firstOpenPeriod);
			//дата не учитывает случай, если сначала не было транзакций, потом были, потом снова нет.
			//предположение: транзакции есть всегда сначала
			this.LastPeriodWithTransactionUTC = this.countLastPeriodWithTransaction(this.scenario);

			this.percentPerDay = getPercentPerDay(LDYearPercent);

			if (this.getLDdurationDays() > numberDaysInYear)
				deposit_sum_discounted_on_firstEndDate = countDiscountedValueFromStartDateToNeededDate(this.firstEndDate, this.start_date);
			else
				deposit_sum_discounted_on_firstEndDate = this.deposit_sum_not_disc;

			CalculatedTransactions = new ArrayList<>();
			CalculatedTransactions.addAll(changeStatusInLastEntries(this.scenarioTo, session, TRAN_STATUS.STORNO));
			CalculatedTransactions.addAll(countTransactionsForLD(session, this.GeneralDataKeeper.getAllPeriods(), this.scenarioTo, this.GeneralDataKeeper.getAllExRates()));
			this.countLDENTRY_IN_IFRS_ACC(session, CalculatedTransactions, this.GeneralDataKeeper.getAllIFRSAccounts());
		}
		else
		{
			changeStatusInLastEntries(scenarioTo, session, TRAN_STATUS.DELETED);
		}
	}

	private BigDecimal getPercentPerDay(BigDecimal yearPercent)
	{
		BigDecimal percentPerDay = BigDecimal.ZERO;
		final int numberDaysInYear = 365;

		percentPerDay = BigDecimal.valueOf(StrictMath.pow(yearPercent.divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE).doubleValue(), (double) 1 / (double) numberDaysInYear)).setScale(32, RoundingMode.UP).subtract(BigDecimal.ONE);
		return percentPerDay;
	}

	private void countLDENTRY_IN_IFRS_ACC(Session session, List<LD_ENTRY> entries, List<IFRS_ACCOUNT> allIFRSAccounts)
	{
		for (LD_ENTRY entry : entries)
		{
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.1=N5", false, entry.getDeposit_sum_not_disc_RUB_REG_LD_1_N());
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.1=N5", true, entry.getDeposit_sum_not_disc_RUB_REG_LD_1_N());
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.1=M5", false, entry.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M());
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.1=M5", true, entry.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M());
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.1=U5 + Reg.LD.1=V5", true, entry.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().add(entry.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U()));
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.1=U5", false, entry.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X());
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.1=V5", false, entry.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U());
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.1=W5 + Reg.LD.1=X5", true, entry.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().add(entry.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W()));
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.1=W5", false, entry.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V());
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.1=X5", false, entry.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W());
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.2=M5", false, entry.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M());
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.2=M5", true, entry.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M());
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.3=N5", false, entry.getREVAL_LD_BODY_PLUS_REG_LD_3_N());
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.3=O5", false, entry.getREVAL_LD_BODY_MINUS_REG_LD_3_O());
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.3=T5", false, entry.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T());
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.3=U5", false, entry.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U());
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.3=V5", false, entry.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V());
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.3=W5", false, entry.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W());
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.3=X5 + Reg.LD.3=Y5", false, entry.getDISPOSAL_BODY_RUB_REG_LD_3_X().add(entry.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y()));
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.3=X5", true, entry.getDISPOSAL_BODY_RUB_REG_LD_3_X());
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.3=Y5", true, entry.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y());
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.4_MA_AFL=B1", false, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.4_MA_AFL=C1", false, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.4_MA_AFL=A1", false, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "APP-1", false, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "APP-2", false, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "APP-3", false, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "APP-4", false, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "APP-5", false, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "APP-6", false, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "APP-7", true, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.3=AE5-Reg.LD.3=AF5", true, entry.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().subtract(entry.getADVANCE_PREVPERIOD_REG_LD_3_AF()));
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.3=AE5-Reg.LD.3=AF5", false, entry.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().subtract(entry.getADVANCE_PREVPERIOD_REG_LD_3_AF()));
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.3=AA5-Reg.LD.3=AC5", true, entry.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().subtract(entry.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC()));
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.3=AA5-Reg.LD.3=AC5", false, entry.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().subtract(entry.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC()));
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.3=AB5-Reg.LD.3=AD5", true, entry.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().subtract(entry.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD()));
			entry_2_IFRSAcc_and_save2DB(entry, session, allIFRSAccounts, "Reg.LD.3=AB5-Reg.LD.3=AD5", false, entry.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().subtract(entry.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD()));
		}
	}

	private void entry_2_IFRSAcc_and_save2DB(LD_ENTRY entry, Session session, List<IFRS_ACCOUNT> allIFRSAccounts, String AccMappedByFormula, boolean isInverse, BigDecimal sum)
	{
		Transaction HibernateTransaction = session.beginTransaction();

		LDENTRY_IN_IFRS_ACC ldEntryIFRSAcc = new LDENTRY_IN_IFRS_ACC();
		ldEntryIFRSAcc.setLDENTRY(entry);

		IFRS_ACCOUNT AccFlow = allIFRSAccounts.stream()
								.filter(acc -> acc.getMappingFormAndColumn().equals(AccMappedByFormula))
								.filter(acc -> acc.isInverseSum() == isInverse)
								.collect(Collectors.toList()).get(0);

		ldEntryIFRSAcc.setAccount_flow(AccFlow);

		ldEntryIFRSAcc.setSum(sum);
		if(isInverse) ldEntryIFRSAcc.setSum(ldEntryIFRSAcc.getSum().negate());

		session.save(ldEntryIFRSAcc);

		HibernateTransaction.commit();
	}

	private List<LD_ENTRY> changeStatusInLastEntries(SCENARIO scenarioWhereToChangeStatusInEntries, Session session, TRAN_STATUS newStatus)
	{
		List<LD_ENTRY> WhereToSaveStornoTransactions = new ArrayList<>();

		Stream<LD_ENTRY> stream_ActualEntries = this.transactions.stream().filter(transaction -> {
			if(transaction.getStatus().equals(TRAN_STATUS.ACTUAL))
				return true;
			else
				return false;
		});

		if(newStatus == TRAN_STATUS.STORNO)
		{
			stream_ActualEntries = stream_ActualEntries.filter(transaction ->
			{
				if (transaction.getScenario().equals(scenarioWhereToChangeStatusInEntries))
					return true;
				else
					return false;
			});

			if (scenarioWhereToChangeStatusInEntries.getStatus().equals(STORNO_SCENARIO_STATUS.ADDITION))
				stream_ActualEntries = stream_ActualEntries.filter(transaction ->
						transaction.getPeriod().getDate().toLocalDate().equals(firstOpenPeriodUTC));

		}

		stream_ActualEntries.forEach(transaction -> {
				Transaction HibernateTransaction = session.beginTransaction();

				transaction.setStatus(newStatus);
				WhereToSaveStornoTransactions.add(transaction);

				session.save(transaction);
				HibernateTransaction.commit();
			});

		return WhereToSaveStornoTransactions;
	}

	private List<LD_ENTRY> countTransactionsForLD(Session session, List<PERIOD> periods, SCENARIO scSAVE, List<EXCHANGE_RATE> allExRates)
	{
		List<LD_ENTRY> WhereToSaveNewTransactions = new ArrayList<>();

		LocalDate firstPeriodWithoutTransaction = this.LastPeriodWithTransactionUTC.toLocalDate();
		LocalDate min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month_InDays = min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month.toLocalDate();

		BigDecimal exRateAtStartDate = allExRates.stream()
				.filter(er -> er.getDate().isEqual(this.start_date))
				.filter(er -> er.getCurrency().equals(this.getCurrency()))
				.map(er -> er.getRate_at_date()).collect(Collectors.toList()).get(0);

		for (LocalDate closingdate : firstPeriodWithoutTransaction.datesUntil(min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month_InDays, Period.ofMonths(1)).collect(Collectors.toList()))
		{
			Transaction HibernateTransaction = session.beginTransaction();

			closingdate = closingdate.withDayOfMonth(closingdate.lengthOfMonth());
			ZonedDateTime finalClosingdate = ZonedDateTime.of(closingdate.getYear(), closingdate.getMonthValue(), closingdate.getDayOfMonth(), 0, 0, 0, 0, ZoneId.of("UTC"));

			LD_ENTRY t = new LD_ENTRY();

			if(finalClosingdate.isBefore(firstOpenPeriodUTC)) t.setStatus_EntryMadeDuringOrAfterClosedPeriod(TRAN_PER.AFTER_CLOSING_PERIOD);
			else t.setStatus_EntryMadeDuringOrAfterClosedPeriod(TRAN_PER.CURRENT_PERIOD);

			t.setLd(this);
			t.setEnd_date_at_this_period(this.TMEndDate.floorEntry(finalClosingdate).getValue());
			t.setCALCULATION_TIME(ZonedDateTime.now());
			t.setPeriod(periods.stream().filter(period -> period.getDate().isEqual(finalClosingdate)).collect(Collectors.toList()).get(0));
			t.setScenario(scSAVE);
			t.setStatus(TRAN_STATUS.ACTUAL);
			t.setDISCONT_AT_START_DATE_cur_REG_LD_1_K(this.deposit_sum_discounted_on_firstEndDate.subtract(this.deposit_sum_not_disc));
			t.setDISCONT_AT_START_DATE_RUB_REG_LD_1_L(t.getDISCONT_AT_START_DATE_cur_REG_LD_1_K().multiply(exRateAtStartDate));

			if(closingdate.isEqual(this.startDateWithlastDayOfStartingMonth))
			{
				t.setDeposit_sum_not_disc_RUB_REG_LD_1_N(this.deposit_sum_not_disc.multiply(exRateAtStartDate));
				t.setDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M(t.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L());
			}
			else
			{
				t.setDeposit_sum_not_disc_RUB_REG_LD_1_N(BigDecimal.ZERO);
				t.setDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M(BigDecimal.ZERO);
			}

			ZonedDateTime PrevClosingDate = finalClosingdate.minusMonths(1).withDayOfMonth(finalClosingdate.minusMonths(1).toLocalDate().lengthOfMonth());
			if(PrevClosingDate.isAfter(this.startDateWithlastDayOfStartingMonth.atStartOfDay(ZoneId.of("UTC"))) && !this.TMEndDate.floorEntry(PrevClosingDate).getValue().isEqual(t.getEnd_date_at_this_period()))
			{
				BigDecimal deposit_sum_discounted_on_End_date_at_this_period = BigDecimal.ZERO;

				if(this.getLDdurationDays() > 365)
					deposit_sum_discounted_on_End_date_at_this_period = countDiscountedValueFromStartDateToNeededDate(t.getEnd_date_at_this_period(), this.start_date);
				else
					deposit_sum_discounted_on_End_date_at_this_period = this.getDeposit_sum_not_disc();

				t.setDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P(deposit_sum_discounted_on_End_date_at_this_period.subtract(this.deposit_sum_not_disc));
				t.setDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q(t.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().multiply(exRateAtStartDate));

				if(this.getLDdurationDays() > 365)
				{
					//Поиск последнего периода с суммой в поле корректировки дисконта в рублях
					if (findLastRevaluationOfDiscount(scSAVE, finalClosingdate, WhereToSaveNewTransactions).equals(BigDecimal.ZERO))
					{
						t.setDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(t.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().subtract(t.getDISCONT_AT_START_DATE_RUB_REG_LD_1_L()));
					} else
					{
						t.setDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(t.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().subtract(findLastRevaluationOfDiscount(scSAVE, finalClosingdate, WhereToSaveNewTransactions)));
					}
				}
				else
				{
					t.setDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(BigDecimal.ZERO);
				}

				//Поиск последнего периода с суммой в поле корректировки дисконта в валюте
				BigDecimal curExOnPrevClosingDate = BigDecimal.ZERO;

				List<BigDecimal> List_curExOnPrevClosingDate = allExRates.stream()
															.filter(er -> er.getDate().toLocalDate().isEqual(PrevClosingDate.toLocalDate()))
															.filter(er -> er.getCurrency().equals(this.getCurrency()))
															.map(er -> er.getRate_at_date()).collect(Collectors.toList());

				if(List_curExOnPrevClosingDate.size() > 0) curExOnPrevClosingDate = List_curExOnPrevClosingDate.get(0);

				if(this.getLDdurationDays() > 365)
				{
					if (findLastCalculatedDiscount(scSAVE, finalClosingdate, WhereToSaveNewTransactions).equals(BigDecimal.ZERO))
					{
						t.setREVAL_CORR_DISC_rub_REG_LD_1_S(t.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().subtract(t.getDISCONT_AT_START_DATE_cur_REG_LD_1_K()).multiply(curExOnPrevClosingDate.subtract(exRateAtStartDate)));
					} else
					{
						t.setREVAL_CORR_DISC_rub_REG_LD_1_S(t.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().subtract(findLastCalculatedDiscount(scSAVE, finalClosingdate, WhereToSaveNewTransactions)).multiply(curExOnPrevClosingDate.subtract(exRateAtStartDate)));
					}
				}
				else
				{
					t.setREVAL_CORR_DISC_rub_REG_LD_1_S(BigDecimal.ZERO);
				}

				ZonedDateTime endDateAtPrevClosingDate = this.TMEndDate.floorEntry(PrevClosingDate).getValue();
				BigDecimal after_DiscSumAtStartDate = countDiscountedValueFromStartDateToNeededDate(t.getEnd_date_at_this_period(), this.start_date);
				BigDecimal after_DiscSumWithAccumAm = countDiscountedValueFromStartDateToNeededDate(t.getEnd_date_at_this_period(), PrevClosingDate);
				BigDecimal after_Discount_cur = after_DiscSumWithAccumAm.subtract(after_DiscSumAtStartDate);
				BigDecimal before_DiscSumAtStartDate = countDiscountedValueFromStartDateToNeededDate(endDateAtPrevClosingDate, this.start_date);
				BigDecimal before_DiscSumWithAccumAm = countDiscountedValueFromStartDateToNeededDate(endDateAtPrevClosingDate, PrevClosingDate);
				BigDecimal before_Discount_cur = before_DiscSumWithAccumAm.subtract(before_DiscSumAtStartDate);

				if(this.getLDdurationDays() > 365)
					t.setCORR_ACC_AMORT_DISC_rub_REG_LD_1_T(after_Discount_cur.subtract(before_Discount_cur).multiply(curExOnPrevClosingDate));
				else
					t.setCORR_ACC_AMORT_DISC_rub_REG_LD_1_T(BigDecimal.ZERO);

				if(t.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().compareTo(BigDecimal.ZERO) < 0)
				{
					t.setCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U(t.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().add(t.getREVAL_CORR_DISC_rub_REG_LD_1_S()));
					t.setCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W(BigDecimal.ZERO);

					t.setCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V(t.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T());
					t.setCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X(BigDecimal.ZERO);
				}
				else if (t.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().compareTo(BigDecimal.ZERO) > 0)
				{
					t.setCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W(t.getDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R().add(t.getREVAL_CORR_DISC_rub_REG_LD_1_S()));
					t.setCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U(BigDecimal.ZERO);

					t.setCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X(t.getCORR_ACC_AMORT_DISC_rub_REG_LD_1_T());
					t.setCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V(BigDecimal.ZERO);
				}
			}
			else
			{
				t.setREVAL_CORR_DISC_rub_REG_LD_1_S(BigDecimal.ZERO);
				t.setDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P(BigDecimal.ZERO);
				t.setDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q(BigDecimal.ZERO);
				t.setDISC_DIFF_BETW_DISCONTS_RUB_REG_LD_1_R(BigDecimal.ZERO);
				t.setDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P(BigDecimal.ZERO);
				t.setCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X(BigDecimal.ZERO);
				t.setCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V(BigDecimal.ZERO);
				t.setCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W(BigDecimal.ZERO);
				t.setCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U(BigDecimal.ZERO);
				t.setCORR_ACC_AMORT_DISC_rub_REG_LD_1_T(BigDecimal.ZERO);
			}

			//Reg.LD.2---------------------START
			BigDecimal avgExRateForPeriod = allExRates.stream()
					.filter(er -> er.getDate().withZoneSameInstant(ZoneId.of("UTC")).isEqual(finalClosingdate))
					.filter(er -> er.getCurrency().equals(this.getCurrency()))
					.map(er -> er.getAverage_rate_for_month()).collect(Collectors.toList()).get(0);

			if(this.getLDdurationDays() > 365)
			{
				if (t.getEnd_date_at_this_period().isAfter(finalClosingdate))
				{
					t.setACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(countDiscountFromStartDateToNeededDate(t.getEnd_date_at_this_period(), finalClosingdate));
					t.setACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N(countAccumDiscountInRub(t.getEnd_date_at_this_period(), finalClosingdate, allExRates));
				} else
				{
					t.setACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(countDiscountFromStartDateToNeededDate(t.getEnd_date_at_this_period(), t.getEnd_date_at_this_period()));
					t.setACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N(countAccumDiscountInRub(t.getEnd_date_at_this_period(), t.getEnd_date_at_this_period(), allExRates));
				}

				if (PrevClosingDate.isAfter(this.start_date.withZoneSameLocal(ZoneId.of("UTC"))) || PrevClosingDate.isEqual(this.start_date.withZoneSameLocal(ZoneId.of("UTC"))))
				{
					t.setACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H(countDiscountFromStartDateToNeededDate(t.getEnd_date_at_this_period(), PrevClosingDate));

					t.setACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(countAccumDiscountInRub(t.getEnd_date_at_this_period(), PrevClosingDate, allExRates));
				} else
				{
					t.setACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H(BigDecimal.ZERO);
					t.setACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(BigDecimal.ZERO);
				}
			}
			else
			{
				t.setACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J(BigDecimal.ZERO);
				t.setACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_2_N(BigDecimal.ZERO);
				t.setACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H(BigDecimal.ZERO);
				t.setACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_2_K(BigDecimal.ZERO);
			}

			t.setAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I(t.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().subtract(t.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H()));
			t.setAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M(t.getAMORT_DISCONT_CURRENT_PERIOD_cur_REG_LD_2_I().multiply(avgExRateForPeriod));
			//Reg.LD.2---------------------END

			//Reg.LD.3---------------------START
			if(!t.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().equals(BigDecimal.ZERO))
			{
				t.setDiscountedSum_at_current_end_date_cur_REG_LD_3_G(this.getDeposit_sum_not_disc().add(t.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P()));
			}
			else
			{
				if (findLastCalculatedDiscount(scSAVE, finalClosingdate, WhereToSaveNewTransactions).equals(BigDecimal.ZERO))
				{
					t.setDiscountedSum_at_current_end_date_cur_REG_LD_3_G(this.getDeposit_sum_not_disc().add(t.getDISCONT_AT_START_DATE_cur_REG_LD_1_K()));
				} else
				{
					t.setDiscountedSum_at_current_end_date_cur_REG_LD_3_G(this.getDeposit_sum_not_disc().add(findLastCalculatedDiscount(scSAVE, finalClosingdate, WhereToSaveNewTransactions)));
				}
			}

			BigDecimal ExRateOnClosingdate = allExRates.stream()
					.filter(er -> er.getDate().withZoneSameInstant(ZoneId.of("UTC")).isEqual(finalClosingdate))
					.filter(er -> er.getCurrency().equals(this.getCurrency()))
					.map(er -> er.getRate_at_date()).collect(Collectors.toList()).get(0);

			if(finalClosingdate.toLocalDate().equals(this.startDateWithlastDayOfStartingMonth))
			{
				BigDecimal ExRateOnLDStartingDate = allExRates.stream()
						.filter(er -> er.getDate().withZoneSameInstant(ZoneId.of("UTC")).isEqual(this.start_date))
						.filter(er -> er.getCurrency().equals(this.getCurrency()))
						.map(er -> er.getRate_at_date()).collect(Collectors.toList()).get(0);

				t.setINCOMING_LD_BODY_RUB_REG_LD_3_L(t.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().multiply(ExRateOnLDStartingDate));
				t.setACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R(BigDecimal.ZERO);
			}
			else
			{
				BigDecimal ExRateOnPrevClosingdate = allExRates.stream()
						.filter(er -> er.getDate().withZoneSameInstant(ZoneId.of("UTC")).isEqual(PrevClosingDate))
						.filter(er -> er.getCurrency().equals(this.getCurrency()))
						.map(er -> er.getRate_at_date()).collect(Collectors.toList()).get(0);

				t.setINCOMING_LD_BODY_RUB_REG_LD_3_L(t.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().multiply(ExRateOnPrevClosingdate));
				t.setACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R(t.getACCUM_AMORT_DISCONT_START_PERIOD_cur_REG_LD_2_H().multiply(ExRateOnPrevClosingdate));
			}

			t.setOUTCOMING_LD_BODY_REG_LD_3_M(t.getDiscountedSum_at_current_end_date_cur_REG_LD_3_G().multiply(ExRateOnClosingdate));

			if(t.getOUTCOMING_LD_BODY_REG_LD_3_M().compareTo(t.getINCOMING_LD_BODY_RUB_REG_LD_3_L()) > 0)
			{
				t.setREVAL_LD_BODY_PLUS_REG_LD_3_N(t.getOUTCOMING_LD_BODY_REG_LD_3_M().subtract(t.getINCOMING_LD_BODY_RUB_REG_LD_3_L()));
				t.setREVAL_LD_BODY_MINUS_REG_LD_3_O(BigDecimal.ZERO);
			}
			else
			{
				t.setREVAL_LD_BODY_PLUS_REG_LD_3_N(BigDecimal.ZERO);
				t.setREVAL_LD_BODY_MINUS_REG_LD_3_O(t.getOUTCOMING_LD_BODY_REG_LD_3_M().subtract(t.getINCOMING_LD_BODY_RUB_REG_LD_3_L()));
			}

			t.setACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S(t.getACCUM_AMORT_DISCONT_END_PERIOD_cur_REG_LD_2_J().multiply(ExRateOnClosingdate));

			if(t.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().subtract(t.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R()).subtract(t.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M()).compareTo(BigDecimal.ZERO) > 0)
			{
				t.setREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U(BigDecimal.ZERO);
				t.setREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T(t.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().subtract(t.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R()).subtract(t.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M()));
			}
			else
			{
				t.setREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U(t.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S().subtract(t.getACCUM_AMORT_DISCONT_START_PERIOD_RUB_REG_LD_3_R()).subtract(t.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M()));
				t.setREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T(BigDecimal.ZERO);
			}

			t.setSUM_PLUS_FOREX_DIFF_REG_LD_3_V(t.getREVAL_LD_BODY_PLUS_REG_LD_3_N().negate().subtract(t.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T()));
			t.setSUM_MINUS_FOREX_DIFF_REG_LD_3_W(t.getREVAL_LD_BODY_MINUS_REG_LD_3_O().negate().subtract(t.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U()));

			if(t.getEnd_date_at_this_period().isBefore(finalClosingdate) && t.getEnd_date_at_this_period().isAfter(finalClosingdate.withDayOfMonth(1)))
			{
				t.setDISPOSAL_BODY_RUB_REG_LD_3_X(t.getOUTCOMING_LD_BODY_REG_LD_3_M());
				t.setDISPOSAL_DISCONT_RUB_REG_LD_3_Y(t.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S());
			}
			else
			{
				t.setDISPOSAL_BODY_RUB_REG_LD_3_X(BigDecimal.ZERO);
				t.setDISPOSAL_DISCONT_RUB_REG_LD_3_Y(BigDecimal.ZERO);
			}

			if(Duration.between(finalClosingdate, t.getEnd_date_at_this_period()).toDays() / 30.417 >= 12)
			{
				t.setLDTERM_REG_LD_3_Z(TERM_ST_LT.LT);
			}
			else
			{
				t.setLDTERM_REG_LD_3_Z(TERM_ST_LT.ST);
			}

			if(t.getEnd_date_at_this_period().isAfter(finalClosingdate))
			{
				if(t.getLDTERM_REG_LD_3_Z().equals(TERM_ST_LT.ST))
				{
					t.setTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA(t.getOUTCOMING_LD_BODY_REG_LD_3_M());
					t.setTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB(t.getACCUM_AMORT_DISCONT_END_PERIOD_RUB_REG_LD_3_S());
				}
				else
				{
					t.setTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA(BigDecimal.ZERO);
					t.setTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB(BigDecimal.ZERO);
				}
			}
			else
			{
				t.setTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA(BigDecimal.ZERO);
				t.setTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB(BigDecimal.ZERO);
			}

			if(t.getLDTERM_REG_LD_3_Z().equals(TERM_ST_LT.ST))
			{
				t.setADVANCE_CURRENTPERIOD_REG_LD_3_AE(this.deposit_sum_discounted_on_firstEndDate.multiply(exRateAtStartDate));
			}
			else
			{
				t.setADVANCE_CURRENTPERIOD_REG_LD_3_AE(BigDecimal.ZERO);
			}

			if(findLastTransaction(this.scenario, PrevClosingDate, WhereToSaveNewTransactions).size() > 0)
			{
				LD_ENTRY lde = findLastTransaction(this.scenario, PrevClosingDate, WhereToSaveNewTransactions).get(0);
				t.setTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC(lde.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA());
				t.setTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD(lde.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB());
				t.setADVANCE_PREVPERIOD_REG_LD_3_AF(lde.getADVANCE_CURRENTPERIOD_REG_LD_3_AE());
			}
			else
			{
				t.setTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC(BigDecimal.ZERO);
				t.setTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD(BigDecimal.ZERO);
				t.setADVANCE_PREVPERIOD_REG_LD_3_AF(BigDecimal.ZERO);
			}

			//Reg.LD.3---------------------END

			WhereToSaveNewTransactions.add(t);
			session.save(t);

			HibernateTransaction.commit();
		}

		return WhereToSaveNewTransactions;
	}

	private BigDecimal countAccumDiscountInRub(ZonedDateTime endDate, ZonedDateTime neededDate, List<EXCHANGE_RATE> allExRates)
	{
		BigDecimal AccumDiscountInRub = BigDecimal.ZERO;

		LocalDate nextDateAfterNeededDate = neededDate.plusMonths(1).withDayOfMonth(neededDate.plusMonths(1).toLocalDate().lengthOfMonth()).toLocalDate();
		for (LocalDate closingdate : this.startDateWithlastDayOfStartingMonth.datesUntil(nextDateAfterNeededDate, Period.ofMonths(1)).collect(Collectors.toList()))
		{
			LocalDate PreviousClosingDate = closingdate.minusMonths(1).withDayOfMonth(closingdate.minusMonths(1).lengthOfMonth());
			ZonedDateTime ZDTPreviousClosingDate = PreviousClosingDate.atStartOfDay(ZoneId.of("UTC"));
			ZonedDateTime ZDTclosingdate = closingdate.atStartOfDay(ZoneId.of("UTC"));
			ZonedDateTime finalZDTclosingdate = ZDTclosingdate;

			BigDecimal avgExRateForPeriod = allExRates.stream()
					.filter(er -> er.getDate().withZoneSameInstant(ZoneId.of("UTC")).isEqual(finalZDTclosingdate))
					.filter(er -> er.getCurrency().equals(this.getCurrency()))
					.map(er -> er.getAverage_rate_for_month()).collect(Collectors.toList()).get(0);

			if(ZDTPreviousClosingDate.isBefore(this.start_date.withZoneSameLocal(ZoneId.of("UTC")))) ZDTPreviousClosingDate = start_date.withZoneSameInstant(ZoneId.of("UTC"));
			if(neededDate.isBefore(finalZDTclosingdate)) ZDTclosingdate = neededDate;

			AccumDiscountInRub = AccumDiscountInRub.add((countDiscountedValueFromStartDateToNeededDate(endDate, ZDTclosingdate).subtract(countDiscountedValueFromStartDateToNeededDate(endDate, ZDTPreviousClosingDate))).multiply(avgExRateForPeriod));
		}

		return AccumDiscountInRub;
	}

	private BigDecimal countDiscountedValueFromStartDateToNeededDate(ZonedDateTime endDate, ZonedDateTime neededDate)
	{
		BigDecimal countDiscountedValueFromStartDateToNeededDate = BigDecimal.ZERO;

		int LDdurationDays = (int) Duration.between(this.start_date, endDate).toDays();
		countDiscountedValueFromStartDateToNeededDate = this.deposit_sum_not_disc.setScale(32).divide(BigDecimal.ONE.add(percentPerDay).pow(LDdurationDays), RoundingMode.UP);

		int LDdurationFormStartToNeededDays = (int) Duration.between(this.start_date, neededDate).toDays();

		countDiscountedValueFromStartDateToNeededDate = countDiscountedValueFromStartDateToNeededDate.multiply(BigDecimal.ONE.add(percentPerDay).pow(LDdurationFormStartToNeededDays));

		return countDiscountedValueFromStartDateToNeededDate;
	}

	private BigDecimal countDiscountFromStartDateToNeededDate(ZonedDateTime endDate, ZonedDateTime neededDate)
	{
		return countDiscountedValueFromStartDateToNeededDate(endDate, neededDate).subtract(countDiscountedValueFromStartDateToNeededDate(endDate, this.start_date));
	}

	private List<LD_ENTRY> findLastTransaction(SCENARIO scenario, ZonedDateTime PrevClosingDate, List<LD_ENTRY> CalculatedTransactions)
	{
		List<LD_ENTRY> allT = CalculatedTransactions;
		allT.addAll(this.transactions);

		List<LD_ENTRY> LastTransaction = allT.stream()
				.filter(transaction -> {
					if(transaction.getStatus().equals(TRAN_STATUS.ACTUAL) &&
							transaction.getScenario().equals(scenario))
						return true;
					else
						return false;
				}).collect(ArrayList::new,
						(list, transaction) -> {
							if (transaction.getPeriod().getDate().withZoneSameInstant(ZoneId.of("UTC")).equals(PrevClosingDate))
								list.add(transaction);
						},
						(list1, list2) -> list1.addAll(list2));

		return LastTransaction;
	}

	private BigDecimal findLastCalculatedDiscount(SCENARIO scSAVE, ZonedDateTime finalClosingdate, List<LD_ENTRY> CalculatedTransactions)
	{
		BigDecimal LastCalculatedDiscount = BigDecimal.ZERO;
		List<LD_ENTRY> allT = CalculatedTransactions;
		allT.addAll(this.transactions);

		TreeMap<ZonedDateTime, BigDecimal> tmZDT_BD = allT.stream()
				.filter(transaction -> {
					if(transaction.getStatus().equals(TRAN_STATUS.ACTUAL) &&
							transaction.getScenario().equals(scSAVE))
						return true;
					else
						return false;
				}).collect(TreeMap::new,
						(tm, transaction) -> {
							if (!transaction.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P().equals(BigDecimal.ZERO))
								tm.put(transaction.getPeriod().getDate(), transaction.getDISCONT_SUM_AT_NEW_END_DATE_cur_REG_LD_1_P());
						},
						(tm1, tm2) -> tm1.putAll(tm2));

		Optional<ZonedDateTime> ZDTOfLastRevaluationBefore = tmZDT_BD.keySet().stream().filter(key -> key.isBefore(finalClosingdate)).max(this.ZDTcomp);

		if(ZDTOfLastRevaluationBefore.isPresent())
		{
			LastCalculatedDiscount = tmZDT_BD.get(ZDTOfLastRevaluationBefore.get());
		}

		return LastCalculatedDiscount;
	}

	private BigDecimal findLastRevaluationOfDiscount(SCENARIO scSAVE, ZonedDateTime finalClosingdate, List<LD_ENTRY> CalculatedTransactions)
	{
		BigDecimal LastRevaluation = BigDecimal.ZERO;
		List<LD_ENTRY> allT = CalculatedTransactions;
		allT.addAll(this.transactions);

		TreeMap<ZonedDateTime, BigDecimal> tmZDT_BD = allT.stream()
				.filter(transaction -> {
			if(transaction.getStatus().equals(TRAN_STATUS.ACTUAL) &&
					transaction.getScenario().equals(scSAVE))
				return true;
			else
				return false;
		}).collect(TreeMap::new,
				(tm, transaction) -> {
					if (!transaction.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q().equals(BigDecimal.ZERO))
						tm.put(transaction.getPeriod().getDate(), transaction.getDISC_SUM_AT_NEW_END_DATE_rub_REG_LD_1_Q());
				},
				(tm1, tm2) -> tm1.putAll(tm2));

		Optional<ZonedDateTime> ZDTOfLastRevaluationBefore = tmZDT_BD.keySet().stream().filter(key -> key.isBefore(finalClosingdate)).max(this.ZDTcomp);

		if(ZDTOfLastRevaluationBefore.isPresent())
		{
			LastRevaluation = tmZDT_BD.get(ZDTOfLastRevaluationBefore.get());
		}

		return LastRevaluation;
	}

	private ZonedDateTime countActualClosingDateForTheFirstOpenPeriod(ZonedDateTime firstOpenPeriod)
	{
		ZonedDateTime lastEndDateLDForFirstOpenPeriod = this.TMEndDate.floorEntry(firstOpenPeriod).getValue();

		ZonedDateTime min_betw_lastEndDateLD_and_firstOpenPeriod = lastEndDateLDForFirstOpenPeriod.isAfter(firstOpenPeriod) ? firstOpenPeriod : lastEndDateLDForFirstOpenPeriod;
		ZonedDateTime min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month = min_betw_lastEndDateLD_and_firstOpenPeriod.toLocalDate().plusMonths(1).atStartOfDay(ZoneId.of("UTC"));
		min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month = min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month.withDayOfMonth(min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month.toLocalDate().lengthOfMonth());

		return min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month;
	}

	private ZonedDateTime countLastPeriodWithTransaction(SCENARIO SCENARIO_FROM)
	{
		ZonedDateTime LastPeriodWithTransactionUTC = this.startDateWithlastDayOfStartingMonth.atStartOfDay(ZoneId.of("UTC"));

		for(LocalDate closingdate : this.startDateWithlastDayOfStartingMonth.datesUntil(min_betw_lastEndDateLD_and_firstOpenPeriod_Next_Month.toLocalDate(), Period.ofMonths(1)).collect(Collectors.toList()))
		{
			closingdate = closingdate.withDayOfMonth(closingdate.lengthOfMonth());

			LocalDate finalClosingdate = closingdate;
			if(this.transactions.stream().reduce(0,
													(result, transaction) -> {
														if(transaction.getScenario().equals(SCENARIO_FROM) &&
															transaction.getStatus().equals(TRAN_STATUS.ACTUAL) &&
															transaction.getPeriod().getDate().equals(finalClosingdate))
														{
															return result + 1;
														}
														else
															return result;
													},
													(res1, res2) -> res1 + res2) > 0)
			{
				LastPeriodWithTransactionUTC = ZonedDateTime.from(closingdate).withMinute(0).withHour(0).withSecond(0).withZoneSameLocal(ZoneId.of("UTC"));
				break;
			}
		}

		return LastPeriodWithTransactionUTC;
	}

	private void countFirstEndData()
	{
		this.startDateWithlastDayOfStartingMonth = this.start_date.toLocalDate().withDayOfMonth(this.start_date.toLocalDate().lengthOfMonth());

		List<ZonedDateTime> ListWithOneEndDateOnLastDayOfLDStarting = TMEndDate.keySet().stream().filter(date -> date.toLocalDate().isEqual(this.startDateWithlastDayOfStartingMonth)).collect(Collectors.toList());

		if (!(ListWithOneEndDateOnLastDayOfLDStarting.size() == 0))
			new IllegalArgumentException("There no ONE end date for " + this.startDateWithlastDayOfStartingMonth);

		this.firstEndDate = this.TMEndDate.get(ListWithOneEndDateOnLastDayOfLDStarting.get(0));
	}

	private int countLDDurationInDays(ZonedDateTime start_date, ZonedDateTime EndDate)
	{
		int LDdurationDays = (int) Duration.between(start_date, EndDate).toDays();

		return LDdurationDays;
	}

	private int countLDDurationInMonth(ZonedDateTime start_date, ZonedDateTime EndDate)
	{
		final int DAYS_IN_YEARS = 365;
		final int MONTHS_IN_YEARS = 12;

		int LDdurationInDays = countLDDurationInDays(start_date, EndDate);

		int LDdurationMonths = (int) Math.round(LDdurationInDays / ((double) DAYS_IN_YEARS/ (double) MONTHS_IN_YEARS));

		return LDdurationMonths;
	}

	private TreeMap<ZonedDateTime, ZonedDateTime> createPeriodsWithEndDatesForAllsLDLife(SCENARIO SCENARIO_LOAD, SCENARIO SCENARIO_SAVE)
	{
		TreeMap<ZonedDateTime, ZonedDateTime> TMEndDate = end_dates.stream().filter(element -> element.getScenario().equals(SCENARIO_LOAD) || element.getScenario().equals(SCENARIO_SAVE))
				.collect(TreeMap::new,
						(tm, end_date) -> {
								if(tm.containsKey(end_date.getPeriod().getDate().withZoneSameInstant(ZoneId.of("UTC"))))
								{
									List<END_DATES> led = end_dates.stream().filter(element -> element.getScenario().equals(SCENARIO_SAVE) && element.getEnd_Date().isEqual(end_date.getEnd_Date())).collect(Collectors.toList());
									if(led.size() == 1)
										tm.put(end_date.getPeriod().getDate().withZoneSameInstant(ZoneId.of("UTC")), led.get(0).getEnd_Date());
									else
										tm.put(end_date.getPeriod().getDate().withZoneSameInstant(ZoneId.of("UTC")), end_date.getEnd_Date().withZoneSameInstant(ZoneId.of("UTC")));
								}
								else
									tm.put(end_date.getPeriod().getDate().withZoneSameInstant(ZoneId.of("UTC")), end_date.getEnd_Date().withZoneSameInstant(ZoneId.of("UTC")));
						}, (tm1, tm2) -> tm1.putAll(tm2));

		return TMEndDate;
	}

	public BigDecimal getLDRate(Session session, String SCENARIO)
	{
		CriteriaBuilder cb = session.getCriteriaBuilder();

		CriteriaQuery<DEPOSIT_RATES> cqLDRate = cb.createQuery(DEPOSIT_RATES.class);
		Root<DEPOSIT_RATES> rootLDRates = cqLDRate.from(DEPOSIT_RATES.class);
		cqLDRate.select(rootLDRates)
				.where(
						cb.and(
								cb.equal(rootLDRates.get("entity"), this.getEntity()),
								cb.lessThanOrEqualTo(rootLDRates.get("START_PERIOD"), this.getStart_date()),
								cb.greaterThanOrEqualTo(rootLDRates.get("END_PERIOD"), this.getStart_date()),
								cb.equal(rootLDRates.get("currency"), this.getCurrency()),
								cb.lessThanOrEqualTo(rootLDRates.get("duration").get("MIN_MONTH"), this.getLDdurationMonths()),
								cb.greaterThanOrEqualTo(rootLDRates.get("duration").get("MAX_MONTH"), this.getLDdurationMonths()),
								cb.equal(rootLDRates.get("scenario").get("name"), SCENARIO)
						));
		Query<DEPOSIT_RATES> LDRate = session.createQuery(cqLDRate);
		List<DEPOSIT_RATES> LDYearPercent = LDRate.getResultList();

		if(!(LDYearPercent.size() == 1))
			new IllegalArgumentException("There is no ONE rate for " + "\n" +
										"entity = " + this.getEntity().getCode() + "\n" +
										"for date = " + this.getStart_date() + "\n" +
										"for currency = " + this.getCurrency().getShort_name() + "\n" +
										"for duration = " + this.getLDdurationMonths());

		return LDYearPercent.get(0).getRATE();
	}

	@Override
	public void run()
	{
		try
		{
			Session session = this.sessionFactory.openSession();

			this.calculate(GeneralDataKeeper.getFirstOpenPeriod(), session);

			session.close();
		}
		catch(Exception allException)
		{
			allException.printStackTrace();
		}
		finally
		{
			try
			{
				this.cyclicBarrier.await();
			}
			catch (InterruptedException | BrokenBarrierException e)
			{
				e.printStackTrace();
			}
		}
	}
}