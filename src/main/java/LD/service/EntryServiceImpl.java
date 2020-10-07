package LD.service;

import LD.config.Security.Repository.UserRepository;
import LD.dao.DaoKeeper;
import LD.dao.EntryDao;
import LD.model.Entry.*;
import LD.model.EntryIFRSAcc.EntryIFRSAcc;
import LD.model.Enums.EntryStatus;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.Period.Period;
import LD.model.Scenario.Scenario;
import LD.repository.*;
import LD.rest.exceptions.NotFoundException;
import LD.service.Calculators.LeasingDeposits.CalculationParametersSource;
import LD.service.Calculators.LeasingDeposits.CalculationParametersSourceImpl;
import LD.service.Calculators.LeasingDeposits.EntryCalculator;
import LD.service.Calculators.LeasingDeposits.EntryIfrsAccCalculator;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Log4j2
public class EntryServiceImpl implements EntryService {

    @Autowired
    EntryRepository entryRepository;
    @Autowired
    DepositRatesRepository depositRatesRepository;
    @Autowired
    EntryIFRSAccRepository entry_ifrs_acc_repository;
    @Autowired
    CalculationParametersSource calculationParametersSource;
    @Autowired
    LeasingDepositRepository leasingDepositRepository;
    @Autowired
    EntryTransform entryTransform;
    @Autowired
    ScenarioRepository scenarioRepository;
    @Autowired
    PeriodsClosedRepository periodsClosedRepository;
    @Autowired
    UserRepository userRepository;
    ReentrantLock reentrantLock;
    @Autowired
    private DaoKeeper daoKeeper;
    @Autowired
    private EntryDao entryDao;

    public EntryServiceImpl(EntryRepository entryRepository,
                            DepositRatesRepository depositRatesRepository,
                            EntryIFRSAccRepository entry_ifrs_acc_repository,
                            CalculationParametersSourceImpl calculationParametersSource) {
        this.entryRepository = entryRepository;
        this.depositRatesRepository = depositRatesRepository;
        this.entry_ifrs_acc_repository = entry_ifrs_acc_repository;
        this.calculationParametersSource = calculationParametersSource;
        this.reentrantLock = new ReentrantLock();
    }

    @Override
    public void calculateEntries(LocalDate copyDate, Long scenarioFrom, Long scenarioTo) throws ExecutionException, InterruptedException {
        this.reentrantLock.lock();
        try {
            log.trace("Начинается расчет проводок в calculateEntries");
            List<Entry> allEntriesOfAllLds = calculateAndSaveEntries(copyDate, scenarioFrom, scenarioTo);

            log.trace("Расчёт проводок окончен, начинается расчет проводок на МСФО счетах");
            calculateAndSaveEntriesIfrsAcc(allEntriesOfAllLds);
            log.trace("Расчёт проводок на МСФО счетах окончен");
        } catch (Exception e) {
            log.error("Произошла ошибка при расчете: {}", e);
            throw e;
        } finally {
            this.reentrantLock.unlock();
        }
    }

    private List<Entry> calculateAndSaveEntries(LocalDate copyDate, Long scenarioFrom, Long scenarioTo) throws ExecutionException, InterruptedException {
        prepareParametersForCalculation(copyDate, scenarioFrom, scenarioTo);

        Collection<LeasingDeposit> calculatingLeasingDeposits = getDepositsByScenariosFromAndTo(scenarioFrom, scenarioTo);

        List<Entry> allEntriesOfAllLds = calculateEntries(calculatingLeasingDeposits);
        entryRepository.saveAll(allEntriesOfAllLds);
        return allEntriesOfAllLds;
    }

    private void prepareParametersForCalculation(LocalDate copyDate, Long scenarioFrom, Long scenarioTo) {
        log.info("Начат расчет calculationParametersSource");
        calculationParametersSource.prepareParameters(copyDate, scenarioFrom, scenarioTo);
        log.info("Окончен расчет calculationParametersSource");
    }

    private void calculateAndSaveEntriesIfrsAcc(List<Entry> allEntriesOfAllLds) {
        EntryIfrsAccCalculator ldeIFRSAcc = new EntryIfrsAccCalculator(allEntriesOfAllLds.stream().toArray(Entry[]::new), calculationParametersSource);
        List<EntryIFRSAcc> resultEntryInIFRS = ldeIFRSAcc.compute();

        log.info("После калькулятора количество транзакций по счетам МСФО стало равно = {}", resultEntryInIFRS.size());

        entry_ifrs_acc_repository.saveAll(resultEntryInIFRS);
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

    private List<Entry> calculateEntries(Collection<LeasingDeposit> calculatingLeasingDeposits) throws ExecutionException, InterruptedException {
        List<Future<List<Entry>>> allEntriesOfAllLds = getFutureEntries(calculatingLeasingDeposits);
        List<Entry> allEntries = extractEntriesFromFuture(allEntriesOfAllLds);

        log.trace("calculateEntries рассчитал записей: {}", allEntries.size());
        return allEntries;
    }

    private List<Future<List<Entry>>> getFutureEntries(Collection<LeasingDeposit> calculatingLeasingDeposits) {
        ExecutorService threadExecutor = Executors.newFixedThreadPool(10);
        List<Future<List<Entry>>> allEntriesOfAllLds = new ArrayList<>();

        calculatingLeasingDeposits.stream().forEach(ld -> {
            EntryCalculator lec = new EntryCalculator(ld, calculationParametersSource, daoKeeper);
            Future<List<Entry>> entries = threadExecutor.submit(lec);
            allEntriesOfAllLds.add(entries);
        });

        threadExecutor.shutdown();

        return allEntriesOfAllLds;
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
        return entryDao.getActiveEntriesForScenarioAndFirstOpenPeriodRegLd1(scenarioToId);
    }

    @Override
    public List<EntryDTO_out_RegLD2> getAllLDEntries_RegLD2(Long scenarioToId) {
        return entryDao.getActiveEntriesForScenarioAndFirstOpenPeriodRegLd2(scenarioToId);
    }

    @Override
    public List<EntryDTO_out_RegLD3> getAllLDEntries_RegLD3(Long scenarioToId) {
        return entryDao.getActiveEntriesForScenarioAndFirstOpenPeriodRegLd3(scenarioToId);
    }

    @Override
    public Entry getEntry(EntryID id) {
        return entryRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    @Override
    public Entry update(EntryID id, Entry entry) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        entry.setUser(userRepository.findByUsername(username));

        entry.setLastChange(ZonedDateTime.now());

        Entry updatingEntry = getEntry(id);

        BeanUtils.copyProperties(entry, updatingEntry);

        entryRepository.saveAndFlush(updatingEntry);

        return updatingEntry;
    }

    @Override
    public Entry saveEntry(Entry entry) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        entry.setUser(userRepository.findByUsername(username));

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