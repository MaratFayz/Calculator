package LD.service;

import LD.model.EntryIFRSAcc.EntryIFRSAcc;
import LD.model.EntryIFRSAcc.EntryIFRSAccDTO_out;
import LD.model.EntryIFRSAcc.EntryIFRSAccDTO_out_form;
import LD.model.EntryIFRSAcc.EntryIFRSAccID;

import java.util.List;

public interface EntryIFRSAccService
{
	List<EntryIFRSAccDTO_out> getAllEntriesIFRSAcc();

	List<EntryIFRSAccDTO_out_form> getAllEntriesIFRSAcc_for2Scenarios(Long scenarioToId);

	EntryIFRSAcc getEntryIFRSAcc(EntryIFRSAccID id);

	EntryIFRSAcc saveNewEntryIFRSAcc(EntryIFRSAcc entryIFRSAcc);

	EntryIFRSAcc updateEntryIFRSAcc(EntryIFRSAccID id, EntryIFRSAcc entryIFRSAcc);

	boolean delete(EntryIFRSAccID id);
}
