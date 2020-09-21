package LD.service;

import LD.model.Entry.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public interface EntryService {

    void calculateEntries(LocalDate copyDate, Long SCENARIO_LOAD, Long SCENARIO_SAVE) throws ExecutionException, InterruptedException;

    List<EntryDTO_out> getAllLDEntries();

    List<EntryDTO_out_RegLD1> getAllLDEntries_RegLD1(Long scenarioToId);

    List<EntryDTO_out_RegLD2> getAllLDEntries_RegLD2(Long scenarioToId);

    List<EntryDTO_out_RegLD3> getAllLDEntries_RegLD3(Long scenarioToId);

    Entry getEntry(EntryID id);

    Entry update(EntryID id, Entry entry);

    Entry saveEntry(Entry entry);

    boolean delete(EntryID id);
}
