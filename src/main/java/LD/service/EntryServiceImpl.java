package LD.service;

import LD.model.Entry.*;
import LD.model.EntryIFRSAcc.EntryIFRSAcc;
import LD.model.Enums.EntryStatus;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.Period.Period;
import LD.model.Scenario.Scenario;
import LD.repository.*;
import LD.rest.exceptions.NotFoundException;
import LD.service.Calculators.LeasingDeposits.EntryCalculator;
import LD.service.Calculators.LeasingDeposits.EntryIFRSAccCalculator;
import LD.service.Calculators.LeasingDeposits.GeneralDataKeeper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import static LD.service.Calculators.LeasingDeposits.GeneralDataKeeper.specFirstClosedPeriod;

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
	@Autowired
	EntryTransform entryTransform;
	@Autowired
	ScenarioRepository scenarioRepository;
	@Autowired
	PeriodsClosedRepository periodsClosedRepository;

	public EntryServiceImpl(EntryRepository entryRepository, DepositRatesRepository depositRatesRepository, EntryIFRSAccRepository entry_ifrs_acc_repository, GeneralDataKeeper GDK)
	{
		this.entryRepository = entryRepository;
		this.depositRatesRepository = depositRatesRepository;
		this.entry_ifrs_acc_repository = entry_ifrs_acc_repository;
		this.GDK = GDK;
	}

	@Override
	public void calculateEntries(Long SCENARIO_LOAD, Long SCENARIO_SAVE) throws ExecutionException, InterruptedException
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
	public List<EntryDTO_out> getAllLDEntries()
	{
		return entryRepository.findAll()
				.stream()
				.map(entry -> entryTransform.Entry_to_EntryDTO_out(entry))
				.collect(Collectors.toList());
	}

	public <R> List<R> getAllLDEntries_RegLDX(Long scenarioToId, Function<Entry, R> transformer_To_DTO_RegLD)
	{
		final Scenario scenario_to = scenarioRepository.findById(scenarioToId)
				.orElseThrow(() -> new NotFoundException("Значение сценария " + scenarioToId + " отсутствует в базе данных"));

		log.info("Был получен сценарий-получатель = {}", scenario_to);

		final Period firstOpenPeriodForScenarioTo =
				periodsClosedRepository.findAll(specFirstClosedPeriod(scenario_to)).get(0).getPeriodsClosedID()
						.getPeriod();

		log.info("Был получен первый открытый период для сценария-получателя = {}", firstOpenPeriodForScenarioTo);

		List<Entry> ActiveEntriesForAllDates = entryRepository.findByStatus(EntryStatus.ACTUAL);

		log.info("Все актуальные транзакции = {}", ActiveEntriesForAllDates);

		List<Entry> activeOnFirstEndDateScenarioTo = ActiveEntriesForAllDates.stream()
				.filter(ae -> ae.getEntryID().getPeriod().equals(firstOpenPeriodForScenarioTo))
				.collect(Collectors.toList());

		log.info("Все актуальные транзакции на первую отчетную дату сценария-получателя = {}",
				activeOnFirstEndDateScenarioTo);

		List<R> activeOnFirstEndDateScenarioTo_regldX = activeOnFirstEndDateScenarioTo.stream()
				.map(transformer_To_DTO_RegLD)
				.collect(Collectors.toList());

		log.info("Все актуальные транзакции на первую отчетную дату сценария-получателя в формате формы = {}",
				activeOnFirstEndDateScenarioTo_regldX);

		return activeOnFirstEndDateScenarioTo_regldX;
	}

	@Override
	public List<EntryDTO_out_RegLD1> getAllLDEntries_RegLD1(Long scenarioToId)
	{
		return getAllLDEntries_RegLDX(scenarioToId, entryTransform::Entry_to_EntryDTO_RegLD1);
	}

	@Override
	public List<EntryDTO_out_RegLD2> getAllLDEntries_RegLD2(Long scenarioToId)
	{
		return getAllLDEntries_RegLDX(scenarioToId, entryTransform::Entry_to_EntryDTO_RegLD2);
	}

	@Override
	public List<EntryDTO_out_RegLD3> getAllLDEntries_RegLD3(Long scenarioToId)
	{
		return getAllLDEntries_RegLDX(scenarioToId, entryTransform::Entry_to_EntryDTO_RegLD3);
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
			log.info("При попытке удаления проводки произошло исключение {}", e);
			return false;
		}

		return true;
	}
}
