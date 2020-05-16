package LD.service;

import LD.config.DateFormat;
import LD.config.Security.Repository.UserRepository;
import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.PeriodsClosed.PeriodsClosedDTO_out;
import LD.model.PeriodsClosed.PeriodsClosedID;
import LD.model.PeriodsClosed.PeriodsClosedTransform;
import LD.model.Scenario.Scenario;
import LD.repository.PeriodsClosedRepository;
import LD.repository.ScenarioRepository;
import LD.rest.exceptions.NotFoundException;
import LD.service.Calculators.LeasingDeposits.GeneralDataKeeper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
public class PeriodsClosedServiceImpl implements PeriodsClosedService
{
	@Autowired
	PeriodsClosedRepository periodsClosedRepository;
	@Autowired
	ScenarioRepository scenarioRepository;
	@Autowired
	PeriodsClosedTransform periodsClosedTransform;
	@Autowired
	UserRepository userRepository;

	@Override
	public List<PeriodsClosedDTO_out> getAllPeriodsClosed()
	{
		List<PeriodsClosed> resultFormDB = periodsClosedRepository.findAll();
		List<PeriodsClosedDTO_out> resultFormDB_out = new ArrayList<>();

		if(resultFormDB.size() == 0)
		{
			resultFormDB_out.add(new PeriodsClosedDTO_out());
		}
		else
		{
			resultFormDB_out = resultFormDB.stream()
					.map(pc -> periodsClosedTransform.PeriodsClosed_to_PeriodsClosedDTO_out(pc))
					.collect(Collectors.toList());
		}

		return resultFormDB_out;
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
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		periodClosed.setUser(userRepository.findByUsername(username));

		periodClosed.setLastChange(ZonedDateTime.now());

		log.info("Закрытый период для сохранения = {}", periodClosed);

		return periodsClosedRepository.saveAndFlush(periodClosed);
	}

	@Override
	public PeriodsClosed updatePeriodsClosed(PeriodsClosedID id, PeriodsClosed periodClosed)
	{
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		periodClosed.setUser(userRepository.findByUsername(username));

		periodClosed.setLastChange(ZonedDateTime.now());

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
