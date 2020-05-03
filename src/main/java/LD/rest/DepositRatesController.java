package LD.rest;

import LD.model.DepositRate.DepositRate;
import LD.model.DepositRate.DepositRateDTO;
import LD.model.DepositRate.DepositRateID;
import LD.model.DepositRate.DepositRateTransform;
import LD.service.DepositRatesService;
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
	public List<DepositRate> getAllDepositRates()
	{
		return depositRatesService.getAllDepositRates();
	}

	@GetMapping("{company_id}/{currency_id}/{duration_id}/{scenario_id}/{startDate}/{endDate}")
	@ApiOperation(value = "Получение ставки депозита с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Ставка депозита существует, возвращается в ответе."),
			@ApiResponse(code = 404, message = "Такая ставка депозита отсутствует")
	})
	public ResponseEntity getDepositRates(@PathVariable Long company_id,
										  @PathVariable Long currency_id,
										  @PathVariable Long duration_id,
										  @PathVariable Long scenario_id,
										  @PathVariable String startDate,
										  @PathVariable String endDate)
	{
		DepositRateID id = depositRateTransform.DepositRatesDTO_to_DepositRatesID(company_id, currency_id, duration_id, scenario_id, startDate, endDate);
		DepositRate depositRate = depositRatesService.getDepositRate(id);
		log.info("(getDepositRates): depositRate was taken: " + depositRate);
		return new ResponseEntity(depositRate, HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение новой ставки депозита", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Новая ставка депозита была сохранена.")
	public ResponseEntity saveNewDepositRate(@RequestBody DepositRateDTO depositRateDTO)
	{
		DepositRate depositRate = depositRateTransform.DepositRatesDTO_to_DepositRates(depositRateDTO);
		DepositRate newDepositRate = depositRatesService.saveNewDepositRates(depositRate);
		return new ResponseEntity(newDepositRate, HttpStatus.OK);
	}

	@PutMapping("{company_id}/{currency_id}/{duration_id}/{scenario_id}/{startDate}/{endDate}")
	@ApiOperation(value = "Изменение ставок депозитов", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Ставка депозита была изменена.")
	public ResponseEntity update(@PathVariable Long company_id,
								 @PathVariable Long currency_id,
								 @PathVariable Long duration_id,
								 @PathVariable Long scenario_id,
								 @PathVariable String startDate,
								 @PathVariable String endDate,
								 @RequestBody DepositRateDTO depositRateDTO)
	{
		log.info("(update): Поступил объект depositRateDTO", depositRateDTO);

		DepositRate depositRate = depositRateTransform.DepositRatesDTO_to_DepositRates(depositRateDTO);

		DepositRateID id = depositRateTransform.DepositRatesDTO_to_DepositRatesID(company_id, currency_id, duration_id, scenario_id, startDate, endDate);
		DepositRate updatedDepositRate = depositRatesService.updateDepositRates(id, depositRate);
		return new ResponseEntity(updatedDepositRate, HttpStatus.OK);
	}

	@DeleteMapping("{company_id}/{currency_id}/{duration_id}/{scenario_id}/{startDate}/{endDate}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Ставка депозита была успешно удалена"),
			@ApiResponse(code = 404, message = "Ставка депозита не была обнаружена")
	})
	public ResponseEntity delete(@PathVariable Long company_id,
								 @PathVariable Long currency_id,
								 @PathVariable Long duration_id,
								 @PathVariable Long scenario_id,
								 @PathVariable String startDate,
								 @PathVariable String endDate)
	{
		DepositRateID id = depositRateTransform.DepositRatesDTO_to_DepositRatesID(company_id, currency_id, duration_id, scenario_id, startDate, endDate);
		return depositRatesService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}
}
