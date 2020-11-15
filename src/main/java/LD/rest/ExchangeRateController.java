package LD.rest;

import LD.model.ExchangeRate.*;
import LD.service.ExchangeRate.ExchangeRateService;
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

@Api(value = "Контроллер для курсов валют")
@RestController
@RequestMapping("/exchangeRates")
@Log4j2
public class ExchangeRateController {

    @Autowired
    ExchangeRateService exchangeRateService;
    @Autowired
    ExchangeRateTransform exchangeRateTransform;

    @GetMapping
    @ApiOperation(value = "Получение всех курсов валют", response = ResponseEntity.class)
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).EXCHANGE_RATE_READER)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Все курсы возвращаются в ответе."),
            @ApiResponse(code = 403, message = "Доступ запрещён")
    })
    public List<ExchangeRateDTO_out> getAllExchangeRates() {
        return exchangeRateService.getAllExchangeRates();
    }

    @GetMapping("{scenario_id}/{currency_id}/{date}")
    @ApiOperation(value = "Получение курса с определённым id", response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Валютный курс существует, возвращается в ответе."),
            @ApiResponse(code = 403, message = "Доступ запрещён"),
            @ApiResponse(code = 404, message = "Валютный курс отсутствует")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).EXCHANGE_RATE_READER)")
    public ResponseEntity getExchangeRate(@PathVariable Long scenario_id, @PathVariable Long currency_id, @PathVariable String date) {
        ExchangeRateID id = exchangeRateTransform.ExchangeRateDTO_to_ExchangeRateKeyInER(scenario_id, currency_id, date);
        ExchangeRate exchangeRate = exchangeRateService.getExchangeRate(id);
        log.info("(getExchangeRate): exchangeRate was taken: " + exchangeRate);
        return new ResponseEntity(exchangeRateTransform.ExchangeRate_to_ExchangeRateDTO_out(exchangeRate), HttpStatus.OK);
    }

    @PostMapping
    @ApiOperation(value = "Сохранение нового курса валют", response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Новый курс валют был сохранен."),
            @ApiResponse(code = 403, message = "Доступ запрещён")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).EXCHANGE_RATE_ADDER)")
    public ResponseEntity saveNewExchangeRate(@RequestBody ExchangeRateDTO_in exchangeRateDTO_in) {
        ExchangeRate exchangeRate = exchangeRateTransform.ExchangeRateDTO_in_to_ExchangeRate(exchangeRateDTO_in);
        ExchangeRate newExchangeRate = exchangeRateService.saveNewExchangeRate(exchangeRate);
        return new ResponseEntity(exchangeRateTransform.ExchangeRate_to_ExchangeRateDTO_out(newExchangeRate), HttpStatus.OK);
    }

    @PostMapping("/importERFromCBR")
    @ApiOperation(value = "Загрузка курсов валют с сайта Центробанка РФ по датам, если дата есть, а курса нет",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Курсы валют были импортированы и сохранены."),
            @ApiResponse(code = 403, message = "Доступ запрещён")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).LOAD_EXCHANGE_RATE_FROM_CBR)")
    public ResponseEntity importExchangeRatesFormCBR(@RequestParam long scenario_id,
                                                     @RequestParam boolean isAddOnlyNewestRates) {
        exchangeRateService.importExchangeRatesFormCBR(scenario_id, isAddOnlyNewestRates);
        return new ResponseEntity(HttpStatus.OK);
    }

    @PutMapping
    @ApiOperation(value = "Изменение значений курса валют", response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Курс валют был изменен."),
            @ApiResponse(code = 403, message = "Доступ запрещён")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).EXCHANGE_RATE_EDITOR)")
    public ResponseEntity update(@RequestBody ExchangeRateDTO_in exchangeRateDTO_in) {
        log.info("(update): Поступил объект exchangeRateDTO_in = {}", exchangeRateDTO_in);

        ExchangeRate exchangeRate = exchangeRateTransform.ExchangeRateDTO_in_to_ExchangeRate(exchangeRateDTO_in);

        ExchangeRateID id = exchangeRateTransform.ExchangeRateDTO_to_ExchangeRateKeyInER(exchangeRateDTO_in.getScenario(),
                exchangeRateDTO_in.getCurrency(),
                exchangeRateDTO_in.getDate());

        ExchangeRate updatedExchangeRate = exchangeRateService.updateExchangeRate(id, exchangeRate);
        return new ResponseEntity(exchangeRateTransform.ExchangeRate_to_ExchangeRateDTO_out(updatedExchangeRate), HttpStatus.OK);
    }

    @DeleteMapping
    @ApiOperation(value = "Удаление значения")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Курс валют был успешно удален"),
            @ApiResponse(code = 403, message = "Доступ запрещён"),
            @ApiResponse(code = 404, message = "Курс валют не был обнаружен")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).EXCHANGE_RATE_DELETER)")
    public void delete(@RequestBody ExchangeRateDTO_in exchangeRateDTO_in) {
        ExchangeRateID id = exchangeRateTransform.ExchangeRateDTO_to_ExchangeRateKeyInER(exchangeRateDTO_in.getScenario(),
                exchangeRateDTO_in.getCurrency(),
                exchangeRateDTO_in.getDate());

        exchangeRateService.delete(id);
    }
}