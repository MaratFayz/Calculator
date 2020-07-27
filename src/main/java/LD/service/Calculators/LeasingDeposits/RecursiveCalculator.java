/*
package LD.service.Calculators.LeasingDeposits;

import LD.model.Entry.Entry;
import LD.model.EntryIFRSAcc.EntryIFRSAcc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class RecursiveCalculator<T> extends RecursiveTask<T>
        implements Calculator<T> {

    private List<Calculatable<T>> calculatables;
    private int THRESHOLD;

    public <T> RecursiveCalculator(List<Calculatable<T>> calculatables, int THRESHOLD) {
        this.calculatables = calculatables;
        this.THRESHOLD = THRESHOLD;
    }

    @Override
    protected T compute() {
        return null;
    }

    @Override
    public List<T> calculate(List<Calculatable<T>> data) {
        if (this.allEntries.length > this.THRESHOLD) {
            return ForkJoinTask.invokeAll(createSubtasks())
                    .stream()
                    .map(ForkJoinTask::join)
                    .collect(ArrayList::new,
                            (al, list) -> al.addAll(list),
                            (al1, al2) -> al1.addAll(al2));
        }
        else {
            countLDENTRY_IN_IFRS_ACC(this.allEntries, GDK.getAllIFRSAccounts());
            return this.mappedResult;
        }
    }

    private List<EntryIFRSAccCalculator> createSubtasks() {
        List<EntryIFRSAccCalculator> dividedTasks = new ArrayList<>();

        dividedTasks.add(new EntryIFRSAccCalculator(
                Arrays.copyOfRange(allEntries, 0, allEntries.length / 2), GDK));
        dividedTasks.add(new EntryIFRSAccCalculator(
                Arrays.copyOfRange(allEntries, allEntries.length / 2,
                        allEntries.length), GDK));

        return dividedTasks;
    }
}
*/
