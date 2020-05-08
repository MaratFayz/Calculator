package LD.rest;

import LD.model.ExchangeRate.ExchangeRate;
import LD.model.ExchangeRate.ExchangeRateDTO;
import LD.model.ExchangeRate.ExchangeRateID;
import LD.model.ExchangeRate.ExchangeRateTransform;
import LD.service.ExchangeRateService;
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

@Api(value = "Контроллер для курсов валют")
@RestController
@RequestMapping("/exchangeRates")
@Log4j2
public class ExchangeRateController
{
	@Autowired
	ExchangeRateService exchangeRateService;
	@Autowired
	ExchangeRateTransform exchangeRateTransform;

	@GetMapping
	@ApiOperation(value = "Получение всех курсов валют", response = ResponseEntity.class)
	public List<ExchangeRateDTO> getAllExchangeRates()
	{
		return exchangeRateService.getAllExchangeRates();
	}

	@GetMapping("{scenario_id}/{currency_id}/{date}")
	@ApiOperation(value = "Получение курса с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Лизинговый депозит существует, возвращается в ответе."),
			@ApiResponse(code = 404, message = "Такой лизинговый депозит отсутствует")
	})
	public ResponseEntity getExchangeRate(@PathVariable Long scenario_id, @PathVariable Long currency_id, @PathVariable String date)
	{
		ExchangeRateID id = exchangeRateTransform.ExchangeRateDTO_to_ExchangeRateKeyInER(scenario_id, currency_id, date);
		ExchangeRate exchangeRate = exchangeRateService.getExchangeRate(id);
		log.info("(getExchangeRate): exchangeRate was taken: " + exchangeRate);
		return new ResponseEntity(exchangeRate, HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение нового курса валют", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Новый курс валют был сохранен.")
	public ResponseEntity saveNewExchangeRate(@RequestBody ExchangeRateDTO exchangeRateDTO)
	{
		ExchangeRate exchangeRate = exchangeRateTransform.ExchangeRateDTO_to_ExchangeRate(exchangeRateDTO);
		ExchangeRate newExchangeRate = exchangeRateService.saveNewExchangeRate(exchangeRate);
		return new ResponseEntity(newExchangeRate, HttpStatus.OK);
	}

	@PutMapping("{scenario_id}/{currency_id}/{date}")
	@ApiOperation(value = "Изменение значений курса валют", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Курс валют был изменен.")
	public ResponseEntity update(@PathVariable Long scenario_id, @PathVariable Long currency_id, @PathVariable String date, @RequestBody ExchangeRateDTO exchangeRateDTO)
	{
		log.info("(update): Поступил объект exchangeRateDTO", exchangeRateDTO);

		ExchangeRate exchangeRate = exchangeRateTransform.ExchangeRateDTO_to_ExchangeRate(exchangeRateDTO);

		ExchangeRateID id = exchangeRateTransform.ExchangeRateDTO_to_ExchangeRateKeyInER(scenario_id, currency_id, date);
		ExchangeRate updatedExchangeRate = exchangeRateService.updateExchangeRate(id, exchangeRate);
		return new ResponseEntity(updatedExchangeRate, HttpStatus.OK);
	}

	@DeleteMapping("{scenario_id}/{currency_id}/{date}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Курс валют был успешно удален"),
			@ApiResponse(code = 404, message = "Курс валют не был обнаружен")
	})
	public ResponseEntity delete(@PathVariable Long scenario_id, @PathVariable Long currency_id, @PathVariable String date)
	{
		ExchangeRateID id = exchangeRateTransform.ExchangeRateDTO_to_ExchangeRateKeyInER(scenario_id, currency_id, date);
		return exchangeRateService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}

}

