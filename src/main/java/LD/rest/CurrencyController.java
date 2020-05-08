package LD.rest;


import LD.model.Currency.CurrencyDTO;
import LD.model.Currency.Currency;
import LD.service.CurrencyService;
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

/*
* For tests in Google Chrome => fetch('/currencies/', {method : 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({short_name: 'RUB'})}).then(console.log)
* */
@Api(value = "Контроллер для валют")
@RestController
@RequestMapping("/currencies")
@Log4j2
public class CurrencyController
{
	@Autowired
	CurrencyService currencyService;

	public CurrencyController(CurrencyService currencyService)
	{
		this.currencyService = currencyService;
	}

	@GetMapping
	@ApiOperation(value = "Получение всех валют", response = ResponseEntity.class)
	public List<Currency> getAllCurrencies()
	{
		return currencyService.getAllCurrencies();
	}

	@GetMapping("{id}")
	@ApiOperation(value = "Получение валюты с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Валюта существует, возвращается в ответе."),
			@ApiResponse(code = 404, message = "Такая валюта отсутствует")
	})
	public ResponseEntity getCurrency(@PathVariable Long id)
	{
		Currency currency = currencyService.getCurrency(id);
		log.info("(getCurrency): currency was taken: " + currency);
		return new ResponseEntity(currency, HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение новой валюты", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Новая валюта была сохранена.")
	public ResponseEntity saveNewCurrency(@RequestBody CurrencyDTO currencyDTO)
	{
		Currency currency = CurrencyDTO.CurrencyDTO_to_Currency(currencyDTO);
		Currency newCurrency = currencyService.saveNewCurrency(currency);
		return new ResponseEntity(newCurrency, HttpStatus.OK);
	}

	@PutMapping("{id}")
	@ApiOperation(value = "Изменение значений валюты", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Валюта была изменена.")
	public ResponseEntity update(@PathVariable Long id, @RequestBody CurrencyDTO currencyDTO)
	{
		log.info("(update): Поступил объект currencyDTO", currencyDTO);

		Currency currency = CurrencyDTO.CurrencyDTO_to_Currency(currencyDTO);

		Currency updatedCurrency = currencyService.updateCurrency(id, currency);
		return new ResponseEntity(updatedCurrency, HttpStatus.OK);
	}

	@DeleteMapping("{id}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Валюта была успешно удалена"),
			@ApiResponse(code = 404, message = "Валюта не была обнаружена")
	})
	public ResponseEntity delete(@PathVariable Long id)
	{
		return currencyService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}

}
