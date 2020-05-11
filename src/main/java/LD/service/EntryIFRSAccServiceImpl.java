package LD.service;

import LD.model.EntryIFRSAcc.EntryIFRSAcc;
import LD.model.EntryIFRSAcc.EntryIFRSAccDTO_out;
import LD.model.EntryIFRSAcc.EntryIFRSAccID;
import LD.model.EntryIFRSAcc.EntryIFRSAccTransform;
import LD.model.Enums.EntryStatus;
import LD.model.Period.Period;
import LD.model.Scenario.Scenario;
import LD.repository.EntryIFRSAccRepository;
import LD.repository.PeriodsClosedRepository;
import LD.repository.ScenarioRepository;
import LD.rest.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static LD.service.Calculators.LeasingDeposits.GeneralDataKeeper.specFirstClosedPeriod;

@Service
@Log4j2
public class EntryIFRSAccServiceImpl implements EntryIFRSAccService
{
	@Autowired
	EntryIFRSAccRepository entryIFRSAccRepository;
	@Autowired
	EntryIFRSAccTransform entryIFRSAccTransform;
	@Autowired
	ScenarioRepository scenarioRepository;
	@Autowired
	PeriodsClosedRepository periodsClosedRepository;

	@Override
	public List<EntryIFRSAccDTO_out> getAllEntriesIFRSAcc()
	{
		return entryIFRSAccRepository.findAll()
				.stream()
				.map(entryIFRSAcc -> entryIFRSAccTransform.EntryIFRSAcc_to_EntryIFRSAcc_DTO_out(entryIFRSAcc))
				.collect(Collectors.toList());
	}

	@Override
	public List<EntryIFRSAccDTO_out> getAllEntriesIFRSAcc_for2Scenarios(Long scenarioToId)
	{
		final Scenario scenario_to = scenarioRepository.findById(scenarioToId)
				.orElseThrow(() -> new NotFoundException("Значение сценария " + scenarioToId + " отсутствует в базе данных"));

		log.info("Был получен сценарий-получатель = {}", scenario_to);

		final Period firstOpenPeriodForScenarioTo =
				periodsClosedRepository.findAll(specFirstClosedPeriod(scenario_to)).get(0).getPeriodsClosedID()
						.getPeriod();

		log.info("Был получен первый открытый период для сценария-получателя = {}", firstOpenPeriodForScenarioTo);

		return entryIFRSAccRepository.findAll()
				.stream()
				.filter(eIFRS -> eIFRS.getEntryIFRSAccID().getEntry().getEntryID().getPeriod().equals(firstOpenPeriodForScenarioTo))
				.filter(eIFRS -> eIFRS.getEntryIFRSAccID().getEntry().getEntryID().getScenario().equals(scenario_to))
				.filter(eIFRS -> eIFRS.getEntryIFRSAccID().getEntry().getStatus().equals(EntryStatus.ACTUAL))
				.map(entryIFRSAccTransform::EntryIFRSAcc_to_EntryIFRSAcc_DTO_out)
				.collect(Collectors.toList());
	}

	@Override
	public EntryIFRSAcc getEntryIFRSAcc(EntryIFRSAccID id)
	{
		return entryIFRSAccRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public EntryIFRSAcc saveNewEntryIFRSAcc(EntryIFRSAcc entryIFRSAcc)
	{
		return entryIFRSAccRepository.saveAndFlush(entryIFRSAcc);
	}

	@Override
	public EntryIFRSAcc updateEntryIFRSAcc(EntryIFRSAccID id, EntryIFRSAcc entryIFRSAcc)
	{
		EntryIFRSAcc endDateToUpdate = getEntryIFRSAcc(id);

		BeanUtils.copyProperties(entryIFRSAcc, endDateToUpdate);

		entryIFRSAccRepository.saveAndFlush(endDateToUpdate);

		return endDateToUpdate;
	}

	@Override
	public boolean delete(EntryIFRSAccID id)
	{
		try
		{
			entryIFRSAccRepository.deleteById(id);
		}
		catch (Exception e)
		{
			return false;
		}

		return true;
	}
}
