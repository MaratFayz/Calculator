package LD.rest;

import LD.model.EntryIFRSAcc.EntryIFRSAcc;
import LD.model.EntryIFRSAcc.EntryIFRSAccDTO;
import LD.model.EntryIFRSAcc.EntryIFRSAccID;
import LD.model.EntryIFRSAcc.EntryIFRSAccTransform;
import LD.service.EntryIFRSAccService;
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
@Api(value = "Контроллер для записей на счетах МСФО")
@Log4j2
@RequestMapping("/entriesIFRS")
public class EntryIFRSAccController
{
	@Autowired
	EntryIFRSAccService entryIFRSAccService;
	@Autowired
	EntryIFRSAccTransform entryIFRSAccTransform;

	@GetMapping
	@ApiOperation(value = "Получение всех записей на счетах МСФО", response = ResponseEntity.class)
	public List<EntryIFRSAcc> getAllEntryIFRSAccs()
	{
		return entryIFRSAccService.getAllEntriesIFRSAcc();
	}

	@GetMapping("{leasingDeposit_id}/{scenario_id}/{period_id}/{CALCULATION_TIME}/{ifrsAcc_id}")
	@ApiOperation(value = "Получение записи на счетах МСФО с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Запись на счетах МСФО существует, возвращается в ответе."),
			@ApiResponse(code = 404, message = "Такая запись на счетах МСФО отсутствует")
	})
	public ResponseEntity getEntryIFRSAcc(@PathVariable Long leasingDeposit_id,
										  @PathVariable Long scenario_id,
										  @PathVariable Long period_id,
										  @PathVariable String CALCULATION_TIME,
										  @PathVariable Long ifrsAcc_id)
	{
		EntryIFRSAccID id = entryIFRSAccTransform.EntryIFRSAccDTO_to_EntryIFRSAccID(scenario_id, leasingDeposit_id, period_id, CALCULATION_TIME, ifrsAcc_id);
		EntryIFRSAcc entryIFRSAcc = entryIFRSAccService.getEntryIFRSAcc(id);
		log.info("(getEntryIFRSAcc): entryIFRSAcc was taken: " + entryIFRSAcc);
		return new ResponseEntity(entryIFRSAcc, HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение новой записи на счетах МСФО", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Новая запись на счетах МСФО была сохранена.")
	public ResponseEntity saveNewEntryIFRSAcc(@RequestBody EntryIFRSAccDTO entryIFRSAccDTO)
	{
		EntryIFRSAcc entryIFRSAcc = entryIFRSAccTransform.EntryIFRSAccDTO_to_EntryIFRSAcc(entryIFRSAccDTO);
		EntryIFRSAcc newEntryIFRSAcc = entryIFRSAccService.saveNewEntryIFRSAcc(entryIFRSAcc);
		return new ResponseEntity(newEntryIFRSAcc, HttpStatus.OK);
	}

	@PutMapping("{leasingDeposit_id}/{scenario_id}/{period_id}/{CALCULATION_TIME}/{ifrsAcc_id}")
	@ApiOperation(value = "Изменение записи на счетах МСФО", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Запись на счетах МСФО была изменена.")
	public ResponseEntity update(@PathVariable Long leasingDeposit_id,
								 @PathVariable Long scenario_id,
								 @PathVariable Long period_id,
								 @PathVariable String CALCULATION_TIME,
								 @PathVariable Long ifrsAcc_id,
								 @RequestBody EntryIFRSAccDTO entryIFRSAccDTO)
	{
		log.info("(update): Поступил объект entryIFRSAccDTO", entryIFRSAccDTO);

		EntryIFRSAcc entryIFRSAcc = entryIFRSAccTransform.EntryIFRSAccDTO_to_EntryIFRSAcc(entryIFRSAccDTO);

		EntryIFRSAccID id = entryIFRSAccTransform.EntryIFRSAccDTO_to_EntryIFRSAccID(scenario_id, leasingDeposit_id, period_id, CALCULATION_TIME, ifrsAcc_id);
		EntryIFRSAcc updatedEntryIFRSAcc = entryIFRSAccService.updateEntryIFRSAcc(id, entryIFRSAcc);
		return new ResponseEntity(updatedEntryIFRSAcc, HttpStatus.OK);
	}

	@DeleteMapping("{leasingDeposit_id}/{scenario_id}/{period_id}/{CALCULATION_TIME}/{ifrsAcc_id}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Запись на счетах МСФО была успешно удалена"),
			@ApiResponse(code = 404, message = "Запись на счетах МСФО не была обнаружена")
	})
	public ResponseEntity delete(@PathVariable Long leasingDeposit_id,
								 @PathVariable Long scenario_id,
								 @PathVariable Long period_id,
								 @PathVariable String CALCULATION_TIME,
								 @PathVariable Long ifrsAcc_id)
	{
		EntryIFRSAccID id = entryIFRSAccTransform.EntryIFRSAccDTO_to_EntryIFRSAccID(scenario_id, leasingDeposit_id, period_id, CALCULATION_TIME, ifrsAcc_id);
		return entryIFRSAccService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}
}
