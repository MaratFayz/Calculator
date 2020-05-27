package LD.service;

import LD.config.DateFormat;
import LD.config.Security.Repository.UserRepository;
import LD.config.Security.model.User.User;
import LD.model.Enums.STATUS_X;
import LD.model.Period.Period;
import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.PeriodsClosed.PeriodsClosedDTO_out;
import LD.model.PeriodsClosed.PeriodsClosedID;
import LD.model.PeriodsClosed.PeriodsClosedTransform;
import LD.model.Scenario.Scenario;
import LD.repository.PeriodRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
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
	@Autowired
	PeriodRepository periodRepository;

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

	@Override
	public void autoClosePeriods(String dateBeforeToClose, long scenario_id)
	{
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		User userChanging = userRepository.findByUsername(username);

		ZonedDateTime endDateToClose = DateFormat.parsingDate(dateBeforeToClose).withDayOfMonth(1).plusMonths(1).minusDays(1);
		Scenario scenarioWhereClose = scenarioRepository.findById(scenario_id).get();

		//периоды до даты включительно будем закрывать
		TreeSet<Period> periods = new TreeSet(Comparator.comparing(Period::getDate));
		periods.addAll(periodRepository.findByDateLessThanEqual(endDateToClose));

		periods.stream().forEach(p -> {
			List<PeriodsClosed> periodsClosedForPeriod = periodsClosedRepository.findAll().stream()
					.filter(pc -> pc.getPeriodsClosedID().getPeriod().equals(p))
					.filter(pc -> pc.getPeriodsClosedID().getScenario().equals(scenarioWhereClose))
					.collect(Collectors.toList());

			if(periodsClosedForPeriod.size() > 0)
			{
				//уже существуют периоды -> надо их закрыть
				PeriodsClosed pcToUpdate = periodsClosedForPeriod.get(0);
				pcToUpdate.setISCLOSED(STATUS_X.X);
				pcToUpdate.setLastChange(ZonedDateTime.now());
				pcToUpdate.setUser(userChanging);

				periodsClosedRepository.saveAndFlush(pcToUpdate);
			}
			else
			{
				//периоды ещё не существую -> надо их создать и закрыть
				PeriodsClosedID periodsClosedID = PeriodsClosedID.builder()
						.scenario(scenarioWhereClose)
						.period(p)
						.build();

				PeriodsClosed pcToUpdate = PeriodsClosed.builder()
						.ISCLOSED(STATUS_X.X)
						.lastChange(ZonedDateTime.now())
						.periodsClosedID(periodsClosedID)
						.user(userChanging)
						.build();

				periodsClosedRepository.saveAndFlush(pcToUpdate);
			}

		});

		//периоды после даты будем открывать
		periods = new TreeSet(Comparator.comparing(Period::getDate));
		periods.addAll(periodRepository.findByDateGreaterThan(endDateToClose));

		periods.stream().forEach(p -> {
			List<PeriodsClosed> periodsClosedForPeriod = periodsClosedRepository.findAll().stream()
					.filter(pc -> pc.getPeriodsClosedID().getPeriod().equals(p))
					.filter(pc -> pc.getPeriodsClosedID().getScenario().equals(scenarioWhereClose))
					.collect(Collectors.toList());

			if(periodsClosedForPeriod.size() > 0)
			{
				//уже существуют периоды -> надо их открыть
				PeriodsClosed pcToUpdate = periodsClosedForPeriod.get(0);
				pcToUpdate.setISCLOSED(null);
				pcToUpdate.setLastChange(ZonedDateTime.now());
				pcToUpdate.setUser(userChanging);

				periodsClosedRepository.saveAndFlush(pcToUpdate);
			}
		});
	}
}
