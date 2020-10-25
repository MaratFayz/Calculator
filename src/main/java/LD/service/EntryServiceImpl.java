package LD.service;

import LD.config.Security.Repository.UserRepository;
import LD.model.Entry.*;
import LD.model.EntryIFRSAcc.EntryIFRSAcc;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.repository.EntryIFRSAccRepository;
import LD.repository.EntryRepository;
import LD.repository.LeasingDepositRepository;
import LD.rest.exceptions.NotFoundException;
import LD.service.Calculators.LeasingDeposits.CalculationParametersSource;
import LD.service.Calculators.LeasingDeposits.EntryCalculator;
import LD.service.Calculators.LeasingDeposits.EntryIfrsAccCalculator;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@Log4j2
public class EntryServiceImpl implements EntryService {

    @Autowired
    private EntryRepository entryRepository;
    @Autowired
    private EntryIFRSAccRepository entry_ifrs_acc_repository;
    @Autowired
    private LeasingDepositRepository leasingDepositRepository;
    @Autowired
    private EntryTransform entryTransform;
    @Autowired
    private UserRepository userRepository;
    private ReentrantLock reentrantLock;

    public EntryServiceImpl(EntryRepository entryRepository,
                            EntryIFRSAccRepository entry_ifrs_acc_repository) {
        this.entryRepository = entryRepository;
        this.entry_ifrs_acc_repository = entry_ifrs_acc_repository;
        this.reentrantLock = new ReentrantLock();
    }

    @Override
    @Transactional
    public void calculateEntries(LocalDate copyDate, Long scenarioFrom, Long scenarioTo) throws ExecutionException, InterruptedException {
        this.reentrantLock.lock();
        try {
            CalculationParametersSource calculationParametersSource = prepareParametersForCalculation(copyDate, scenarioFrom, scenarioTo);

            log.trace("Начинается расчет проводок в calculateEntries");
            List<Entry> allEntriesOfAllLds = calculateAndSaveEntries(copyDate, scenarioFrom, scenarioTo, calculationParametersSource);

            log.trace("Расчёт проводок окончен, начинается расчет проводок на МСФО счетах");
            calculateAndSaveEntriesIfrsAcc(allEntriesOfAllLds, calculationParametersSource);
            log.trace("Расчёт проводок на МСФО счетах окончен");
        } catch (Exception e) {
            log.error("Произошла ошибка при расчете: {}", e);
            throw e;
        } finally {
            this.reentrantLock.unlock();
        }
    }

    private List<Entry> calculateAndSaveEntries(LocalDate copyDate, Long scenarioFrom, Long scenarioTo,
                                                CalculationParametersSource calculationParametersSource) throws ExecutionException, InterruptedException {
        Collection<LeasingDeposit> calculatingLeasingDeposits = getDepositsByScenariosFromAndTo(scenarioFrom, scenarioTo);

        List<Entry> allEntriesOfAllLds = calculateEntries(calculatingLeasingDeposits, calculationParametersSource);
        entryRepository.saveAll(allEntriesOfAllLds);
        return allEntriesOfAllLds;
    }

    private CalculationParametersSource prepareParametersForCalculation(LocalDate copyDate, Long scenarioFrom, Long scenarioTo) {
        log.info("Начат расчет calculationParametersSource");
        CalculationParametersSource calculationParametersSource = getCalculationParametersSource(copyDate, scenarioFrom, scenarioTo);
        log.info("Окончен расчет calculationParametersSource = {}", calculationParametersSource);

        return calculationParametersSource;
    }

    @Lookup
    CalculationParametersSource getCalculationParametersSource(LocalDate copyDate, Long scenarioFrom, Long scenarioTo) {
        return null;
    }

    private Collection<LeasingDeposit> getDepositsByScenariosFromAndTo(Long scenarioFrom, Long scenarioTo) {
        List<LeasingDeposit> scenarioFromDeposits = leasingDepositRepository.getDepositsByScenario(scenarioFrom);
        List<LeasingDeposit> scenarioToDeposits = leasingDepositRepository.getDepositsByScenario(scenarioTo);

        Set<LeasingDeposit> result = new HashSet<>();
        result.addAll(scenarioFromDeposits);
        result.addAll(scenarioToDeposits);

        log.info("getDepositsByScenariosFromAndTo завершил работу и выдал такие результаты: {}", result.size());
        return result;
    }

    private List<Entry> calculateEntries(Collection<LeasingDeposit> calculatingLeasingDeposits, CalculationParametersSource calculationParametersSource) throws ExecutionException, InterruptedException {
        List<Future<List<Entry>>> allEntriesOfAllLds = getFutureEntries(calculatingLeasingDeposits, calculationParametersSource);
        List<Entry> allEntries = extractEntriesFromFuture(allEntriesOfAllLds);

        log.trace("calculateEntries рассчитал записей: {}", allEntries.size());
        return allEntries;
    }

