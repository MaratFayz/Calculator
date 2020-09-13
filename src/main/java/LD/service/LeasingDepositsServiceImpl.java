package LD.service;

import LD.config.DateFormat;
import LD.config.Security.Repository.UserRepository;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.LeasingDeposit.LeasingDepositDTO_out;
import LD.model.LeasingDeposit.LeasingDepositDTO_out_onPeriodFor2Scenarios;
import LD.model.LeasingDeposit.LeasingDepositTransform;
import LD.model.Scenario.Scenario;
import LD.repository.LeasingDepositRepository;
import LD.repository.PeriodsClosedRepository;
import LD.repository.ScenarioRepository;
import LD.rest.exceptions.NotFoundException;
import LD.service.Calculators.LeasingDeposits.SupportEntryCalculator;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static LD.service.Calculators.LeasingDeposits.GeneralDataKeeper.specFirstClosedPeriod;

@Service
@Log4j2
public class LeasingDepositsServiceImpl implements LeasingDepositService {

    @Autowired
    LeasingDepositRepository leasingDepositRepository;
    @Autowired
    LeasingDepositTransform leasingDepositTransform;
    @Autowired
    ScenarioRepository scenarioRepository;
    @Autowired
    PeriodsClosedRepository periodsClosedRepository;
    @Autowired
    UserRepository userRepository;

    @Override
    public List<LeasingDepositDTO_out> getAllLeasingDeposits() {
        List<LeasingDeposit> resultFormDB = leasingDepositRepository.findAll();
        List<LeasingDepositDTO_out> resultFormDB_out = new ArrayList<>();

        if (resultFormDB.size() == 0) {
            resultFormDB_out.add(new LeasingDepositDTO_out());
        } else {
            resultFormDB_out = resultFormDB.stream()
                    .map(ld -> leasingDepositTransform.LeasingDeposit_to_LeasingDepositDTO_out(ld))
                    .collect(Collectors.toList());
        }

        return resultFormDB_out;
    }

    @Override
    public List<LeasingDepositDTO_out_onPeriodFor2Scenarios> getAllLeasingDepositsOnPeriodFor2Scenarios(Long scenarioFromId,
                                                                                                        Long scenarioToId) {
        final Scenario scenario_from = scenarioRepository.findById(scenarioFromId)
                .orElseThrow(() -> new NotFoundException("Значение сценария " + scenarioFromId + " отсутствует в базе данных"));

        log.info("Был получен сценарий-источник = {}", scenario_from);

        final Scenario scenario_to = scenarioRepository.findById(scenarioToId)
                .orElseThrow(() -> new NotFoundException("Значение сценария " + scenarioToId + " отсутствует в базе данных"));

        log.info("Был получен сценарий-получатель = {}", scenario_to);

        final ZonedDateTime firstOpenPeriodForScenarioTo =
                periodsClosedRepository.findAll(specFirstClosedPeriod(scenario_to)).get(0).getPeriodsClosedID()
                        .getPeriod().getDate().withZoneSameInstant(ZoneId.of("UTC"));

        log.info("Был получен первый открытый период для сценария-получателя = {}", firstOpenPeriodForScenarioTo);

        List<LeasingDepositDTO_out_onPeriodFor2Scenarios> leasingDepositFor2Scenarios = leasingDepositRepository.findAll().stream()
                .map(ld -> {
                    TreeMap<ZonedDateTime, ZonedDateTime> endDatesForLd = SupportEntryCalculator.calculateDateUntilThatEntriesMustBeCalculated(ld, scenario_to).getMappingPeriodEndDate();

                    log.info("Был сформирован treemap для дат окончания = {}", endDatesForLd);

                    ZonedDateTime endDateForFirstOpenPeriodScenarioTo = endDatesForLd.floorEntry(firstOpenPeriodForScenarioTo).getValue();

                    log.info("Была определена конечная дата для первого открытого периода сценария-получателя = {}", endDateForFirstOpenPeriodScenarioTo);

                    LeasingDepositDTO_out_onPeriodFor2Scenarios mappedld = leasingDepositTransform
                            .LeasingDeposit_to_LeasingDepositDTO_out_onPeriodFor2Scenarios(ld, endDateForFirstOpenPeriodScenarioTo);

                    log.info("Был получен out экземпляр депозита = {}", mappedld);

                    return mappedld;
                })
                .filter(ld_mapped -> {
                    ZonedDateTime parsedEndDate = DateFormat.parsingDate(ld_mapped.getEndDate());

                    log.info("Был получен parsedEndDate депозита = {}", parsedEndDate);
                    log.info("firstOpenPeriodForScenarioTo = {}", firstOpenPeriodForScenarioTo);

                    boolean isShow = parsedEndDate.isAfter(firstOpenPeriodForScenarioTo)
                            || parsedEndDate.withDayOfMonth(1).isEqual(firstOpenPeriodForScenarioTo.withDayOfMonth(1)) ? true : false;

                    log.info("parsedEndDate.isAfter(firstOpenPeriodForScenarioTo) = {}", parsedEndDate.isAfter(firstOpenPeriodForScenarioTo));
                    log.info("parsedEndDate.withDayOfMonth(1).isEqual(firstOpenPeriodForScenarioTo.withDayOfMonth(1)) = {}", parsedEndDate.withDayOfMonth(1).isEqual(firstOpenPeriodForScenarioTo.withDayOfMonth(1)));
                    log.info("isShow = {}", isShow);

                    return isShow;
                })
                .collect(Collectors.toList());

        log.info("Выводимые депозиты = {}", leasingDepositFor2Scenarios);

        return leasingDepositFor2Scenarios;
    }

    @Override
    public LeasingDeposit getLeasingDeposit(Long id) {
        return leasingDepositRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    @Override
    public LeasingDeposit saveNewLeasingDeposit(LeasingDeposit leasingDeposit) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        leasingDeposit.setUser(userRepository.findByUsername(username));

        leasingDeposit.setLastChange(ZonedDateTime.now());

        log.info("Лизинговый депозит для сохранения = {}", leasingDeposit);

        return leasingDepositRepository.save(leasingDeposit);
    }

    @Override
    public LeasingDeposit updateLeasingDeposit(Long id, LeasingDeposit leasingDeposit) {
        leasingDeposit.setId(id);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        leasingDeposit.setUser(userRepository.findByUsername(username));

        leasingDeposit.setLastChange(ZonedDateTime.now());

        LeasingDeposit leasingDepositToUpdate = getLeasingDeposit(id);

        BeanUtils.copyProperties(leasingDeposit, leasingDepositToUpdate);

        leasingDepositRepository.saveAndFlush(leasingDepositToUpdate);

        return leasingDepositToUpdate;
    }

    @Override
    public boolean delete(Long id) {
        try {
            leasingDepositRepository.deleteById(id);
        } catch (Exception e) {
            return false;
        }

        return true;
    }
}
