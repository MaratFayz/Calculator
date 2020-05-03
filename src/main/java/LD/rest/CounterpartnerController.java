package LD.rest;

import LD.model.Counterpartner.Counterpartner;
import LD.model.Counterpartner.CounterpartnerDTO;
import LD.service.CounterpartnerService;
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
@Api(value = "Контроллер для контрагентов")
@Log4j2
@RequestMapping("/counterpartners")
public class CounterpartnerController
{
	@Autowired
	CounterpartnerService counterpartnerService;

	public CounterpartnerController(CounterpartnerService counterpartnerService)
	{
		this.counterpartnerService = counterpartnerService;
	}

	@GetMapping
	@ApiOperation(value = "Получение всех контрагентов", response = ResponseEntity.class)
	public List<Counterpartner> getAllCounterpartners()
	{
		return counterpartnerService.getAllCounterpartners();
	}

	@GetMapping("{id}")
	@ApiOperation(value = "Получение контрагента с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Контрагент существует, возвращается в ответе."),
			@ApiResponse(code = 404, message = "Такой контрагент отсутствует")
	})
	public ResponseEntity getCounterpartner(@PathVariable Long id)
	{
		Counterpartner counterpartner = counterpartnerService.getCounterpartner(id);
		log.info("(getCounterpartner): counterpartner was taken: " + counterpartner);
		return new ResponseEntity(counterpartner, HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение нового контрагента", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Новый контрагент был сохранен.")
	public ResponseEntity saveNewCounterpartner(@RequestBody CounterpartnerDTO counterpartnerDTO)
	{
		Counterpartner counterpartner = CounterpartnerDTO.CounterpartnerDTO_to_Counterpartner(counterpartnerDTO);
		Counterpartner newCounterpartner = counterpartnerService.saveNewCounterpartner(counterpartner);
		return new ResponseEntity(newCounterpartner, HttpStatus.OK);
	}

	@PutMapping("{id}")
	@ApiOperation(value = "Изменение значений контрагента", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Контрагент была изменена.")
	public ResponseEntity update(@PathVariable Long id, @RequestBody CounterpartnerDTO counterpartnerDTO)
	{
		log.info("(update): Поступил объект counterpartnerDTO", counterpartnerDTO);

		Counterpartner counterpartner = CounterpartnerDTO.CounterpartnerDTO_to_Counterpartner(counterpartnerDTO);
		Counterpartner updatedCounterpartner = counterpartnerService.updateCounterpartner(id, counterpartner);
		return new ResponseEntity(updatedCounterpartner, HttpStatus.OK);
	}

	@DeleteMapping("{id}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Контрагент был успешно удален"),
			@ApiResponse(code = 404, message = "Контрагент не была обнаружена")
	})
	public ResponseEntity delete(@PathVariable Long id)
	{
		return counterpartnerService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}
}
