package LD.dao;

import LD.model.EntryIFRSAcc.EntryIFRSAccDTO_out_form;

import java.util.List;

public interface EntryIfrsAccDao {

    List<EntryIFRSAccDTO_out_form> sumActualEntriesIfrs(long scenarioToId);
}
