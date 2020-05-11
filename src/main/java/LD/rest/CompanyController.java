package LD.rest;

import LD.model.Company.CompanyDTO_in;
import LD.model.Company.Company;
import LD.service.CompanyService;
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

/*
 * For tests in Google Chrome => fetch('/currencies/', {method : 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({short_name: 'RUB'})}).then(console.log)
 * */
@Api(value = "Контроллер для компаний")
@RestController
@RequestMapping("/companies")
@Log4j2
public class CompanyController
{
	@Autowired
	CompanyService companyService;

	public CompanyController(CompanyService companyService)
	{
		this.companyService = companyService;
	}

	@GetMapping
	@ApiOperation(value = "Получение всех компаний", response = ResponseEntity.class)
	public List<Company> getAllCompanies()
	{
		return companyService.getAllCompanies();
	}

	@GetMapping("{id}")
	@ApiOperation(value = "Получение компании с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Компания существует, возвращается в ответе."),
			@ApiResponse(code = 404, message = "Такая компания отсутствует")
	})
	public ResponseEntity getCompany(@PathVariable Long id)
	{
		Company company = companyService.getCompany(id);
		log.info("(getCompany): company was taken: " + company);
		return new ResponseEntity(company, HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение новой компании", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Новая компания была сохранена.")
	public ResponseEntity saveNewCompany(@RequestBody CompanyDTO_in companyDTOOut)
	{
		Company company = CompanyDTO_in.CompanyDTO_in_to_Company(companyDTOOut);
		Company newCompany = companyService.saveNewCompany(company);
		return new ResponseEntity(newCompany, HttpStatus.OK);
	}

	@PutMapping("{id}")
	@ApiOperation(value = "Изменение значений компании", response = ResponseEntity.class)
	@ApiResponse(code = 200, message = "Компания была изменена.")
	public ResponseEntity update(@PathVariable Long id, @RequestBody CompanyDTO_in companyDTOOut)
	{
		log.info("(update): Поступил объект companyDTOOut", companyDTOOut);

		Company company = CompanyDTO_in.CompanyDTO_in_to_Company(companyDTOOut);
		Company updatedCompany = companyService.updateCompany(id, company);
		return new ResponseEntity(updatedCompany, HttpStatus.OK);
	}

	@DeleteMapping("{id}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Компания была успешно удалена"),
			@ApiResponse(code = 404, message = "Компания не была обнаружена")
	})
	public ResponseEntity delete(@PathVariable Long id)
	{
		return companyService.delete(id) ? ResponseEntity.ok().build(): ResponseEntity.status(404).build();
	}

}