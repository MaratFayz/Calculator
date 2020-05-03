package LD.service;

import LD.model.Entry.Entry;
import LD.model.Entry.EntryID;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public interface EntryService
{
	public void calculateEntries(String SCENARIO_LOAD, String SCENARIO_SAVE) throws ExecutionException, InterruptedException;

	List<Entry> getAllLDEntries();

	Entry getEntry(EntryID id);

	Entry update(EntryID id, Entry entry);

	Entry saveEntry(Entry entry);

	boolean delete(EntryID id);
}
