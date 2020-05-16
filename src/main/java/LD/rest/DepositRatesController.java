package LD.rest;

import LD.model.DepositRate.*;
import LD.service.DepositRatesService;
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
@Api(value = "Контроллер для ставок депозитов")
@Log4j2
@RequestMapping("/depositRates")
public class DepositRatesController
{
	@Autowired
	DepositRatesService depositRatesService;
	@Autowired
	DepositRateTransform depositRateTransform;

	@GetMapping
	@ApiOperation(value = "Получение всех ставок депозитов", response = ResponseEntity.class)
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).DEPOSIT_RATES_READER)")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Все ставки депозитов возвращаются в ответе."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	public List<DepositRateDTO_out> getAllDepositRates()
	{
		return depositRatesService.getAllDepositRates();
	}

	@GetMapping("{company_id}/{currency_id}/{duration_id}/{scenario_id}/{startDate}/{endDate}")
	@ApiOperation(value = "Получение ставки депозита с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Ставка депозита существует, возвращается в ответе."),
			@ApiResponse(code = 404, message = "Доступ запрещён"),
			@ApiResponse(code = 404, message = "Такая ставка депозита отсутствует")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).DEPOSIT_RATES_READER)")
	public ResponseEntity getDepositRates(@PathVariable Long company_id,
										  @PathVariable Long currency_id,
										  @PathVariable Long duration_id,
										  @PathVariable Long scenario_id,
										  @PathVariable String startDate,
										  @PathVariable String endDate)
	{
		DepositRateID id = depositRateTransform.getDepositRatesID(company_id, currency_id, duration_id, scenario_id, startDate, endDate);
		DepositRate depositRate = depositRatesService.getDepositRate(id);
		log.info("(getDepositRates): depositRate was taken: " + depositRate);
		return new ResponseEntity(depositRateTransform.DepositRates_to_DepositRatesDTO_out(depositRate), HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение новой ставки депозита", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Новая ставка депозита была сохранена."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).DEPOSIT_RATES_ADDER)")
	public ResponseEntity saveNewDepositRate(@RequestBody DepositRateDTO_in depositRateDTO_in)
	{
		DepositRate depositRate = depositRateTransform.DepositRatesDTO_in_to_DepositRates(depositRateDTO_in);
		DepositRate newDepositRate = depositRatesService.saveNewDepositRates(depositRate);
		return new ResponseEntity(depositRateTransform.DepositRates_to_DepositRatesDTO_out(newDepositRate), HttpStatus.OK);
	}

	@PutMapping
	@ApiOperation(value = "Изменение ставок депозитов", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Ставка депозита была изменена.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Ставка депозита была изменена."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).DEPOSIT_RATES_EDITOR)")
	public ResponseEntity update(@RequestBody DepositRateDTO_in depositRateDTO_in)
	{
		log.info("(update): Поступил объект depositRateDTO_in = {}", depositRateDTO_in);

		DepositRate depositRate = depositRateTransform.DepositRatesDTO_in_to_DepositRates(depositRateDTO_in);

		DepositRateID id = depositRateTransform.getDepositRatesID(depositRateDTO_in.getCompany(),
				depositRateDTO_in.getCurrency(),
				depositRateDTO_in.getDuration(),
				depositRateDTO_in.getScenario(),
				depositRateDTO_in.getSTART_PERIOD(),
				depositRateDTO_in.getEND_PERIOD());

		DepositRate updatedDepositRate = depositRatesService.updateDepositRates(id, depositRate);
		return new ResponseEntity(depositRateTransform.DepositRates_to_DepositRatesDTO_out(updatedDepositRate), HttpStatus.OK);
	}

	@DeleteMapping
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Ставка депозита была успешно удалена"),
			@ApiResponse(code = 404, message = "Доступ запрещён"),
			@ApiResponse(code = 404, message = "Ставка депозита не была обнаружена")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).DEPOSIT_RATES_DELETER)")
	public ResponseEntity delete(@RequestBody DepositRateDTO_in depositRateDTO_in)
	{
		DepositRateID id = depositRateTransform.getDepositRatesID(depositRateDTO_in.getCompany(),
				depositRateDTO_in.getCurrency(),
				depositRateDTO_in.getDuration(),
				depositRateDTO_in.getScenario(),
				depositRateDTO_in.getSTART_PERIOD(),
				depositRateDTO_in.getEND_PERIOD());

		return depositRatesService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}
}
