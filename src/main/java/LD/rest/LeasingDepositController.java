package LD.rest;

import LD.model.LeasingDeposit.*;
import LD.service.LeasingDepositService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).LEASING_DEPOSIT_READER)")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Все лизинговые депозиты возвращаются в ответе."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	public List<LeasingDepositDTO_out> getAllLeasingDeposits()
	{
		return leasingDepositService.getAllLeasingDeposits();
	}

	@GetMapping("/for2Scenarios")
	@ApiOperation(value = "Получение всех лизинговых депозитов для определённого периода", response = ResponseEntity.class)
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).LEASING_DEPOSIT_READER)")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Все лизинговые депозиты для определённого периода возвращаются в ответе."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	public List<LeasingDepositDTO_out_onPeriodFor2Scenarios> getAllLeasingDepositsOnPeriodFor2Scenarios(@RequestParam @NonNull Long scenarioFromId,
																										@RequestParam @NonNull Long scenarioToId)
	{
		return leasingDepositService.getAllLeasingDepositsOnPeriodFor2Scenarios(scenarioFromId, scenarioToId);
	}

	@GetMapping("{id}")
	@ApiOperation(value = "Получение лизингового депозита с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Лизинговый депозит существует, возвращается в ответе."),
			@ApiResponse(code = 404, message = "Такой лизинговый депозит отсутствует"),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).LEASING_DEPOSIT_ADDER)")
	public ResponseEntity getLeasingDeposit(@PathVariable Long id)
	{
		LeasingDeposit leasingDeposit = leasingDepositService.getLeasingDeposit(id);
		log.info("(getLeasingDeposit): leasingDeposit was taken: " + leasingDeposit);
		return new ResponseEntity(leasingDepositTransform.LeasingDeposit_to_LeasingDepositDTO_out(leasingDeposit), HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение нового лизингового депозита", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Новый лизинговый депозит был сохранен."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).LEASING_DEPOSIT_ADDER)")
	public ResponseEntity saveNewLeasingDeposit(@RequestBody LeasingDepositDTO_in leasingDepositDTO_in)
	{
		LeasingDeposit leasingDeposit = leasingDepositTransform.LeasingDepositDTO_in_to_LeasingDeposit(leasingDepositDTO_in);
		LeasingDeposit newLeasingDeposit = leasingDepositService.saveNewLeasingDeposit(leasingDeposit);
		return new ResponseEntity(leasingDepositTransform.LeasingDeposit_to_LeasingDepositDTO_out(newLeasingDeposit), HttpStatus.OK);
	}

	@PutMapping("{id}")
	@ApiOperation(value = "Изменение значений лизингового депозита", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Лизинговый депозит был изменен."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).LEASING_DEPOSIT_EDITOR)")
	public ResponseEntity update(@PathVariable Long id, @RequestBody LeasingDepositDTO_in leasingDepositDTO_in)
	{
		log.info("(update): Поступил объект leasingDepositDTO_in = {}", leasingDepositDTO_in);

		LeasingDeposit leasingDeposit = leasingDepositTransform.LeasingDepositDTO_in_to_LeasingDeposit(leasingDepositDTO_in);
		LeasingDeposit updatedLeasingDeposit = leasingDepositService.updateLeasingDeposit(id, leasingDeposit);
		return new ResponseEntity(leasingDepositTransform.LeasingDeposit_to_LeasingDepositDTO_out(updatedLeasingDeposit), HttpStatus.OK);
	}

	@DeleteMapping("{id}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Лизинговый депозит был успешно удален"),
			@ApiResponse(code = 404, message = "Доступ запрещён"),
			@ApiResponse(code = 404, message = "Лизинговый депозит не был обнаружен")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).LEASING_DEPOSIT_DELETER)")
	public ResponseEntity delete(@PathVariable Long id)
	{
		return leasingDepositService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}
}
