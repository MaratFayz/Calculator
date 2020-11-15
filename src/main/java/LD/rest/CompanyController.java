package LD.rest;

import LD.model.Company.Company;
import LD.model.Company.CompanyDTO_in;
import LD.model.Company.CompanyDTO_out;
import LD.service.CompanyService;
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

/*
 * For tests in Google Chrome => fetch('/currencies/', {method : 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({short_name: 'RUB'})}).then(console.log)
 * */
@Api(value = "Контроллер для компаний")
@RestController
@RequestMapping("/companies")
@Log4j2
public class CompanyController {

    @Autowired
    CompanyService companyService;

    @GetMapping
    @ApiOperation(value = "Получение всех компаний", response = ResponseEntity.class)
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).COMPANY_READER)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Все компании возвращаются в ответе."),
            @ApiResponse(code = 403, message = "Доступ запрещён")
    })
    public List<CompanyDTO_out> getAllCompanies() {
        log.info("Выдаются все компании.");
        return companyService.getAllCompanies();
    }

    @GetMapping("{id}")
    @ApiOperation(value = "Получение компании с определённым id", response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Компания существует, возвращается в ответе."),
            @ApiResponse(code = 403, message = "Доступ запрещён"),
            @ApiResponse(code = 404, message = "Такая компания отсутствует")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).COMPANY_READER)")
    public ResponseEntity getCompany(@PathVariable Long id) {
        Company company = companyService.getCompany(id);
        log.info("(getCompany): company was taken: " + company);
        return new ResponseEntity(CompanyDTO_out.Company_to_CompanyDTO_out(company), HttpStatus.OK);
    }

    @PostMapping
    @ApiOperation(value = "Сохранение новой компании", response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Новая компания была сохранена."),
            @ApiResponse(code = 403, message = "Доступ запрещён")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).COMPANY_ADDER)")
    public ResponseEntity saveNewCompany(@RequestBody CompanyDTO_in companyDTO_in) {
        Company company = CompanyDTO_in.CompanyDTO_in_to_Company(companyDTO_in);

        log.info("Получена компания на вход: {}", company);

        Company newCompany = companyService.saveNewCompany(company);

        log.info("Новая компания на вход: {}", newCompany);

        return new ResponseEntity(CompanyDTO_out.Company_to_CompanyDTO_out(newCompany), HttpStatus.OK);
    }

    @PutMapping("{id}")
    @ApiOperation(value = "Изменение значений компании", response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Компания была изменена."),
            @ApiResponse(code = 403, message = "Доступ запрещён")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).COMPANY_EDITOR)")
    public ResponseEntity update(@PathVariable Long id, @RequestBody CompanyDTO_in companyDTO_in) {
        log.info("(update): Поступил объект companyDTO_in", companyDTO_in);

        Company company = CompanyDTO_in.CompanyDTO_in_to_Company(companyDTO_in);
        Company updatedCompany = companyService.updateCompany(id, company);
        return new ResponseEntity(CompanyDTO_out.Company_to_CompanyDTO_out(updatedCompany), HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    @ApiOperation(value = "Удаление значения")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Компания была успешно удалена"),
            @ApiResponse(code = 403, message = "Доступ запрещён"),
            @ApiResponse(code = 404, message = "Компания не была обнаружена")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).COMPANY_DELETER)")
    public void delete(@PathVariable Long id) {
        companyService.delete(id);
    }
}