package LD.rest;

import LD.model.EndDate.EndDate;
import LD.model.EndDate.EndDateDTO;
import LD.model.EndDate.EndDateID;
import LD.model.EndDate.EndDateTransform;
import LD.service.EndDateService;
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
@Api(value = "Контроллер для конечных дат")
@RequestMapping("/endDates")
@Log4j2
public class EndDateController
{
	@Autowired
	EndDateService endDateService;
	@Autowired
	EndDateTransform endDateTransform;

	public EndDateController(EndDateService endDateService)
	{
		this.endDateService = endDateService;
	}

	@GetMapping
	@ApiOperation(value = "Получение всех конечных дат по лизинговым депозитам")
	public List<EndDateDTO> getAllEndDates()
	{
		return endDateService.getAllEndDates();
	}

	@GetMapping("{leasingDeposit_id}/{scenario_id}/{period_id}")
	@ApiOperation("Получение конечных дат с определённым id")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Конечная дата существует, возвращается в ответе.")
	})
	public EndDate getEndDate(@PathVariable Long leasingDeposit_id,
							  @PathVariable Long scenario_id,
							  @PathVariable Long period_id)
	{
		EndDateID id = endDateTransform.EndDatesDTO_to_EndDatesID(scenario_id, leasingDeposit_id, period_id);
		return endDateService.getEndDate(id);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение новой конечной даты", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Новая конечная дата была сохранена.")
	public ResponseEntity saveNewEndDates(@RequestBody EndDateDTO endDateDTO)
	{
		EndDate endDate = endDateTransform.EndDatesDTO_to_EndDates(endDateDTO);
		EndDate newEndDate = endDateService.saveEndDate(endDate);
		return new ResponseEntity(newEndDate, HttpStatus.OK);
	}

	@PutMapping("{leasingDeposit_id}/{scenario_id}/{period_id}")
	@ApiOperation(value = "Изменение значений конечной даты", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Конечная дата была изменена.")
	public ResponseEntity update(@PathVariable Long leasingDeposit_id,
								 @PathVariable Long scenario_id,
								 @PathVariable Long period_id,
								 @RequestBody EndDateDTO endDateDTO)
	{
		log.info("(update): Поступил объект endDateDTO", endDateDTO);

		EndDate endDate = endDateTransform.EndDatesDTO_to_EndDates(endDateDTO);

		EndDateID id = endDateTransform.EndDatesDTO_to_EndDatesID(scenario_id, leasingDeposit_id, period_id);
		EndDate updatedEndDate = endDateService.update(id, endDate);
		return new ResponseEntity(updatedEndDate, HttpStatus.OK);
	}

	@DeleteMapping("{leasingDeposit_id}/{scenario_id}/{period_id}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Конечная дата была успешно удалена"),
			@ApiResponse(code = 404, message = "Конечная дата не была обнаружена")
	})
	public ResponseEntity delete(@PathVariable Long leasingDeposit_id,
								 @PathVariable Long scenario_id,
								 @PathVariable Long period_id)
	{
		EndDateID id = endDateTransform.EndDatesDTO_to_EndDatesID(scenario_id, leasingDeposit_id, period_id);
		return endDateService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}
}
