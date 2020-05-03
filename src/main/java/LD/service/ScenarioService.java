package LD.service;

import LD.model.Scenario.Scenario;

import java.util.List;

public interface ScenarioService
{
	List<Scenario> getAllScenarios();

	Scenario getScenario(Long id);

	Scenario saveNewScenario(Scenario scenario);

	Scenario updateScenario(Long id, Scenario scenario);

	boolean delete(Long id);
}
