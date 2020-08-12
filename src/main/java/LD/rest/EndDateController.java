package LD.rest;

import LD.model.EndDate.*;
import LD.service.EndDateService;
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
@Api(value = "Контроллер для конечных дат")
@RequestMapping("/endDates")
@Log4j2
public class EndDateController
{
	@Autowired
	EndDateService endDateService;
	@Autowired
	EndDateTransform endDateTransform;

	@GetMapping
	@ApiOperation(value = "Получение всех конечных дат по лизинговым депозитам")
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).END_DATE_READER)")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Все конечные даты возвращаются в ответе."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	public List<EndDateDTO_out> getAllEndDates()
	{
		return endDateService.getAllEndDates();
	}

	@GetMapping("{leasingDeposit_id}/{scenario_id}/{period_id}")
	@ApiOperation("Получение конечных дат с определённым id")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Конечная дата существует, возвращается в ответе."),
			@ApiResponse(code = 403, message = "Доступ запрещён"),
			@ApiResponse(code = 404, message = "Такая конечная дата отсутствует")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).END_DATE_READER)")
	public ResponseEntity getEndDate(@PathVariable Long leasingDeposit_id,
							  @PathVariable Long scenario_id,
							  @PathVariable Long period_id)
	{
		EndDateID id = endDateTransform.EndDatesDTO_to_EndDatesID(scenario_id, leasingDeposit_id, period_id);
		return new ResponseEntity(endDateTransform.EndDates_to_EndDatesDTO_out(endDateService.getEndDate(id)), HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение новой конечной даты", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Новая конечная дата была сохранена."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).END_DATE_ADDER)")
	public ResponseEntity saveNewEndDates(@RequestBody EndDateDTO_in endDateDTO_in)
	{
		log.info("endDateDTO_in = {}", endDateDTO_in);
		EndDate endDate = endDateTransform.EndDatesDTO_in_to_EndDates(endDateDTO_in);

		log.info("endDate = {}", endDate);
		EndDate newEndDate = endDateService.saveEndDate(endDate);
		return new ResponseEntity(endDateTransform.EndDates_to_EndDatesDTO_out(newEndDate), HttpStatus.OK);
	}

	@PutMapping
	@ApiOperation(value = "Изменение значений конечной даты", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Конечная дата была изменена."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).END_DATE_EDITOR)")
	public ResponseEntity update(@RequestBody EndDateDTO_in endDateDTO_in)
	{
		log.info("(update): Поступил объект endDateDTO_in = {}", endDateDTO_in);

		EndDate endDate = endDateTransform.EndDatesDTO_in_to_EndDates(endDateDTO_in);

		EndDateID id = endDateTransform.EndDatesDTO_to_EndDatesID(endDateDTO_in.getScenario(),
				endDateDTO_in.getLeasingDeposit_id(),
				endDateDTO_in.getPeriod());

		EndDate updatedEndDate = endDateService.update(id, endDate);
		return new ResponseEntity(endDateTransform.EndDates_to_EndDatesDTO_out(updatedEndDate), HttpStatus.OK);
	}

	@DeleteMapping
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Конечная дата была успешно удалена"),
			@ApiResponse(code = 403, message = "Доступ запрещён"),
			@ApiResponse(code = 404, message = "Конечная дата не была обнаружена")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).END_DATE_DELETER)")
	public ResponseEntity delete(@RequestBody EndDateDTO_in endDateDTO_in)
	{
		EndDateID id = endDateTransform.EndDatesDTO_to_EndDatesID(endDateDTO_in.getScenario(),
				endDateDTO_in.getLeasingDeposit_id(),
				endDateDTO_in.getPeriod());

		return endDateService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}
}
