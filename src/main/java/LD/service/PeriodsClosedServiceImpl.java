package LD.service;

import LD.config.DateFormat;
import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.PeriodsClosed.PeriodsClosedDTO;
import LD.model.PeriodsClosed.PeriodsClosedID;
import LD.model.PeriodsClosed.PeriodsClosedTransform;
import LD.model.Scenario.Scenario;
import LD.repository.PeriodsClosedRepository;
import LD.repository.ScenarioRepository;
import LD.rest.exceptions.NotFoundException;
import LD.service.Calculators.LeasingDeposits.GeneralDataKeeper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PeriodsClosedServiceImpl implements PeriodsClosedService
{
	@Autowired
	PeriodsClosedRepository periodsClosedRepository;
	@Autowired
	ScenarioRepository scenarioRepository;
	@Autowired
	PeriodsClosedTransform periodsClosedTransform;

	@Override
	public List<PeriodsClosedDTO> getAllPeriodsClosed()
	{
		return periodsClosedRepository.findAll().stream()
				.map(pc -> periodsClosedTransform.PeriodsClosed_to_PeriodsClosedDTO(pc))
				.collect(Collectors.toList());
	}

	@Override
	public PeriodsClosed getPeriodsClosed(PeriodsClosedID id)
	{
		return periodsClosedRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public String getDateFirstOpenPeriodForScenario(Long scenario_id)
	{
		Scenario neededScenario = scenarioRepository.findById(scenario_id).orElseThrow(() -> new NotFoundException("Не найден открытый период для сценария с id = " + scenario_id));
		ZonedDateTime notFormattedResult = periodsClosedRepository.findAll(GeneralDataKeeper.specFirstClosedPeriod(neededScenario))
				.get(0)
				.getPeriodsClosedID()
				.getPeriod()
				.getDate();

		return DateFormat.formatDate(notFormattedResult);
	}

	@Override
	public PeriodsClosed saveNewPeriodsClosed(PeriodsClosed periodClosed)
	{
		return periodsClosedRepository.saveAndFlush(periodClosed);
	}

	@Override
	public PeriodsClosed updatePeriodsClosed(PeriodsClosedID id, PeriodsClosed periodClosed)
	{
		PeriodsClosed periodsClosedToUpdate = getPeriodsClosed(id);
		BeanUtils.copyProperties(periodClosed, periodsClosedToUpdate);
		periodsClosedRepository.saveAndFlush(periodsClosedToUpdate);

		return periodsClosedToUpdate;
	}

	@Override
	public boolean delete(PeriodsClosedID id)
	{
		try
		{
			periodsClosedRepository.deleteById(id);
		}
		catch(Exception e)
		{
			return false;
		}

		return true;
	}
}
