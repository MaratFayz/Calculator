package LD.rest;

import LD.model.Scenario.Scenario;
import LD.model.Scenario.ScenarioDTO_in;
import LD.model.Scenario.ScenarioDTO_out;
import LD.service.ScenarioService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/scenarios", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Контроллер для сценариев")
@Log4j2
public class ScenarioController {

    @Autowired
    ScenarioService scenarioService;

    @GetMapping
    @ApiOperation(value = "Получение всех сценариев", response = ResponseEntity.class)
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).SCENARIO_READER)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Все сценарии возвращаются в ответе."),
            @ApiResponse(code = 403, message = "Доступ запрещён")
    })
    public List<ScenarioDTO_out> getAllScenarios() {
        return scenarioService.getAllScenarios();
    }

    @GetMapping("{id}")
    @ApiOperation(value = "Получение сценария с определённым id", response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Сценарий существует, возвращается в ответе."),
            @ApiResponse(code = 403, message = "Доступ запрещён"),
            @ApiResponse(code = 404, message = "Такой сценарий отсутствует")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).SCENARIO_READER)")
    public ResponseEntity getScenario(@PathVariable Long id) {
        Scenario scenario = scenarioService.getScenario(id);
        log.info("(getScenario): scenario was taken: " + scenario);
        return new ResponseEntity(ScenarioDTO_out.Scenario_to_ScenarioDTO_out(scenario), HttpStatus.OK);
    }

    @PostMapping
    @ApiOperation(value = "Сохранение нового сценария", response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Новый сценарий был сохранен."),
            @ApiResponse(code = 403, message = "Доступ запрещён")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).SCENARIO_ADDER)")
    public ResponseEntity saveNewScenario(@RequestBody ScenarioDTO_in scenarioDTOIn) {
        Scenario scenario = ScenarioDTO_in.ScenarioDTO_to_Scenario(scenarioDTOIn);
        Scenario newScenario = scenarioService.saveNewScenario(scenario);
        return new ResponseEntity(ScenarioDTO_out.Scenario_to_ScenarioDTO_out(newScenario), HttpStatus.OK);
    }

    @PutMapping("{id}")
    @ApiOperation(value = "Изменение значений сценария", response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Сценарий был изменен."),
            @ApiResponse(code = 403, message = "Доступ запрещён")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).SCENARIO_EDITOR)")
    public ResponseEntity update(@PathVariable Long id, @RequestBody ScenarioDTO_in scenarioDTOIn) {
        log.info("(update): Поступил объект scenarioDTOIn", scenarioDTOIn);

        Scenario scenario = ScenarioDTO_in.ScenarioDTO_to_Scenario(scenarioDTOIn);
        Scenario updatedScenario = scenarioService.updateScenario(id, scenario);
        return new ResponseEntity(ScenarioDTO_out.Scenario_to_ScenarioDTO_out(updatedScenario), HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    @ApiOperation(value = "Удаление значения")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Сценарий был успешно удален"),
            @ApiResponse(code = 403, message = "Доступ запрещён"),
            @ApiResponse(code = 404, message = "Сценарий не был обнаружен")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).SCENARIO_DELETER)")
    public void delete(@PathVariable Long id) {
        scenarioService.delete(id);
    }
}