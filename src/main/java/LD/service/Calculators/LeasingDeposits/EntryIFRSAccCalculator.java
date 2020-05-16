package LD.service.Calculators.LeasingDeposits;

import LD.model.Entry.Entry;
import LD.model.EntryIFRSAcc.EntryIFRSAcc;
import LD.model.EntryIFRSAcc.EntryIFRSAccID;
import LD.model.IFRSAccount.IFRSAccount;
import lombok.extern.log4j.Log4j2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

@Log4j2
public class EntryIFRSAccCalculator extends RecursiveTask<List<EntryIFRSAcc>>
{
	List<EntryIFRSAcc> mappedResult;
	Entry[] allEntries;
	private static final int THRESHOLD = 10;
	GeneralDataKeeper GDK;

	public EntryIFRSAccCalculator(Entry[] allEntries, GeneralDataKeeper GDK)
	{
		log.info("Создание объекта-калькулятора для МСФО счетов");
		this.mappedResult = new ArrayList<>();
		this.allEntries = allEntries;
		this.GDK = GDK;
	}

	@Override
	public List<EntryIFRSAcc> compute()
	{
		if(this.allEntries.length > this.THRESHOLD)
		{
			return ForkJoinTask.invokeAll(createSubtasks())
					.stream()
					.map(ForkJoinTask::join)
					.collect(ArrayList::new,
							(al, list) -> al.addAll(list),
							(al1, al2) -> al1.addAll(al2));
		}
		else
		{
			countLDENTRY_IN_IFRS_ACC(this.allEntries, GDK.getAllIFRSAccounts());
			return this.mappedResult;
		}
	}

	private List<EntryIFRSAccCalculator> createSubtasks()
	{
		List<EntryIFRSAccCalculator> dividedTasks = new ArrayList<>();

		dividedTasks.add(new EntryIFRSAccCalculator(
				Arrays.copyOfRange(allEntries, 0, allEntries.length / 2), GDK));
		dividedTasks.add(new EntryIFRSAccCalculator(
				Arrays.copyOfRange(allEntries, allEntries.length / 2, allEntries.length), GDK));

		return dividedTasks;
	}

	public void countLDENTRY_IN_IFRS_ACC(Entry[] entries, List<IFRSAccount> allIFRSAccounts)
	{
		for (Entry entry : entries)
		{
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.1=N5", false, entry.getDeposit_sum_not_disc_RUB_REG_LD_1_N());
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.1=N5", true, entry.getDeposit_sum_not_disc_RUB_REG_LD_1_N());
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.1=M5", false, entry.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M());
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.1=M5", true, entry.getDISCONT_AT_START_DATE_RUB_forIFRSAcc_REG_LD_1_M());
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.1=U5 + Reg.LD.1=V5", true, entry.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X().add(entry.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U()));
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.1=U5", false, entry.getCORR_NEW_DATE_LESS_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_X());
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.1=V5", false, entry.getCORR_NEW_DATE_HIGHER_DISCONT_RUB_REG_LD_1_U());
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.1=W5 + Reg.LD.1=X5", true, entry.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V().add(entry.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W()));
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.1=W5", false, entry.getCORR_NEW_DATE_HIGHER_CORR_ACC_AMORT_DISC_RUB_REG_LD_1_V());
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.1=X5", false, entry.getCORR_NEW_DATE_LESS_DISCONT_RUB_REG_LD_1_W());
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.2=M5", false, entry.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M());
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.2=M5", true, entry.getAMORT_DISCONT_CURRENT_PERIOD_RUB_REG_LD_2_M());
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.3=N5", false, entry.getREVAL_LD_BODY_PLUS_REG_LD_3_N());
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.3=O5", false, entry.getREVAL_LD_BODY_MINUS_REG_LD_3_O());
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.3=T5", false, entry.getREVAL_ACC_AMORT_PLUS_RUB_REG_LD_3_T());
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.3=U5", false, entry.getREVAL_ACC_AMORT_MINUS_RUB_REG_LD_3_U());
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.3=V5", false, entry.getSUM_PLUS_FOREX_DIFF_REG_LD_3_V());
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.3=W5", false, entry.getSUM_MINUS_FOREX_DIFF_REG_LD_3_W());
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.3=X5 + Reg.LD.3=Y5", false, entry.getDISPOSAL_BODY_RUB_REG_LD_3_X().add(entry.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y()));
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.3=X5", true, entry.getDISPOSAL_BODY_RUB_REG_LD_3_X());
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.3=Y5", true, entry.getDISPOSAL_DISCONT_RUB_REG_LD_3_Y());
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.4_MA_AFL=B1", false, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.4_MA_AFL=C1", false, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.4_MA_AFL=A1", false, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "APP-1", false, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "APP-2", false, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "APP-3", false, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "APP-4", false, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "APP-5", false, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "APP-6", false, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "APP-7", true, BigDecimal.ZERO);
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.3=AE5-Reg.LD.3=AF5", true, entry.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().subtract(entry.getADVANCE_PREVPERIOD_REG_LD_3_AF()));
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.3=AE5-Reg.LD.3=AF5", false, entry.getADVANCE_CURRENTPERIOD_REG_LD_3_AE().subtract(entry.getADVANCE_PREVPERIOD_REG_LD_3_AF()));
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.3=AA5-Reg.LD.3=AC5", true, entry.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().subtract(entry.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC()));
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.3=AA5-Reg.LD.3=AC5", false, entry.getTERMRECLASS_BODY_CURRENTPERIOD_REG_LD_3_AA().subtract(entry.getTERMRECLASS_BODY_PREVPERIOD_REG_LD_3_AC()));
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.3=AB5-Reg.LD.3=AD5", true, entry.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().subtract(entry.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD()));
			entry_2_IFRSAcc_and_save2DB(entry, allIFRSAccounts, "Reg.LD.3=AB5-Reg.LD.3=AD5", false, entry.getTERMRECLASS_PERCENT_CURRENTPERIOD_REG_LD_3_AB().subtract(entry.getTERMRECLASS_PERCENT_PREVPERIOD_REG_LD_3_AD()));
		}

	}

	private void entry_2_IFRSAcc_and_save2DB(Entry entry, List<IFRSAccount> allIFRSAccounts, String AccMappedByFormula, boolean isInverse, BigDecimal sum)
	{
		IFRSAccount ifrsAccount = allIFRSAccounts.stream()
				.filter(acc -> acc.getMappingFormAndColumn().equals(AccMappedByFormula))
				.filter(acc -> acc.isInverseSum() == isInverse)
				.collect(Collectors.toList()).get(0);

		EntryIFRSAccID entryIFRSID = EntryIFRSAccID.builder()
												.entry(entry)
												.ifrsAccount(ifrsAccount)
												.build();

		EntryIFRSAcc ldEntryIFRSAcc = new EntryIFRSAcc();
		ldEntryIFRSAcc.setEntryIFRSAccID(entryIFRSID);
		ldEntryIFRSAcc.setSum(sum);
		ldEntryIFRSAcc.setUser(entry.getUser());
		if(isInverse) ldEntryIFRSAcc.setSum(ldEntryIFRSAcc.getSum().negate());

		this.mappedResult.add(ldEntryIFRSAcc);
	}
}
