package LD.rest;

import LD.model.Scenario.ScenarioDTO;
import LD.model.Scenario.Scenario;
import LD.service.ScenarioService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/scenarios")
@Api(value = "Контроллер для сценариев")
@Log4j2
public class ScenarioController
{
	@Autowired
	ScenarioService scenarioService;

	public ScenarioController(ScenarioService scenarioService)
	{
		this.scenarioService = scenarioService;
	}

	@GetMapping
	@ApiOperation(value = "Получение всех сценариев", response = ResponseEntity.class)
	public List<Scenario> getAllScenarios()
	{
		return scenarioService.getAllScenarios();
	}

	@GetMapping("{id}")
	@ApiOperation(value = "Получение сценария с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Сценарий существует, возвращается в ответе."),
			@ApiResponse(code = 404, message = "Такой сценарий отсутствует")
	})
	public ResponseEntity getScenario(@PathVariable Long id)
	{
		Scenario scenario = scenarioService.getScenario(id);
		log.info("(getScenario): scenario was taken: " + scenario);
		return new ResponseEntity(scenario, HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение нового сценария", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Новый сценарий был сохранен.")
	public ResponseEntity saveNewScenario(@RequestBody ScenarioDTO scenarioDTO)
	{
		Scenario scenario = ScenarioDTO.ScenarioDTO_to_Scenario(scenarioDTO);
		Scenario newScenario = scenarioService.saveNewScenario(scenario);
		return new ResponseEntity(newScenario, HttpStatus.OK);
	}

	@PutMapping("{id}")
	@ApiOperation(value = "Изменение значений сценария", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Сценарий был изменен.")
	public ResponseEntity update(@PathVariable Long id, @RequestBody ScenarioDTO scenarioDTO)
	{
		log.info("(update): Поступил объект scenarioDTO", scenarioDTO);

		Scenario scenario = ScenarioDTO.ScenarioDTO_to_Scenario(scenarioDTO);
		Scenario updatedScenario = scenarioService.updateScenario(id, scenario);
		return new ResponseEntity(updatedScenario, HttpStatus.OK);
	}

	@DeleteMapping("{id}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Сценарий был успешно удален"),
			@ApiResponse(code = 404, message = "Сценарий не был обнаружен")
	})
	public ResponseEntity delete(@PathVariable Long id)
	{
		return scenarioService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}
}
