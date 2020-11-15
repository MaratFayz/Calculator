package LD.rest;

import LD.model.EntryIFRSAcc.*;
import LD.service.EntryIFRSAccService;
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
@Api(value = "Контроллер для записей на счетах МСФО")
@Log4j2
@RequestMapping("/entriesIFRS")
public class EntryIFRSAccController {

    @Autowired
    EntryIFRSAccService entryIFRSAccService;
    @Autowired
    EntryIFRSAccTransform entryIFRSAccTransform;

    @GetMapping
    @ApiOperation(value = "Получение всех записей на счетах МСФО", response = ResponseEntity.class)
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ENTRY_IFRS_READER)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Все компании возвращаются в ответе."),
            @ApiResponse(code = 403, message = "Доступ запрещён")
    })
    public List<EntryIFRSAccDTO_out> getAllEntryIFRSAccs() {
        return entryIFRSAccService.getAllEntriesIFRSAcc();
    }

    @GetMapping("/forDate")
    @ApiOperation(value = "Получение всех записей на счетах МСФО на определённую дату по определённому сценарию", response = ResponseEntity.class)
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ENTRY_IFRS_READER)")
    public List<EntryIFRSAccDTO_out_form> getAllEntryIFRSAccs_for2Scenarios(@RequestParam @NonNull Long scenarioFromId,
                                                                            @RequestParam @NonNull Long scenarioToId) {
        return entryIFRSAccService.getAllEntriesIFRSAcc_for2Scenarios(scenarioToId);
    }

    @GetMapping("{leasingDeposit_id}/{scenario_id}/{period_id}/{CALCULATION_TIME}/{ifrsAcc_id}")
    @ApiOperation(value = "Получение записи на счетах МСФО с определённым id", response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Запись на счетах МСФО существует, возвращается в ответе."),
            @ApiResponse(code = 403, message = "Доступ запрещён"),
            @ApiResponse(code = 404, message = "Такая запись на счетах МСФО отсутствует")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ENTRY_IFRS_READER)")
    public ResponseEntity getEntryIFRSAcc(@PathVariable Long leasingDeposit_id,
                                          @PathVariable Long scenario_id,
                                          @PathVariable Long period_id,
                                          @PathVariable String CALCULATION_TIME,
                                          @PathVariable Long ifrsAcc_id) {
        EntryIFRSAccID id = entryIFRSAccTransform.getEntryIFRSAccID(scenario_id, leasingDeposit_id, period_id, CALCULATION_TIME, ifrsAcc_id);
        EntryIFRSAcc entryIFRSAcc = entryIFRSAccService.getEntryIFRSAcc(id);
        log.info("(getEntryIFRSAcc): entryIFRSAcc was taken: " + entryIFRSAcc);
        return new ResponseEntity(entryIFRSAccTransform.EntryIFRSAcc_to_EntryIFRSAcc_DTO_out(entryIFRSAcc), HttpStatus.OK);
    }

    @PostMapping
    @ApiOperation(value = "Сохранение новой записи на счетах МСФО", response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Новая запись на счетах МСФО была сохранена."),
            @ApiResponse(code = 403, message = "Доступ запрещён")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ENTRY_IFRS_ADDER)")
    public ResponseEntity saveNewEntryIFRSAcc(@RequestBody EntryIFRSAccDTO_in entryIFRSAccDTO_in) {
        EntryIFRSAcc entryIFRSAcc = entryIFRSAccTransform.EntryIFRSAccDTO_in_to_EntryIFRSAcc(entryIFRSAccDTO_in);
        EntryIFRSAcc newEntryIFRSAcc = entryIFRSAccService.saveNewEntryIFRSAcc(entryIFRSAcc);
        return new ResponseEntity(entryIFRSAccTransform.EntryIFRSAcc_to_EntryIFRSAcc_DTO_out(newEntryIFRSAcc), HttpStatus.OK);
    }

    @PutMapping
    @ApiOperation(value = "Изменение записи на счетах МСФО", response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Запись на счетах МСФО была изменена."),
            @ApiResponse(code = 403, message = "Доступ запрещён")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ENTRY_IFRS_EDITOR)")
    public ResponseEntity update(@RequestBody EntryIFRSAccDTO_in entryIFRSAccDTO_in) {
        log.info("(update): Поступил объект entryIFRSAccDTO_in = {}", entryIFRSAccDTO_in);

        EntryIFRSAcc entryIFRSAcc = entryIFRSAccTransform.EntryIFRSAccDTO_in_to_EntryIFRSAcc(entryIFRSAccDTO_in);

        EntryIFRSAccID id = entryIFRSAccTransform.getEntryIFRSAccID(entryIFRSAccDTO_in.getScenario(),
                entryIFRSAccDTO_in.getLeasingDeposit(),
                entryIFRSAccDTO_in.getPeriod(),
                entryIFRSAccDTO_in.getCALCULATION_TIME(),
                entryIFRSAccDTO_in.getIfrsAccount());

        EntryIFRSAcc updatedEntryIFRSAcc = entryIFRSAccService.updateEntryIFRSAcc(id, entryIFRSAcc);
        return new ResponseEntity(entryIFRSAccTransform.EntryIFRSAcc_to_EntryIFRSAcc_DTO_out(updatedEntryIFRSAcc), HttpStatus.OK);
    }

    @DeleteMapping
    @ApiOperation(value = "Удаление значения")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Запись на счетах МСФО была успешно удалена"),
            @ApiResponse(code = 403, message = "Доступ запрещён"),
            @ApiResponse(code = 404, message = "Запись на счетах МСФО не была обнаружена")
    })
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ENTRY_IFRS_DELETER)")
    public void delete(@RequestBody EntryIFRSAccDTO_in entryIFRSAccDTO_in) {
        EntryIFRSAccID id = entryIFRSAccTransform.getEntryIFRSAccID(entryIFRSAccDTO_in.getScenario(),
                entryIFRSAccDTO_in.getLeasingDeposit(),
                entryIFRSAccDTO_in.getPeriod(),
                entryIFRSAccDTO_in.getCALCULATION_TIME(),
                entryIFRSAccDTO_in.getIfrsAccount());

        entryIFRSAccService.delete(id);
    }
}