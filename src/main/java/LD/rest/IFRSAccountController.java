package LD.rest;

import LD.model.IFRSAccount.IFRSAccount;
import LD.model.IFRSAccount.IFRSAccountDTO_in;
import LD.model.IFRSAccount.IFRSAccountDTO_out;
import LD.service.IFRSAccountService;
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
@RequestMapping("/ifrsAccounts")
@Api(value = "Контроллер для счетов IFRS")
@Log4j2
public class IFRSAccountController
{
	@Autowired
	IFRSAccountService ifrsAccountService;

	public IFRSAccountController(IFRSAccountService ifrsAccountService)
	{
		this.ifrsAccountService = ifrsAccountService;
	}

	@GetMapping
	@ApiOperation(value = "Получение всех счетов IFRS", response = ResponseEntity.class)
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).IFRS_ACC_READER)")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Все счета МСФО возвращаются в ответе."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	public List<IFRSAccountDTO_out> getAllIFRSAccounts()
	{
		return ifrsAccountService.getAllIFRSAccounts();
	}

	@GetMapping("{id}")
	@ApiOperation(value = "Получение счета IFRS с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Счет существует, возвращается в ответе."),
			@ApiResponse(code = 404, message = "Доступ запрещён"),
			@ApiResponse(code = 404, message = "Такой Счет отсутствует")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).IFRS_ACC_READER)")
	public ResponseEntity getIFRSAccount(@PathVariable Long id)
	{
		IFRSAccount ifrsAccount = ifrsAccountService.getIFRSAccount(id);
		log.info("(getIFRSAccount): ifrsAccount was taken: " + ifrsAccount);
		return new ResponseEntity(IFRSAccountDTO_out.IFRSAccount_to_IFRSAccount_DTO_out(ifrsAccount), HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение нового счёта", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Новый счёт был сохранен."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).IFRS_ACC_ADDER)")
	public ResponseEntity saveNewIFRSAccount(@RequestBody IFRSAccountDTO_in ifrsAccountDTOIn)
	{
		IFRSAccount ifrsAccount = IFRSAccountDTO_in.IFRSAccountDTO_in_to_IFRSAccount(ifrsAccountDTOIn);
		IFRSAccount newIFRSAccount = ifrsAccountService.saveNewIFRSAccount(ifrsAccount);
		return new ResponseEntity(IFRSAccountDTO_out.IFRSAccount_to_IFRSAccount_DTO_out(newIFRSAccount), HttpStatus.OK);
	}

	@PutMapping("{id}")
	@ApiOperation(value = "Изменение значений счёта", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Счёт был изменен."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).IFRS_ACC_EDITOR)")
	public ResponseEntity update(@PathVariable Long id, @RequestBody IFRSAccountDTO_in ifrsAccountDTOIn)
	{
		log.info("(update): Поступил объект ifrsAccountDTOIn", ifrsAccountDTOIn);

		IFRSAccount ifrsAccount = IFRSAccountDTO_in.IFRSAccountDTO_in_to_IFRSAccount(ifrsAccountDTOIn);
		IFRSAccount updatedIFRSAccount = ifrsAccountService.updateIFRSAccount(id, ifrsAccount);
		return new ResponseEntity(IFRSAccountDTO_out.IFRSAccount_to_IFRSAccount_DTO_out(updatedIFRSAccount), HttpStatus.OK);
	}

	@DeleteMapping("{id}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Счёт был успешно удален"),
			@ApiResponse(code = 404, message = "Доступ запрещён"),
			@ApiResponse(code = 404, message = "Счёт не был обнаружен")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).IFRS_ACC_DELETER)")
	public ResponseEntity delete(@PathVariable Long id)
	{
		return ifrsAccountService.delete(id) ? ResponseEntity.ok().build() : ResponseEntity.status(404).build();
	}
}