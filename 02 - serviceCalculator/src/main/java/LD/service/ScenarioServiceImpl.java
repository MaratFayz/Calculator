package LD.service;

import LD.config.UserSource;
import LD.model.AbstractModelClass_;
import LD.model.Scenario.Scenario;
import LD.model.Scenario.ScenarioDTO_out;
import LD.repository.ScenarioRepository;
import LD.rest.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
public class ScenarioServiceImpl implements ScenarioService {

    @Autowired
    ScenarioRepository scenarioRepository;
    @Autowired
    UserSource userSource;

    @Override
    public List<ScenarioDTO_out> getAllScenarios() {
        List<Scenario> resultFormDB = scenarioRepository.findByisBlockedIsNull();
        List<ScenarioDTO_out> resultFormDB_out = new ArrayList<>();

        if (resultFormDB.size() == 0) {
            resultFormDB_out.add(new ScenarioDTO_out());
        } else {
            resultFormDB_out = resultFormDB.stream()
                    .map(c -> ScenarioDTO_out.Scenario_to_ScenarioDTO_out(c))
                    .collect(Collectors.toList());
        }

        return resultFormDB_out;
    }

    @Override
    public Scenario getScenario(Long id) {
        return scenarioRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    @Override
    public Scenario saveNewScenario(Scenario scenario) {
        log.info("Сценарий для сохранения = {}", scenario);

        scenario.setUserLastChanged(userSource.getAuthenticatedUser());
        scenario.setLastChange(ZonedDateTime.now());

        return scenarioRepository.save(scenario);
    }

    @Override
    public Scenario updateScenario(Long id, Scenario scenario) {
        scenario.setId(id);

        Scenario scenarioToUpdate = getScenario(id);

        BeanUtils.copyProperties(scenario, scenarioToUpdate, AbstractModelClass_.LAST_CHANGE, AbstractModelClass_.USER_LAST_CHANGED);

        scenarioRepository.saveAndFlush(scenarioToUpdate);

        return scenarioToUpdate;
    }

    @Override
    public void delete(Long id) {
        scenarioRepository.deleteById(id);
    }
}