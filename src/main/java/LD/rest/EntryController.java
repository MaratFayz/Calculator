package LD.rest;

import LD.model.Entry.*;
import LD.service.EntryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
	public List<EntryDTO_out> getAllEntries()
	{
		return entryService.getAllLDEntries();
	}

	@GetMapping("/regld1")
	@ApiOperation(value = "Получение всех транзакций по лизинговым депозитам для формы Reg.LD.1 для сценария-получателя")
	public List<EntryDTO_out_RegLD1> getAllEntries_RegLD1(@RequestParam @NonNull Long scenarioFromId,
														  @RequestParam @NonNull Long scenarioToId)
	{
		return entryService.getAllLDEntries_RegLD1(scenarioToId);
	}

	@GetMapping("/regld2")
	@ApiOperation(value = "Получение всех транзакций по лизинговым депозитам для формы Reg.LD.2 для сценария-получателя")
	public List<EntryDTO_out_RegLD2> getAllEntries_RegLD2(@RequestParam @NonNull Long scenarioFromId,
														  @RequestParam @NonNull Long scenarioToId)
	{
		return entryService.getAllLDEntries_RegLD2(scenarioToId);
	}

	@GetMapping("/regld3")
	@ApiOperation(value = "Получение всех транзакций по лизинговым депозитам для формы Reg.LD.3 для сценария-получателя")
	public List<EntryDTO_out_RegLD3> getAllEntries_RegLD3(@RequestParam @NonNull Long scenarioFromId,
														  @RequestParam @NonNull Long scenarioToId)
	{
		return entryService.getAllLDEntries_RegLD3(scenarioToId);
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
		EntryID id = entryTransform.getEntryID(scenario_id, leasingDeposit_id, period_id, CALCULATION_TIME);
		return entryService.getEntry(id);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение новой транзакции", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Новая транзакция была сохранена.")
	public ResponseEntity saveNewEntry(@RequestBody EntryDTO_in entryDTO_in)
	{
		Entry entry = entryTransform.EntryDTO_in_to_Entry(entryDTO_in);
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
			@RequestParam(name = "scenario_from") Long scenarioFrom,
			@RequestParam(name = "scenario_to") Long scenarioTo) throws ExecutionException, InterruptedException
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

	@PutMapping
	@ApiOperation(value = "Изменение значений транзакции", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Транзакция была изменена.")
	public ResponseEntity update(@RequestBody EntryDTO_in entryDTO_in)
	{
		log.info("(update): Поступил объект entryDTO_in = {}", entryDTO_in);

		Entry entry = entryTransform.EntryDTO_in_to_Entry(entryDTO_in);

		EntryID id = entryTransform.getEntryID(entryDTO_in.getScenario(),
				entryDTO_in.getLeasingDeposit(),
				entryDTO_in.getPeriod(),
				entryDTO_in.getCALCULATION_TIME());

		Entry updatedEntry = entryService.update(id, entry);
		return new ResponseEntity(updatedEntry, HttpStatus.OK);
	}

	@DeleteMapping
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Транзакция была успешно удалена"),
			@ApiResponse(code = 404, message = "Транзакция не была обнаружена")
	})
	public ResponseEntity delete(@RequestBody EntryDTO_in entryDTO_in)
	{
		log.info("Поступил такой DTO = {}", entryDTO_in);
		EntryID id = entryTransform.getEntryID(entryDTO_in.getScenario(),
				entryDTO_in.getLeasingDeposit(),
				entryDTO_in.getPeriod(),
				entryDTO_in.getCALCULATION_TIME());

		log.info("id стал равен = {}", id);

		return entryService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}
}
