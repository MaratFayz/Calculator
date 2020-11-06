package LD.service;

import LD.config.UserSource;
import LD.model.AbstractModelClass_;
import LD.model.Counterpartner.Counterpartner;
import LD.model.Counterpartner.Counterpartner_out;
import LD.repository.CounterpartnerRepository;
import LD.rest.exceptions.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CounterpartnerServiceImpl implements CounterpartnerService {

    @Autowired
    CounterpartnerRepository counterpartnerRepository;
    @Autowired
    UserSource userSource;

    @Override
    public Counterpartner getCounterpartner(Long id) {
        return counterpartnerRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    @Override
    public Counterpartner saveNewCounterpartner(Counterpartner counterpartner) {
        counterpartner.setUserLastChanged(userSource.getAuthenticatedUser());

        counterpartner.setLastChange(ZonedDateTime.now());

        return counterpartnerRepository.save(counterpartner);
    }

    @Override
    public Counterpartner updateCounterpartner(Long id, Counterpartner counterpartner) {
        counterpartner.setId(id);

        Counterpartner counterpartnerToUpdate = getCounterpartner(id);

        BeanUtils.copyProperties(counterpartner, counterpartnerToUpdate, AbstractModelClass_.LAST_CHANGE, AbstractModelClass_.USER_LAST_CHANGED);

        counterpartnerRepository.saveAndFlush(counterpartnerToUpdate);

        return counterpartnerToUpdate;
    }

    @Override
    public void delete(Long id) {
        counterpartnerRepository.deleteById(id);
    }

    @Override
    public List<Counterpartner_out> getAllCounterpartners() {
        List<Counterpartner> resultFormDB = counterpartnerRepository.findAll();
        List<Counterpartner_out> resultFormDB_out = new ArrayList<>();

        if (resultFormDB.size() == 0) {
            resultFormDB_out.add(new Counterpartner_out());
        } else {
            resultFormDB_out = resultFormDB.stream()
                    .map(c -> Counterpartner_out.Counterpartner_to_CounterpartnerDTO_out(c))
                    .collect(Collectors.toList());
        }

        return resultFormDB_out;
    }
}