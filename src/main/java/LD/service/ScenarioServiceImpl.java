package LD.service;

import LD.model.Scenario.Scenario;
import LD.repository.ScenarioRepository;
import LD.rest.exceptions.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScenarioServiceImpl implements ScenarioService
{
	@Autowired
	ScenarioRepository scenarioRepository;

	@Override
	public List<Scenario> getAllScenarios()
	{
		return scenarioRepository.findAll();
	}

	@Override
	public Scenario getScenario(Long id)
	{
		return scenarioRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public Scenario saveNewScenario(Scenario scenario)
	{
		return scenarioRepository.save(scenario);
	}

	@Override
	public Scenario updateScenario(Long id, Scenario scenario)
	{
		scenario.setId(id);

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
