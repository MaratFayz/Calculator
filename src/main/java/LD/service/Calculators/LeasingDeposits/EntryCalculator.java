package LD.service.Calculators.LeasingDeposits;

import LD.model.Entry.Entry;

import java.util.List;
import java.util.concurrent.Callable;

public interface EntryCalculator extends Callable<List<Entry>> {
    List<Entry> calculateEntries();
}
