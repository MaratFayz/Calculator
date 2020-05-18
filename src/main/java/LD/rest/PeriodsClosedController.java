package LD.rest;

import LD.model.PeriodsClosed.*;
import LD.service.PeriodsClosedService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Api(value = "Контроллер для закрытия периодов")
@Log4j2
@RequestMapping("/periodsClosed")
public class PeriodsClosedController
{
	@Autowired
	PeriodsClosedService periodsClosedService;
	@Autowired
	PeriodsClosedTransform periodsClosedTransform;

	@GetMapping
	@ApiOperation(value = "Получение всех периодов и их статусов закрытия", response = ResponseEntity.class)
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).PERIODS_CLOSED_READER)")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Все периоды и их статусы закрытия возвращаются в ответе."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	public List<PeriodsClosedDTO_out> getAllPeriodsClosed()
	{
		return periodsClosedService.getAllPeriodsClosed();
	}

	@GetMapping("{scenario_id}/{period_id}")
	@ApiOperation(value = "Получение периода и статуса закрытия с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Период и статус закрытия существует, возвращается в ответе."),
			@ApiResponse(code = 404, message = "Доступ запрещён"),
			@ApiResponse(code = 404, message = "Такой период и статус закрытия отсутствует")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).PERIODS_CLOSED_READER)")
	public ResponseEntity getPeriodsClosed(@PathVariable Long scenario_id, @PathVariable Long period_id)
	{
		PeriodsClosedID id = periodsClosedTransform.PeriodsClosedDTO_to_PeriodsClosedID(scenario_id, period_id);
		PeriodsClosed periodClosed = periodsClosedService.getPeriodsClosed(id);
		log.info("(getPeriodsClosed): periodClosed was taken: " + periodClosed);
		return new ResponseEntity(periodsClosedTransform.PeriodsClosed_to_PeriodsClosedDTO_out(periodClosed), HttpStatus.OK);
	}

	@GetMapping("{scenario_id}")
	@ApiOperation(value = "Получение по сценарию периода закрытия", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Период закрытия в сценарии существует, возвращается в ответе."),
			@ApiResponse(code = 404, message = "Доступ запрещён"),
			@ApiResponse(code = 500, message = "Нет последнего закрытого периода в сценарии")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).PERIODS_CLOSED_READER)")
	public ResponseEntity getPeriodsClosed(@PathVariable Long scenario_id)
	{
		log.info("(getPeriodsClosed): запрос для id сценария {}", scenario_id);
		String result = periodsClosedService.getDateFirstOpenPeriodForScenario(scenario_id);
		log.info("(getPeriodsClosed): результат запроса первого открытого периода для id сценария {} => {}", scenario_id, result);
		return new ResponseEntity(result, HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение периода со статусом закрытия", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Новый период со статусом закрытия был сохранен."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).PERIODS_CLOSED_ADDER)")
	public ResponseEntity saveNewPeriodsClosed(@RequestBody PeriodsClosedDTO_in periodsClosedDTO_in)
	{
		PeriodsClosed periodClosed = periodsClosedTransform.PeriodsClosedDTO_in_to_PeriodsClosed(periodsClosedDTO_in);
		PeriodsClosed newPeriodsClosed = periodsClosedService.saveNewPeriodsClosed(periodClosed);
		return new ResponseEntity(periodsClosedTransform.PeriodsClosed_to_PeriodsClosedDTO_out(newPeriodsClosed), HttpStatus.OK);
	}

	@PutMapping
	@ApiOperation(value = "Изменение значений периода со статусом закрытия", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Период со статусом закрытия был изменен."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).PERIODS_CLOSED_EDITOR)")
	public ResponseEntity update(@RequestBody PeriodsClosedDTO_in periodsClosedDTO_in)
	{
		log.info("(update): Поступил объект periodsClosedDTO_in = {}", periodsClosedDTO_in);

		PeriodsClosed periodClosed = periodsClosedTransform.PeriodsClosedDTO_in_to_PeriodsClosed(periodsClosedDTO_in);

		PeriodsClosedID id = periodsClosedTransform.PeriodsClosedDTO_to_PeriodsClosedID(periodsClosedDTO_in.getScenario(),
				periodsClosedDTO_in.getPeriod());

		PeriodsClosed updatedPeriodsClosed = periodsClosedService.updatePeriodsClosed(id, periodClosed);
		return new ResponseEntity(periodsClosedTransform.PeriodsClosed_to_PeriodsClosedDTO_out(updatedPeriodsClosed), HttpStatus.OK);
	}

	@DeleteMapping
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Период со статусом закрытия был успешно удален"),
			@ApiResponse(code = 404, message = "Доступ запрещён"),
			@ApiResponse(code = 404, message = "Период со статусом закрытия не был обнаружен")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).PERIODS_CLOSED_DELETER)")
	public ResponseEntity delete(@RequestBody PeriodsClosedDTO_in periodsClosedDTO_in)
	{
		PeriodsClosedID id = periodsClosedTransform.PeriodsClosedDTO_to_PeriodsClosedID(periodsClosedDTO_in.getScenario(),
				periodsClosedDTO_in.getPeriod());

		return periodsClosedService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}

	@PutMapping("/autoClosingPeriods")
	@ApiOperation(value = "Автоматическое массовое изменение значений периода со статусом закрытия", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Периоды со статусом закрытия былы изменены."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).AUTO_CLOSING_PERIODS)")
	public ResponseEntity autoClosingPeriods(@RequestParam String dateBeforeToClose, @RequestParam long scenario_id)
	{
		log.info("Для автоматического закрытия поступила следующая дата = {} по сценарию = {}", dateBeforeToClose, scenario_id);

		periodsClosedService.autoClosePeriods(dateBeforeToClose, scenario_id);

		return ResponseEntity.ok().build();
	}
}
