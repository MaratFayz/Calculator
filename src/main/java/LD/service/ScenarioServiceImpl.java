package LD.service;

import LD.config.Security.Repository.UserRepository;
import LD.model.Scenario.Scenario;
import LD.model.Scenario.ScenarioDTO_out;
import LD.repository.ScenarioRepository;
import LD.rest.exceptions.NotFoundException;
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
public class ScenarioServiceImpl implements ScenarioService
{
	@Autowired
	ScenarioRepository scenarioRepository;
	@Autowired
	UserRepository userRepository;

	@Override
	public List<ScenarioDTO_out> getAllScenarios()
	{
		List<Scenario> resultFormDB = scenarioRepository.findByisBlockedIsNull();
		List<ScenarioDTO_out> resultFormDB_out = new ArrayList<>();

		if(resultFormDB.size() == 0)
		{
			resultFormDB_out.add(new ScenarioDTO_out());
		}
		else
		{
			resultFormDB_out = resultFormDB.stream()
					.map(c -> ScenarioDTO_out.Scenario_to_ScenarioDTO_out(c))
					.collect(Collectors.toList());
		}

		return resultFormDB_out;
	}

	@Override
	public Scenario getScenario(Long id)
	{
		return scenarioRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public Scenario saveNewScenario(Scenario scenario)
	{
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		scenario.setUser(userRepository.findByUsername(username));

		scenario.setLastChange(ZonedDateTime.now());

		log.info("Сценарий для сохранения = {}", scenario);

		return scenarioRepository.save(scenario);
	}

	@Override
	public Scenario updateScenario(Long id, Scenario scenario)
	{
		scenario.setId(id);

		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		scenario.setUser(userRepository.findByUsername(username));

		scenario.setLastChange(ZonedDateTime.now());

		Scenario scenarioToUpdate = getScenario(id);

		BeanUtils.copyProperties(scenario, scenarioToUpdate);

		scenarioRepository.saveAndFlush(scenarioToUpdate);

		return scenarioToUpdate;
	}

	@Override
	public boolean delete(Long id)
	{
		try
		{
			scenarioRepository.deleteById(id);
		}
		catch (Exception e)
		{
			return false;
		}

		return true;
	}
}
