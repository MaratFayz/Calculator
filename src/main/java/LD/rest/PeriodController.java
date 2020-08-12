package LD.rest;

import LD.model.Period.Period;
import LD.model.Period.PeriodDTO_in;
import LD.model.Period.PeriodDTO_out;
import LD.service.PeriodService;
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
@Api(value = "Контроллер для периодов")
@Log4j2
@RequestMapping("/periods")
public class PeriodController
{
	@Autowired
	PeriodService periodService;

	public PeriodController(PeriodService periodService)
	{
		this.periodService = periodService;
	}

	@GetMapping
	@ApiOperation(value = "Получение всех периодов", response = ResponseEntity.class)
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).PERIOD_READER)")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Все периоды возвращаются в ответе."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	public List<PeriodDTO_out> getAllPeriods()
	{
		return periodService.getAllPeriods();
	}

	@GetMapping("{id}")
	@ApiOperation(value = "Получение периода с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Период существует, возвращается в ответе."),
			@ApiResponse(code = 403, message = "Доступ запрещён"),
			@ApiResponse(code = 404, message = "Такой период отсутствует")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).PERIOD_READER)")
	public ResponseEntity getPeriod(@PathVariable Long id)
	{
		Period period = periodService.getPeriod(id);
		log.info("(getPeriod): period was taken: " + period);
		return new ResponseEntity(PeriodDTO_out.Period_to_PeriodDTO_out(period), HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение нового периода", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Новый период был сохранен."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).PERIOD_ADDER)")
	public ResponseEntity saveNewPeriod(@RequestBody PeriodDTO_in periodDTOIn)
	{
		Period period = PeriodDTO_in.PeriodDTO_to_Period(periodDTOIn);
		Period newPeriod = periodService.saveNewPeriod(period);
		return new ResponseEntity(PeriodDTO_out.Period_to_PeriodDTO_out(newPeriod), HttpStatus.OK);
	}

	@PutMapping("{id}")
	@ApiOperation(value = "Изменение значений периода", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Период был изменен."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).PERIOD_EDITOR)")
	public ResponseEntity update(@PathVariable Long id, @RequestBody PeriodDTO_in periodDTOIn)
	{
		log.info("(update): Поступил объект periodDTOIn", periodDTOIn);

		Period period = PeriodDTO_in.PeriodDTO_to_Period(periodDTOIn);
		Period updatedPeriod = periodService.updatePeriod(id, period);
		return new ResponseEntity(PeriodDTO_out.Period_to_PeriodDTO_out(updatedPeriod), HttpStatus.OK);
	}

	@DeleteMapping("{id}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Период был успешно удален"),
			@ApiResponse(code = 403, message = "Доступ запрещён"),
			@ApiResponse(code = 404, message = "Период не был обнаружен")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).PERIOD_DELETER)")
	public ResponseEntity delete(@PathVariable Long id)
	{
		return periodService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}

	@PostMapping("/autoCreatePeriods")
	@ApiOperation(value = "Автоматическое добавление значений периодов в базу данных")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Периоды были успешно добавлены"),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).AUTO_ADDING_PERIODS)")
	public ResponseEntity autoCreatePeriods(@RequestParam String dateFrom, @RequestParam String dateTo)
	{
		periodService.autoCreatePeriods(dateFrom, dateTo);
		return ResponseEntity.ok().build();
	}
}
