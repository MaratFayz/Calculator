package LD.service;

import LD.config.UserSource;
import LD.model.AbstractModelClass_;
import LD.model.EntryIFRSAcc.*;
import LD.repository.EntryIFRSAccRepository;
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
public class EntryIFRSAccServiceImpl implements EntryIFRSAccService {

    @Autowired
    EntryIFRSAccRepository entryIFRSAccRepository;
    @Autowired
    EntryIFRSAccTransform entryIFRSAccTransform;
    @Autowired
    UserSource userSource;

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
        return entryIFRSAccRepository.sumActualEntriesIfrs(scenarioToId);
    }

    @Override
    public EntryIFRSAcc getEntryIFRSAcc(EntryIFRSAccID id) {
        return entryIFRSAccRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    @Override
    public EntryIFRSAcc saveNewEntryIFRSAcc(EntryIFRSAcc entryIFRSAcc) {
        entryIFRSAcc.setUserLastChanged(userSource.getAuthenticatedUser());

        entryIFRSAcc.setLastChange(ZonedDateTime.now());

        log.info("Проводка на счетах МСФО для сохранения = {}", entryIFRSAcc);

        return entryIFRSAccRepository.saveAndFlush(entryIFRSAcc);
    }

    @Override
    public EntryIFRSAcc updateEntryIFRSAcc(EntryIFRSAccID id, EntryIFRSAcc entryIFRSAcc) {
        EntryIFRSAcc entryIFRSAccToUpdate = getEntryIFRSAcc(id);

        BeanUtils.copyProperties(entryIFRSAcc, entryIFRSAccToUpdate, AbstractModelClass_.LAST_CHANGE, AbstractModelClass_.USER_LAST_CHANGED);

        entryIFRSAccRepository.saveAndFlush(entryIFRSAccToUpdate);

        return entryIFRSAccToUpdate;
    }

    @Override
    public void delete(EntryIFRSAccID id) {
        entryIFRSAccRepository.deleteById(id);
    }
}