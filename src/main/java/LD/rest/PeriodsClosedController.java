package LD.rest;

import LD.model.PeriodsClosed.PeriodsClosed;
import LD.model.PeriodsClosed.PeriodsClosedDTO;
import LD.model.PeriodsClosed.PeriodsClosedID;
import LD.model.PeriodsClosed.PeriodsClosedTransform;
import LD.model.PeriodsClosed.PeriodsClosedTransform;
import LD.service.PeriodsClosedService;
import LD.service.PeriodsClosedService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
	public List<PeriodsClosed> getAllPeriodsClosed()
	{
		return periodsClosedService.getAllPeriodsClosed();
	}

	@GetMapping("{scenario_id}/{period_id}")
	@ApiOperation(value = "Получение периода и статуса закрытия с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Период и статус закрытия существует, возвращается в ответе."),
			@ApiResponse(code = 404, message = "Такой период и статус закрытия отсутствует")
	})
	public ResponseEntity getPeriodsClosed(@PathVariable Long scenario_id, @PathVariable Long period_id)
	{
		PeriodsClosedID id = periodsClosedTransform.PeriodsClosedDTO_to_PeriodsClosedID(scenario_id, period_id);
		PeriodsClosed periodClosed = periodsClosedService.getPeriodsClosed(id);
		log.info("(getPeriodsClosed): periodClosed was taken: " + periodClosed);
		return new ResponseEntity(periodClosed, HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение периода со статусом закрытия", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Новый период со статусом закрытия был сохранен.")
	public ResponseEntity saveNewPeriodsClosed(@RequestBody PeriodsClosedDTO periodsClosedDTO)
	{
		PeriodsClosed periodClosed = periodsClosedTransform.PeriodsClosedDTO_to_PeriodsClosed(periodsClosedDTO);
		PeriodsClosed newPeriodsClosed = periodsClosedService.saveNewPeriodsClosed(periodClosed);
		return new ResponseEntity(newPeriodsClosed, HttpStatus.OK);
	}

	@PutMapping("{scenario_id}/{period_id}")
	@ApiOperation(value = "Изменение значений периода со статусом закрытия", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Период со статусом закрытия был изменен.")
	public ResponseEntity update(@PathVariable Long scenario_id, @PathVariable Long period_id, @RequestBody PeriodsClosedDTO periodsClosedDTO)
	{
		log.info("(update): Поступил объект periodsClosedDTO", periodsClosedDTO);

		PeriodsClosed periodClosed = periodsClosedTransform.PeriodsClosedDTO_to_PeriodsClosed(periodsClosedDTO);

		PeriodsClosedID id = periodsClosedTransform.PeriodsClosedDTO_to_PeriodsClosedID(scenario_id, period_id);
		PeriodsClosed updatedPeriodsClosed = periodsClosedService.updatePeriodsClosed(id, periodClosed);
		return new ResponseEntity(updatedPeriodsClosed, HttpStatus.OK);
	}

	@DeleteMapping("{scenario_id}/{period_id}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Период со статусом закрытия был успешно удален"),
			@ApiResponse(code = 404, message = "Период со статусом закрытия не был обнаружен")
	})
	public ResponseEntity delete(@PathVariable Long scenario_id, @PathVariable Long period_id)
	{
		PeriodsClosedID id = periodsClosedTransform.PeriodsClosedDTO_to_PeriodsClosedID(scenario_id, period_id);
		return periodsClosedService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}
}
