package LD.service;

import LD.model.Entry.Entry;
import LD.model.Entry.EntryID;
import LD.model.EntryIFRSAcc.EntryIFRSAcc;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.repository.DepositRatesRepository;
import LD.repository.EntryIFRSAccRepository;
import LD.repository.EntryRepository;
import LD.repository.LeasingDepositRepository;
import LD.rest.exceptions.NotFoundException;
import LD.service.Calculators.LeasingDeposits.EntryCalculator;
import LD.service.Calculators.LeasingDeposits.EntryIFRSAccCalculator;
import LD.service.Calculators.LeasingDeposits.GeneralDataKeeper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
@Log4j2
public class EntryServiceImpl implements EntryService
{
	@Autowired
	EntryRepository entryRepository;
	@Autowired
	DepositRatesRepository depositRatesRepository;
	@Autowired
	EntryIFRSAccRepository entry_ifrs_acc_repository;
	@Autowired
	GeneralDataKeeper GDK;
	@Autowired
	LeasingDepositRepository leasingDepositRepository;

	public EntryServiceImpl(EntryRepository entryRepository, DepositRatesRepository depositRatesRepository, EntryIFRSAccRepository entry_ifrs_acc_repository, GeneralDataKeeper GDK)
	{
		this.entryRepository = entryRepository;
		this.depositRatesRepository = depositRatesRepository;
		this.entry_ifrs_acc_repository = entry_ifrs_acc_repository;
		this.GDK = GDK;
	}

	@Override
	public void calculateEntries(String SCENARIO_LOAD, String SCENARIO_SAVE) throws ExecutionException, InterruptedException
	{
		ExecutorService threadExecutor = Executors.newFixedThreadPool(10);

		List<Future<List<Entry>>> allEntriesInAllLD = new ArrayList<>();
		List<Entry> allEntries = new ArrayList<>();

		log.info("Начат расчет GDK");

		GDK.getDataFromDB(SCENARIO_LOAD, SCENARIO_SAVE);

		log.info("Значения в this.GDK = {}", this.GDK);

		//получить список депозитов
		List<LeasingDeposit> calculatingLeasingDeposits = GDK.getLeasingDeposits();

		//рассчитать транзакции по всем депозитам
		calculatingLeasingDeposits.stream().forEach(ld -> {
			EntryCalculator lec = new EntryCalculator(ld, GDK, depositRatesRepository);

			Future<List<Entry>> entries = threadExecutor.submit(lec);

			allEntriesInAllLD.add(entries);
		});

		//сохранить рассчитанные транзакции
		for(Future<List<Entry>> flEntry : allEntriesInAllLD)
			for(Entry ldEntry : flEntry.get())
			{
				log.info("Рассчитанная транзакция по депозиту => {}", ldEntry);
				allEntries.add(ldEntry);
			}

		entryRepository.saveAll(allEntries);

		//определить суммы на счетах МСФО
		EntryIFRSAccCalculator ldeIFRSAcc = new EntryIFRSAccCalculator(allEntries.stream().toArray(Entry[]::new), GDK);
		List<EntryIFRSAcc> resultEntryInIFRS = ldeIFRSAcc.compute();

		log.info("После калькулятора транзакции по счетам МСФО стали равны = {}", resultEntryInIFRS);

		//сохранить рассчитанные транзакции на счетах МСФО
		entry_ifrs_acc_repository.saveAll(resultEntryInIFRS);

		threadExecutor.shutdown();
	}

	@Override
	public List<Entry> getAllLDEntries()
	{
		return entryRepository.findAll();
	}

	@Override
	public Entry getEntry(EntryID id)
	{
		return entryRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public Entry update(EntryID id, Entry entry)
	{
		Entry updatingEntry = getEntry(id);

		BeanUtils.copyProperties(entry, updatingEntry);

		entryRepository.saveAndFlush(updatingEntry);

		return updatingEntry;
	}

	@Override
	public Entry saveEntry(Entry entry)
	{
		return entryRepository.saveAndFlush(entry);
	}

	@Override
	public boolean delete(EntryID id)
	{
		try
		{
			entryRepository.deleteById(id);
		}
		catch (Exception e)
		{
			return false;
		}

		return true;
	}
}