    private List<Future<List<Entry>>> getFutureEntries(Collection<LeasingDeposit> calculatingLeasingDeposits, CalculationParametersSource calculationParametersSource) {
        ExecutorService threadExecutor = Executors.newFixedThreadPool(10);
        List<Future<List<Entry>>> allEntriesOfAllLds = new ArrayList<>();

        calculatingLeasingDeposits.stream().forEach(ld -> {
            EntryCalculator lec = getEntryCalculator(ld, calculationParametersSource);
            Future<List<Entry>> entries = threadExecutor.submit(lec);
            allEntriesOfAllLds.add(entries);
        });

        threadExecutor.shutdown();

        return allEntriesOfAllLds;
    }

    @Lookup
    EntryCalculator getEntryCalculator(LeasingDeposit leasingDepositToCalculate,
                                               CalculationParametersSource calculationParametersSource) {
        return null;
    }

    private List<Entry> extractEntriesFromFuture(List<Future<List<Entry>>> futureEntries) throws InterruptedException, ExecutionException {
        List<Entry> allEntries = new ArrayList<>();
        //сохранить рассчитанные транзакции
        for (Future<List<Entry>> futureEntry : futureEntries) {
            for (Entry ldEntry : futureEntry.get()) {
                allEntries.add(ldEntry);
            }
        }

        log.info("Количество рассчитанных транзакций по депозитам => {}", allEntries.size());

        return allEntries;
    }

    private void calculateAndSaveEntriesIfrsAcc(List<Entry> allEntriesOfAllLds, CalculationParametersSource calculationParametersSource) {
        EntryIfrsAccCalculator ldeIFRSAcc = getEntryIfrsAccCalculator(allEntriesOfAllLds.stream().toArray(Entry[]::new), calculationParametersSource);
        List<EntryIFRSAcc> resultEntryInIFRS = ldeIFRSAcc.calculateEntryIfrsAcc();

        log.info("После калькулятора количество транзакций по счетам МСФО стало равно = {}", resultEntryInIFRS.size());

        entry_ifrs_acc_repository.saveAll(resultEntryInIFRS);
    }

    @Lookup
    EntryIfrsAccCalculator getEntryIfrsAccCalculator(Entry[] allEntries, CalculationParametersSource calculationParametersSource) {
        return null;
    }

    @Override
    public List<EntryDTO_out> getAllLDEntries() {
        List<Entry> resultFormDB = entryRepository.findAll();
        List<EntryDTO_out> resultFormDB_out = new ArrayList<>();

        if (resultFormDB.isEmpty()) {
            resultFormDB_out.add(new EntryDTO_out());
        } else {
            resultFormDB_out = resultFormDB.stream()
                    .map(entry -> entryTransform.Entry_to_EntryDTO_out(entry))
                    .collect(Collectors.toList());
        }

        return resultFormDB_out;
    }

    @Override
    public List<EntryDTO_out_RegLD1> getAllLDEntries_RegLD1(Long scenarioToId) {
        return entryRepository.getActiveEntriesForScenarioAndFirstOpenPeriodRegLd1(scenarioToId);
    }

    @Override
    public List<EntryDTO_out_RegLD2> getAllLDEntries_RegLD2(Long scenarioToId) {
        return entryRepository.getActiveEntriesForScenarioAndFirstOpenPeriodRegLd2(scenarioToId);
    }

    @Override
    public List<EntryDTO_out_RegLD3> getAllLDEntries_RegLD3(Long scenarioToId) {
        return entryRepository.getActiveEntriesForScenarioAndFirstOpenPeriodRegLd3(scenarioToId);
    }

    @Override
    public Entry getEntry(EntryID id) {
        return entryRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    @Override
    public Entry update(EntryID id, Entry entry) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        entry.setUserLastChanged(userRepository.findByUsername(username));

        entry.setLastChange(ZonedDateTime.now());

        Entry updatingEntry = getEntry(id);

        BeanUtils.copyProperties(entry, updatingEntry);

        entryRepository.saveAndFlush(updatingEntry);

        return updatingEntry;
    }

    @Override
    public Entry saveEntry(Entry entry) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        entry.setUserLastChanged(userRepository.findByUsername(username));

        entry.setLastChange(ZonedDateTime.now());

        log.info("Проводка для сохранения = {}", entry);

        return entryRepository.saveAndFlush(entry);
    }

    @Override
    public boolean delete(EntryID id) {
        try {
            entryRepository.deleteById(id);
        } catch (Exception e) {
            log.info("При попытке удаления проводки произошло исключение {}", e);
            return false;
        }

        return true;
    }
}