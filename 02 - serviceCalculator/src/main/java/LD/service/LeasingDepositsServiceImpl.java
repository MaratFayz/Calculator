package LD.service;

import LD.config.UserSource;
import LD.model.AbstractModelClass_;
import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.LeasingDeposit.LeasingDepositDTO_out;
import LD.model.LeasingDeposit.LeasingDepositDTO_out_onPeriodFor2Scenarios;
import LD.model.LeasingDeposit.LeasingDepositTransform;
import LD.repository.LeasingDepositRepository;
import LD.rest.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
public class LeasingDepositsServiceImpl implements LeasingDepositService {

    @Autowired
    LeasingDepositRepository leasingDepositRepository;
    @Autowired
    LeasingDepositTransform leasingDepositTransform;
    @Autowired
    UserSource userSource;

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
        return leasingDepositRepository.getActualDepositsWithEndDatesForScenarios(scenarioFromId, scenarioToId);
    }

    @Override
    public LeasingDeposit getLeasingDeposit(Long id) {
        return leasingDepositRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    @Override
    public LeasingDeposit saveNewLeasingDeposit(LeasingDeposit leasingDeposit) {
        leasingDeposit.setUserLastChanged(userSource.getAuthenticatedUser());
        leasingDeposit.setLastChange(ZonedDateTime.now());

        log.info("Лизинговый депозит для сохранения = {}", leasingDeposit);

        return leasingDepositRepository.save(leasingDeposit);
    }

    @Override
    public LeasingDeposit updateLeasingDeposit(Long id, LeasingDeposit leasingDeposit) {
        leasingDeposit.setId(id);

        LeasingDeposit leasingDepositToUpdate = getLeasingDeposit(id);

        BeanUtils.copyProperties(leasingDeposit, leasingDepositToUpdate, AbstractModelClass_.LAST_CHANGE, AbstractModelClass_.USER_LAST_CHANGED);

        leasingDepositRepository.saveAndFlush(leasingDepositToUpdate);

        return leasingDepositToUpdate;
    }

    @Override
    public void delete(Long id) {
        leasingDepositRepository.deleteById(id);
    }
}