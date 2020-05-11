package LD.model.EntryIFRSAcc;

import LD.model.Entry.EntryID;
import LD.model.Entry.EntryTransform;
import LD.service.EntryService;
import LD.service.IFRSAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EntryIFRSAccTransform
{
	@Autowired
	EntryService entryService;
	@Autowired
	IFRSAccountService ifrsAccountService;
	@Autowired
	EntryTransform entryTransform;

	public EntryIFRSAcc EntryIFRSAccDTO_in_to_EntryIFRSAcc(EntryIFRSAccDTO_in entryIFRSAccDTO_in)
	{
		EntryIFRSAccID entryIFRSAccID = getEntryIFRSAccID(entryIFRSAccDTO_in.getLeasingDeposit(),
				entryIFRSAccDTO_in.getScenario(),
				entryIFRSAccDTO_in.getPeriod(),
				entryIFRSAccDTO_in.getCALCULATION_TIME(),
				entryIFRSAccDTO_in.getIfrsAccount());

		return EntryIFRSAcc.builder()
				.entryIFRSAccID(entryIFRSAccID)
				.sum(entryIFRSAccDTO_in.getSum())
				.build();
	}

	public EntryIFRSAccID getEntryIFRSAccID(Long leasingDeposit_id, Long scenario_id, Long period_id, String CALCULATION_TIME, Long ifrsAcc_id)
	{
		EntryID entryID = entryTransform.getEntryID(scenario_id, leasingDeposit_id, period_id, CALCULATION_TIME);

		return EntryIFRSAccID.builder()
				.entry(entryService.getEntry(entryID))
				.ifrsAccount(ifrsAccountService.getIFRSAccount(ifrsAcc_id))
				.build();
	}

	public EntryIFRSAccDTO_out EntryIFRSAcc_to_EntryIFRSAcc_DTO_out(EntryIFRSAcc entryIFRSAcc)
	{
		return EntryIFRSAccDTO_out.builder()
				.leasingDeposit(entryIFRSAcc.getEntryIFRSAccID().getEntry().getLeasingDeposit().getId())
				.scenario(entryIFRSAcc.getEntryIFRSAccID().getEntry().getEntryID().getScenario().getId())
				.period(entryIFRSAcc.getEntryIFRSAccID().getEntry().getEntryID().getPeriod().getId())
				.CALCULATION_TIME(entryIFRSAcc.getEntryIFRSAccID().getEntry().getEntryID().getCALCULATION_TIME().toString())
				.ifrsAccount(entryIFRSAcc.getEntryIFRSAccID().getIfrsAccount().getId())
				.sum(entryIFRSAcc.getSum())
				.build();
	}
}
