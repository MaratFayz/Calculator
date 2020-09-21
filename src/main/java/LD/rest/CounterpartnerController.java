package LD.rest;

import LD.model.Counterpartner.Counterpartner;
import LD.model.Counterpartner.CounterpartnerDTO_in;
import LD.model.Counterpartner.Counterpartner_out;
import LD.service.CounterpartnerService;
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
@Api(value = "Контроллер для контрагентов")
@Log4j2
@RequestMapping("/counterpartners")
public class CounterpartnerController
{
	@Autowired
	CounterpartnerService counterpartnerService;

	@GetMapping
	@ApiOperation(value = "Получение всех контрагентов", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Все контрагенты возвращаются в ответе."),
			@ApiResponse(code = 403, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).COUNTERPARTNER_READER)")
	public List<Counterpartner_out> getAllCounterpartners()
	{
		return counterpartnerService.getAllCounterpartners();
	}

	@GetMapping("{id}")
	@ApiOperation(value = "Получение контрагента с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Контрагент существует, возвращается в ответе."),
			@ApiResponse(code = 403, message = "Доступ запрещён"),
			@ApiResponse(code = 404, message = "Такой контрагент отсутствует")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).COUNTERPARTNER_READER)")
	public ResponseEntity getCounterpartner(@PathVariable Long id)
	{
		Counterpartner counterpartner = counterpartnerService.getCounterpartner(id);
		log.info("(getCounterpartner): counterpartner was taken: " + counterpartner);
		return new ResponseEntity(Counterpartner_out.Counterpartner_to_CounterpartnerDTO_out(counterpartner), HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение нового контрагента", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Новый контрагент был сохранен."),
			@ApiResponse(code = 403, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).COUNTERPARTNER_ADDER)")
	public ResponseEntity saveNewCounterpartner(@RequestBody CounterpartnerDTO_in counterpartnerDTOIn)
	{
		Counterpartner counterpartner = CounterpartnerDTO_in.CounterpartnerDTO_in_to_Counterpartner(counterpartnerDTOIn);
		Counterpartner newCounterpartner = counterpartnerService.saveNewCounterpartner(counterpartner);
		return new ResponseEntity(Counterpartner_out.Counterpartner_to_CounterpartnerDTO_out(newCounterpartner), HttpStatus.OK);
	}

	@PutMapping("{id}")
	@ApiOperation(value = "Изменение значений контрагента", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Контрагент был изменен."),
			@ApiResponse(code = 403, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).COUNTERPARTNER_EDITOR)")
	public ResponseEntity update(@PathVariable Long id, @RequestBody CounterpartnerDTO_in counterpartnerDTOIn)
	{
		log.info("(update): Поступил объект counterpartnerDTOIn", counterpartnerDTOIn);

		Counterpartner counterpartner = CounterpartnerDTO_in.CounterpartnerDTO_in_to_Counterpartner(counterpartnerDTOIn);
		Counterpartner updatedCounterpartner = counterpartnerService.updateCounterpartner(id, counterpartner);
		return new ResponseEntity(Counterpartner_out.Counterpartner_to_CounterpartnerDTO_out(updatedCounterpartner), HttpStatus.OK);
	}

	@DeleteMapping("{id}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Контрагент был успешно удален"),
			@ApiResponse(code = 403, message = "Доступ запрещён"),
			@ApiResponse(code = 404, message = "Контрагент не был обнаружен")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).COUNTERPARTNER_DELETER)")
	public ResponseEntity delete(@PathVariable Long id)
	{
		return counterpartnerService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}
}
