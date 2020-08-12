package LD.rest;

import LD.config.DateFormat;
import LD.model.Entry.*;
import LD.service.EntryService;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Api(value = "Контроллер для транзакций по лизинговым депозитам")
@RestController
@RequestMapping("/entries")
@Log4j2
public class EntryController {

    @Autowired
    EntryService entryService;
    @Autowired
    EntryTransform entryTransform;

    @GetMapping
    @ApiOperation(value = "Получение всех транзакций по лизинговым депозитам")
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ENTRY_READER)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Все транзакции возвращаются в ответе."),
            @ApiResponse(code = 404, message = "Доступ запрещён")
    })
    public List<EntryDTO_out> getAllEntries() {
        return entryService.getAllLDEntries();
    }

    @GetMapping("/regld1")
    @ApiOperation(value = "Получение всех транзакций по лизинговым депозитам для формы Reg.LD.1 для сценария-получателя")
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ENTRY_READER)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Все транзакции возвращаются в ответе."),
            @ApiResponse(code = 404, message = "Доступ запрещён")
    })
    public List<EntryDTO_out_RegLD1> getAllEntries_RegLD1(
            @RequestParam @NonNull Long scenarioFromId,
            @RequestParam @NonNull Long scenarioToId) {
        return entryService.getAllLDEntries_RegLD1(scenarioToId);
    }

    @GetMapping("/regld2")
    @ApiOperation(value = "Получение всех транзакций по лизинговым депозитам для формы Reg.LD.2 для сценария-получателя")
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ENTRY_READER)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Все транзакции возвращаются в ответе."),
            @ApiResponse(code = 404, message = "Доступ запрещён")
    })
    public List<EntryDTO_out_RegLD2> getAllEntries_RegLD2(
            @RequestParam @NonNull Long scenarioFromId,
            @RequestParam @NonNull Long scenarioToId) {
        return entryService.getAllLDEntries_RegLD2(scenarioToId);
    }

    @GetMapping("/regld3")
    @ApiOperation(value = "Получение всех транзакций по лизинговым депозитам для формы Reg.LD.3 для сценария-получателя")
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ENTRY_READER)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Все транзакции возвращаются в ответе."),
            @ApiResponse(code = 404, message = "Доступ запрещён")
    })
    public List<EntryDTO_out_RegLD3> getAllEntries_RegLD3(
            @RequestParam @NonNull Long scenarioFromId,
            @RequestParam @NonNull Long scenarioToId) {
        return entryService.getAllLDEntries_RegLD3(scenarioToId);
    }

    @GetMapping("{leasingDeposit_id}/{scenario_id}/{period_id}/{CALCULATION_TIME}")
    @ApiOperation("Получение транзакции с определённым id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Транзакция существует, возвращается в ответе."),
            @ApiResponse(code = 403, message = "Доступ запрещён"),
            @ApiResponse(code = 404, message = "Такая транзакция отсутствует")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ENTRY_READER)")
    public ResponseEntity getEntry(@PathVariable Long leasingDeposit_id,
                                   @PathVariable Long scenario_id,
                                   @PathVariable Long period_id,
                                   @PathVariable String CALCULATION_TIME) {
        EntryID id = entryTransform.getEntryID(scenario_id, leasingDeposit_id, period_id,
                CALCULATION_TIME);
        return new ResponseEntity(entryTransform.Entry_to_EntryDTO_out(entryService.getEntry(id)),
                HttpStatus.OK);
    }

    @PostMapping
    @ApiOperation(value = "Сохранение новой транзакции", response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Новая транзакция была сохранена."),
            @ApiResponse(code = 404, message = "Доступ запрещён")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ENTRY_ADDER)")
    public ResponseEntity saveNewEntry(@RequestBody EntryDTO_in entryDTO_in) {
        Entry entry = entryTransform.EntryDTO_in_to_Entry(entryDTO_in);
        Entry newEntry = entryService.saveEntry(entry);
        return new ResponseEntity(entryTransform.Entry_to_EntryDTO_out(newEntry), HttpStatus.OK);
    }

    @PostMapping("/calculator")
    @ApiOperation(value = "Расчет транзакций по лизинговым депозитам")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Расчет завершился корректно"),
            @ApiResponse(code = 500, message = "Произошла ошибка при расчете")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).CALCULATE)")
    public ResponseEntity calculateAllEntries(
            @RequestParam(name = "scenario_from") Long scenarioFrom,
            @RequestParam(name = "scenario_to") Long scenarioTo,
            @RequestParam(name = "dateCopyStart", required = false) String dateCopyStart)
            throws ExecutionException, InterruptedException {
        ZonedDateTime parsedCopyDate;

        try {
            parsedCopyDate = DateFormat.parsingDate(dateCopyStart)
                    .plusMonths(1)
                    .withDayOfMonth(1)
                    .minusDays(1);
        }
        catch (Exception e) {
            parsedCopyDate = ZonedDateTime.of(LocalDateTime.MIN, ZoneId.of("UTC"));
		}

        log.info("Дата начала копирования была запарсена в {}", parsedCopyDate);
        try {
            log.info("Расчет транзакций начался. Значения параметров: From = {}, To = {}",
                    scenarioFrom, scenarioTo);
            entryService.calculateEntries(parsedCopyDate, scenarioFrom, scenarioTo);
            log.info("Расчет транзакций окончен. Значения параметров: From = {}, To = {}",
                    scenarioFrom, scenarioTo);
        }
        catch (Exception any) {
            log.info("Возникло исключение при расчете транзакций: {}", any.toString());
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity(HttpStatus.OK);
    }

    @PutMapping
    @ApiOperation(value = "Изменение значений транзакции", response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Транзакция была изменена."),
            @ApiResponse(code = 404, message = "Доступ запрещён")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ENTRY_EDITOR)")
    public ResponseEntity update(@RequestBody EntryDTO_in entryDTO_in) {
        log.info("(update): Поступил объект entryDTO_in = {}", entryDTO_in);

        Entry entry = entryTransform.EntryDTO_in_to_Entry(entryDTO_in);

        EntryID id = entryTransform.getEntryID(entryDTO_in.getScenario(),
                entryDTO_in.getLeasingDeposit(),
                entryDTO_in.getPeriod(),
                entryDTO_in.getCALCULATION_TIME());

        Entry updatedEntry = entryService.update(id, entry);
        return new ResponseEntity(entryTransform.Entry_to_EntryDTO_out(updatedEntry),
                HttpStatus.OK);
    }

    @DeleteMapping
    @ApiOperation(value = "Удаление значения")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Транзакция была успешно удалена"),
            @ApiResponse(code = 403, message = "Доступ запрещён"),
            @ApiResponse(code = 404, message = "Транзакция не была обнаружена")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ENTRY_DELETER)")
    public ResponseEntity delete(@RequestBody EntryDTO_in entryDTO_in) {
        log.info("Поступил такой DTO = {}", entryDTO_in);
        EntryID id = entryTransform.getEntryID(entryDTO_in.getScenario(),
                entryDTO_in.getLeasingDeposit(),
                entryDTO_in.getPeriod(),
                entryDTO_in.getCALCULATION_TIME());

        log.info("id стал равен = {}", id);

        return entryService.delete(id) ? ResponseEntity.ok()
                .build() : ResponseEntity.status(404)
                .build();
    }
}
