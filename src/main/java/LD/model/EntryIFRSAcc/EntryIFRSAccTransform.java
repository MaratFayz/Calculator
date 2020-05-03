package LD.model.EntryIFRSAcc;

import LD.model.Entry.EntryID;
import LD.model.Entry.EntryTransform;
import LD.service.EntryService;
import LD.service.IFRSAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EntryIFRSAccTransform
{
	@Autowired
	EntryService entryService;
	@Autowired
	IFRSAccountService ifrsAccountService;
	@Autowired
	EntryTransform entryTransform;

	public EntryIFRSAcc EntryIFRSAccDTO_to_EntryIFRSAcc(EntryIFRSAccDTO entryIFRSAccDTO)
	{
		EntryIFRSAccID entryIFRSAccID = EntryIFRSAccDTO_to_EntryIFRSAccID(entryIFRSAccDTO.getLeasingDeposit(),
				entryIFRSAccDTO.getScenario(),
				entryIFRSAccDTO.getPeriod(),
				entryIFRSAccDTO.getCALCULATION_TIME(),
				entryIFRSAccDTO.getIfrsAccount());

		return EntryIFRSAcc.builder()
				.entryIFRSAccID(entryIFRSAccID)
				.sum(entryIFRSAccDTO.getSum())
				.build();
	}

	public EntryIFRSAccID EntryIFRSAccDTO_to_EntryIFRSAccID(Long leasingDeposit_id, Long scenario_id, Long period_id, String CALCULATION_TIME, Long ifrsAcc_id)
	{
		EntryID entryID = entryTransform.EntryDTO_to_EntryID(scenario_id, leasingDeposit_id, period_id, CALCULATION_TIME);

		return EntryIFRSAccID.builder()
				.entry(entryService.getEntry(entryID))
				.ifrsAccount(ifrsAccountService.getIFRSAccount(ifrsAcc_id))
				.build();
	}
}
