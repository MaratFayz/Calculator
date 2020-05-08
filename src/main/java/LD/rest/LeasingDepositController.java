package LD.rest;

import LD.model.LeasingDeposit.LeasingDepositDTO;
import LD.model.LeasingDeposit.LeasingDepositTransform;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.service.LeasingDepositService;
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
@Api(value = "Контроллер для лизинговых депозитов")
@Log4j2
@RequestMapping("/leasingDeposits")
public class LeasingDepositController
{
	@Autowired
	LeasingDepositService leasingDepositService;
	@Autowired
	LeasingDepositTransform leasingDepositTransform;

	@GetMapping
	@ApiOperation(value = "Получение всех лизинговых депозитов", response = ResponseEntity.class)
	public List<LeasingDepositDTO> getAllLeasingDeposits()
	{
		return leasingDepositService.getAllLeasingDeposits();
	}

	@GetMapping("{id}")
	@ApiOperation(value = "Получение лизингового депозита с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Лизинговый депозит существует, возвращается в ответе."),
			@ApiResponse(code = 404, message = "Такой лизинговый депозит отсутствует")
	})
	public ResponseEntity getLeasingDeposit(@PathVariable Long id)
	{
		LeasingDeposit leasingDeposit = leasingDepositService.getLeasingDeposit(id);
		log.info("(getLeasingDeposit): leasingDeposit was taken: " + leasingDeposit);
		return new ResponseEntity(leasingDeposit, HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение нового лизингового депозита", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Новый лизинговый депозит был сохранен.")
	public ResponseEntity saveNewLeasingDeposit(@RequestBody LeasingDepositDTO leasingDepositDTO)
	{
		LeasingDeposit leasingDeposit = leasingDepositTransform.LeasingDepositDTO_to_LeasingDeposit(leasingDepositDTO);
		LeasingDeposit newLeasingDeposit = leasingDepositService.saveNewLeasingDeposit(leasingDeposit);
		return new ResponseEntity(newLeasingDeposit, HttpStatus.OK);
	}

	@PutMapping("{id}")
	@ApiOperation(value = "Изменение значений лизингового депозита", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Лизинговый депозит был изменен.")
	public ResponseEntity update(@PathVariable Long id, @RequestBody LeasingDepositDTO leasingDepositDTO)
	{
		log.info("(update): Поступил объект leasingDepositDTO", leasingDepositDTO);

		LeasingDeposit leasingDeposit = leasingDepositTransform.LeasingDepositDTO_to_LeasingDeposit(leasingDepositDTO);
		LeasingDeposit updatedLeasingDeposit = leasingDepositService.updateLeasingDeposit(id, leasingDeposit);
		return new ResponseEntity(updatedLeasingDeposit, HttpStatus.OK);
	}

	@DeleteMapping("{id}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Лизинговый депозит был успешно удален"),
			@ApiResponse(code = 404, message = "Лизинговый депозит не был обнаружен")
	})
	public ResponseEntity delete(@PathVariable Long id)
	{
		return leasingDepositService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}
}
