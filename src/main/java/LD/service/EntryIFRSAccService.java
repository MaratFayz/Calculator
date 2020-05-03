package LD.service;

import LD.model.EntryIFRSAcc.EntryIFRSAcc;
import LD.model.EntryIFRSAcc.EntryIFRSAccID;

import java.util.List;

public interface EntryIFRSAccService
{
	List<EntryIFRSAcc> getAllEntriesIFRSAcc();

	EntryIFRSAcc getEntryIFRSAcc(EntryIFRSAccID id);

	EntryIFRSAcc saveNewEntryIFRSAcc(EntryIFRSAcc entryIFRSAcc);

	EntryIFRSAcc updateEntryIFRSAcc(EntryIFRSAccID id, EntryIFRSAcc entryIFRSAcc);

	boolean delete(EntryIFRSAccID id);
}
