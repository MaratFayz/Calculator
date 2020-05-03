package LD.rest;

import LD.model.IFRSAccount.IFRSAccount;
import LD.model.IFRSAccount.IFRSAccountDTO;
import LD.service.IFRSAccountService;
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
	public List<IFRSAccount> getAllIFRSAccounts()
	{
		return ifrsAccountService.getAllIFRSAccounts();
	}

	@GetMapping("{id}")
	@ApiOperation(value = "Получение счета IFRS с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Счет существует, возвращается в ответе."),
			@ApiResponse(code = 404, message = "Такой Счет отсутствует")
	})
	public ResponseEntity getIFRSAccount(@PathVariable Long id)
	{
		IFRSAccount ifrsAccount = ifrsAccountService.getIFRSAccount(id);
		log.info("(getIFRSAccount): ifrsAccount was taken: " + ifrsAccount);
		return new ResponseEntity(ifrsAccount, HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение нового счёта", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Новый счёт был сохранен.")
	public ResponseEntity saveNewIFRSAccount(@RequestBody IFRSAccountDTO ifrsAccountDTO)
	{
		IFRSAccount ifrsAccount = IFRSAccountDTO.IFRSAccountDTO_to_IFRSAccount(ifrsAccountDTO);
		IFRSAccount newIFRSAccount = ifrsAccountService.saveNewIFRSAccount(ifrsAccount);
		return new ResponseEntity(newIFRSAccount, HttpStatus.OK);
	}

	@PutMapping("{id}")
	@ApiOperation(value = "Изменение значений счёта", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Счёт был изменен.")
	public ResponseEntity update(@PathVariable Long id, @RequestBody IFRSAccountDTO ifrsAccountDTO)
	{
		log.info("(update): Поступил объект ifrsAccountDTO", ifrsAccountDTO);

		IFRSAccount ifrsAccount = IFRSAccountDTO.IFRSAccountDTO_to_IFRSAccount(ifrsAccountDTO);
		IFRSAccount updatedIFRSAccount = ifrsAccountService.updateIFRSAccount(id, ifrsAccount);
		return new ResponseEntity(updatedIFRSAccount, HttpStatus.OK);
	}

	@DeleteMapping("{id}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Счёт был успешно удален"),
			@ApiResponse(code = 404, message = "Счёт не был обнаружен")
	})
	public ResponseEntity delete(@PathVariable Long id)
	{
		return ifrsAccountService.delete(id) ? ResponseEntity.ok().build() : ResponseEntity.status(404).build();
	}
}