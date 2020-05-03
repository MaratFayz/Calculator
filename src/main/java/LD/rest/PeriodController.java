package LD.rest;

import LD.model.Period.Period;
import LD.model.Period.PeriodDTO;
import LD.service.PeriodService;
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
	public List<Period> getAllPeriods()
	{
		return periodService.getAllPeriods();
	}

	@GetMapping("{id}")
	@ApiOperation(value = "Получение периода с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Период существует, возвращается в ответе."),
			@ApiResponse(code = 404, message = "Такой период отсутствует")
	})
	public ResponseEntity getPeriod(@PathVariable Long id)
	{
		Period period = periodService.getPeriod(id);
		log.info("(getPeriod): period was taken: " + period);
		return new ResponseEntity(period, HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение нового периода", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Новый период был сохранен.")
	public ResponseEntity saveNewPeriod(@RequestBody PeriodDTO periodDTO)
	{
		Period period = PeriodDTO.PeriodDTO_to_Period(periodDTO);
		Period newPeriod = periodService.saveNewPeriod(period);
		return new ResponseEntity(newPeriod, HttpStatus.OK);
	}

	@PutMapping("{id}")
	@ApiOperation(value = "Изменение значений периода", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Период был изменен.")
	public ResponseEntity update(@PathVariable Long id, @RequestBody PeriodDTO periodDTO)
	{
		log.info("(update): Поступил объект periodDTO", periodDTO);

		Period period = PeriodDTO.PeriodDTO_to_Period(periodDTO);
		Period updatedPeriod = periodService.updatePeriod(id, period);
		return new ResponseEntity(updatedPeriod, HttpStatus.OK);
	}

	@DeleteMapping("{id}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Период был успешно удален"),
			@ApiResponse(code = 404, message = "Период не был обнаружен")
	})
	public ResponseEntity delete(@PathVariable Long id)
	{
		return periodService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}
}
