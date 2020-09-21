package LD.service;

import LD.config.Security.Repository.UserRepository;
import LD.model.EntryIFRSAcc.*;
import LD.model.Enums.EntryStatus;
import LD.model.Period.Period;
import LD.model.Scenario.Scenario;
import LD.repository.EntryIFRSAccRepository;
import LD.repository.IFRSAccountRepository;
import LD.repository.PeriodsClosedRepository;
import LD.repository.ScenarioRepository;
import LD.rest.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static LD.service.Calculators.LeasingDeposits.CalculationParametersSourceImpl.specFirstClosedPeriod;

@Service
@Log4j2
public class EntryIFRSAccServiceImpl implements EntryIFRSAccService {

    @Autowired
    EntryIFRSAccRepository entryIFRSAccRepository;
    @Autowired
    EntryIFRSAccTransform entryIFRSAccTransform;
    @Autowired
    ScenarioRepository scenarioRepository;
    @Autowired
    PeriodsClosedRepository periodsClosedRepository;
    @Autowired
    IFRSAccountRepository ifrsAccountRepository;
    @Autowired
    UserRepository userRepository;

    @Override
    public List<EntryIFRSAccDTO_out> getAllEntriesIFRSAcc() {
        List<EntryIFRSAcc> resultFormDB = entryIFRSAccRepository.findAll();
        List<EntryIFRSAccDTO_out> resultFormDB_out = new ArrayList<>();

        if (resultFormDB.size() == 0) {
            resultFormDB_out.add(new EntryIFRSAccDTO_out());
        } else {
            resultFormDB_out = resultFormDB.stream()
                    .map(entryIFRSAcc -> entryIFRSAccTransform.EntryIFRSAcc_to_EntryIFRSAcc_DTO_out(entryIFRSAcc))
                    .collect(Collectors.toList());
        }

        return resultFormDB_out;
    }

    @Override
    public List<EntryIFRSAccDTO_out_form> getAllEntriesIFRSAcc_for2Scenarios(Long scenarioToId) {
        final Scenario scenario_to = scenarioRepository.findById(scenarioToId)
                .orElseThrow(() -> new NotFoundException("Значение сценария " + scenarioToId + " отсутствует в базе данных"));

        log.info("Был получен сценарий-получатель = {}", scenario_to);

        final Period firstOpenPeriodForScenarioTo =
                periodsClosedRepository.findAll(specFirstClosedPeriod(scenario_to)).get(0).getPeriodsClosedID()
                        .getPeriod();

        log.info("Был получен первый открытый период для сценария-получателя = {}", firstOpenPeriodForScenarioTo);

        ArrayList<EntryIFRSAcc> notAggregateEntries = new ArrayList<>(entryIFRSAccRepository.findAll()
                .stream()
                .filter(eIFRS -> eIFRS.getEntryIFRSAccID().getEntry().getEntryID().getPeriod().equals(firstOpenPeriodForScenarioTo))
                .filter(eIFRS -> eIFRS.getEntryIFRSAccID().getEntry().getEntryID().getScenario().equals(scenario_to))
                .filter(eIFRS -> eIFRS.getEntryIFRSAccID().getEntry().getStatus().equals(EntryStatus.ACTUAL))
                .collect(Collectors.toList()));

        ArrayList<Long> ifrsAccs = new ArrayList<>(notAggregateEntries.stream()
                .map(entryIFRSAcc -> entryIFRSAcc.getEntryIFRSAccID().getIfrsAccount().getId())
                .collect(Collectors.toList()));

        ArrayList<EntryIFRSAccDTO_out_form> aggregatedEntries = new ArrayList<>();

        ifrsAccs.stream().forEach(acc -> {
            List<EntryIFRSAcc> ifrsEntriesForAcc = notAggregateEntries.stream()
                    .filter(entryIFRSAcc -> entryIFRSAcc.getEntryIFRSAccID().getIfrsAccount().getId() == acc)
                    .collect(Collectors.toList());

            EntryIFRSAccDTO_out_form aggregatedEntryForAcc =
                    entryIFRSAccTransform.EntryIFRSAcc_to_EntryIFRSAcc_DTO_out_form(ifrsEntriesForAcc.get(0));
            aggregatedEntryForAcc.setSum(BigDecimal.ZERO);

            for (EntryIFRSAcc entry : ifrsEntriesForAcc) {
                aggregatedEntryForAcc.setSum(aggregatedEntryForAcc.getSum().add(entry.getSum()));
            };

            aggregatedEntries.add(aggregatedEntryForAcc);
        });

        return aggregatedEntries;
    }

    @Override
    public EntryIFRSAcc getEntryIFRSAcc(EntryIFRSAccID id) {
        return entryIFRSAccRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    @Override
    public EntryIFRSAcc saveNewEntryIFRSAcc(EntryIFRSAcc entryIFRSAcc) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        entryIFRSAcc.setUser(userRepository.findByUsername(username));

        entryIFRSAcc.setLastChange(ZonedDateTime.now());

        log.info("Проводка на счетах МСФО для сохранения = {}", entryIFRSAcc);

        return entryIFRSAccRepository.saveAndFlush(entryIFRSAcc);
    }

    @Override
    public EntryIFRSAcc updateEntryIFRSAcc(EntryIFRSAccID id, EntryIFRSAcc entryIFRSAcc) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        entryIFRSAcc.setUser(userRepository.findByUsername(username));

        entryIFRSAcc.setLastChange(ZonedDateTime.now());

        EntryIFRSAcc entryIFRSAccToUpdate = getEntryIFRSAcc(id);

        BeanUtils.copyProperties(entryIFRSAcc, entryIFRSAccToUpdate);

        entryIFRSAccRepository.saveAndFlush(entryIFRSAccToUpdate);

        return entryIFRSAccToUpdate;
    }

    @Override
    public boolean delete(EntryIFRSAccID id) {
        try {
            entryIFRSAccRepository.deleteById(id);
        } catch (Exception e) {
            return false;
        }

        return true;
    }
}
