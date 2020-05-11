package LD.rest;

import LD.model.Duration.DurationDTO_in;
import LD.model.Duration.Duration;
import LD.service.DurationService;
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
@Api(value = "Контроллер для длительности")
@RequestMapping("/durations")
@Log4j2
public class DurationController
{
	@Autowired
	DurationService durationService;

	public DurationController(DurationService durationService)
	{
		this.durationService = durationService;
	}

	@GetMapping
	@ApiOperation(value = "Получение всех длительностей", response = ResponseEntity.class)
	public List<Duration> getAllDurations()
	{
		return durationService.getAllDurations();
	}

	@GetMapping("{id}")
	@ApiOperation(value = "Получение длительности с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Длительность существует, возвращается в ответе."),
			@ApiResponse(code = 404, message = "Такая длительность отсутствует")
	})
	public ResponseEntity getDuration(@PathVariable Long id)
	{
		Duration duration = durationService.getDuration(id);
		log.info("(getDuration): duration was taken: " + duration);
		return new ResponseEntity(duration, HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение новой длительности", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Новая длительность была сохранена.")
	public ResponseEntity saveNewDuration(@RequestBody DurationDTO_in durationDTOIn)
	{
		Duration duration = DurationDTO_in.DurationDTO_in_to_Duration(durationDTOIn);
		Duration newDuration = durationService.saveNewDuration(duration);
		return new ResponseEntity(newDuration, HttpStatus.OK);
	}

	@PutMapping("{id}")
	@ApiOperation(value = "Изменение значений длительности", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Длительность была изменена.")
	public ResponseEntity update(@PathVariable Long id, @RequestBody DurationDTO_in durationDTOIn)
	{
		log.info("(update): Поступил объект durationDTOIn", durationDTOIn);

		Duration duration = DurationDTO_in.DurationDTO_in_to_Duration(durationDTOIn);
		Duration updatedDuration = durationService.updateDuration(id, duration);
		return new ResponseEntity(updatedDuration, HttpStatus.OK);
	}

	@DeleteMapping("{id}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Длительность была успешно удалена"),
			@ApiResponse(code = 404, message = "Длительность не была обнаружена")
	})
	public ResponseEntity delete(@PathVariable Long id)
	{
		return durationService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}
}
