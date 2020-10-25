package LD.service.Calculators.LeasingDeposits;

import LD.model.EntryIFRSAcc.EntryIFRSAcc;

import java.util.List;

public interface EntryIfrsAccCalculator {

    List<EntryIFRSAcc> calculateEntryIfrsAcc();
}