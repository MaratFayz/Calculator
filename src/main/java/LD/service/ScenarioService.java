package LD.service;

import LD.model.Scenario.Scenario;
import LD.model.Scenario.ScenarioDTO_out;

import java.util.List;

public interface ScenarioService {

    List<ScenarioDTO_out> getAllScenarios();

    Scenario getScenario(Long id);

    Scenario saveNewScenario(Scenario scenario);

    Scenario updateScenario(Long id, Scenario scenario);

    void delete(Long id);
}
