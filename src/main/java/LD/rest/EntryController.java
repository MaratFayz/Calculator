package LD.rest;

import LD.model.Entry.Entry;
import LD.model.Entry.EntryDTO;
import LD.model.Entry.EntryID;
import LD.model.Entry.EntryTransform;
import LD.model.ExchangeRate.ExchangeRate;
import LD.model.ExchangeRate.ExchangeRateDTO;
import LD.model.ExchangeRate.ExchangeRateID;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.LeasingDeposit.LeasingDepositDTO;
import LD.model.Period.Period;
import LD.model.Scenario.Scenario;
import LD.service.EntryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Api(value = "Контроллер для транзакций по лизинговым депозитам")
@RestController
@RequestMapping("/entries")
@Log4j2
public class EntryController
{
	@Autowired
	EntryService entryService;
	@Autowired
	EntryTransform entryTransform;

	public EntryController(EntryService entryService)
	{
		this.entryService = entryService;
	}

	@GetMapping
	@ApiOperation(value = "Получение всех транзакций по лизинговым депозитам")
	public List<Entry> getAllEntries()
	{
		return entryService.getAllLDEntries();
	}

	@GetMapping("{leasingDeposit_id}/{scenario_id}/{period_id}/{CALCULATION_TIME}")
	@ApiOperation("Получение транзакции с определённым id")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Транзакция существует, возвращается в ответе.")
	})
	public Entry getEntry(@PathVariable Long leasingDeposit_id,
						  @PathVariable Long scenario_id,
						  @PathVariable Long period_id,
						  @PathVariable String CALCULATION_TIME)
	{
		EntryID id = entryTransform.EntryDTO_to_EntryID(scenario_id, leasingDeposit_id, period_id, CALCULATION_TIME);
		return entryService.getEntry(id);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение новой транзакции", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Новая транзакция была сохранена.")
	public ResponseEntity saveNewEntry(@RequestBody EntryDTO entryDTO)
	{
		Entry entry = entryTransform.EntryDTO_to_Entry(entryDTO);
		Entry newEntry = entryService.saveEntry(entry);
		return new ResponseEntity(newEntry, HttpStatus.OK);
	}

	@PostMapping("/calculator")
	@ApiOperation(value = "Расчет транзакций по лизинговым депозитам")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Расчет завершился корректно"),
			@ApiResponse(code = 500, message = "Произошла ошибка при расчете")
	})
	public ResponseEntity calculateAllEntries(
			@RequestParam(name = "scenario_from") String scenarioFrom,
			@RequestParam(name = "scenario_to") String scenarioTo) throws ExecutionException, InterruptedException
	{
		try
		{
			log.info("Расчет транзакций начался. Значения параметров: From = {}, To = {}", scenarioFrom, scenarioTo);
			entryService.calculateEntries(scenarioFrom, scenarioTo);
			log.info("Расчет транзакций окончен. Значения параметров: From = {}, To = {}", scenarioFrom, scenarioTo);
		}
		catch (Exception any)
		{
			log.info("Возникло исключение при расчете транзакций: {}", any.toString());
			return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity(HttpStatus.OK);
	}

	@PutMapping("{leasingDeposit_id}/{scenario_id}/{period_id}/{CALCULATION_TIME}")
	@ApiOperation(value = "Изменение значений транзакции", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Транзакция была изменена.")
	public ResponseEntity update(@PathVariable Long leasingDeposit_id,
								 @PathVariable Long scenario_id,
								 @PathVariable Long period_id,
								 @PathVariable String CALCULATION_TIME,
								 @RequestBody EntryDTO entryDTO)
	{
		log.info("(update): Поступил объект entryDTO", entryDTO);

		Entry entry = entryTransform.EntryDTO_to_Entry(entryDTO);

		EntryID id = entryTransform.EntryDTO_to_EntryID(scenario_id, leasingDeposit_id, period_id, CALCULATION_TIME);
		Entry updatedEntry = entryService.update(id, entry);
		return new ResponseEntity(updatedEntry, HttpStatus.OK);
	}

	@DeleteMapping("{leasingDeposit_id}/{scenario_id}/{period_id}/{CALCULATION_TIME}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Транзакция была успешно удалена"),
			@ApiResponse(code = 404, message = "Транзакция не была обнаружена")
	})
	public ResponseEntity delete(@PathVariable Long leasingDeposit_id,
								 @PathVariable Long scenario_id,
								 @PathVariable Long period_id,
								 @PathVariable String CALCULATION_TIME)
	{
		EntryID id = entryTransform.EntryDTO_to_EntryID(scenario_id, leasingDeposit_id, period_id, CALCULATION_TIME);
		return entryService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}
}
