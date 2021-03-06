package LD.service;

import LD.config.UserSource;
import LD.model.AbstractModelClass_;
import LD.model.Duration.Duration;
import LD.model.Duration.DurationDTO_out;
import LD.repository.DurationRepository;
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
public class DurationServiceImpl implements DurationService {

    @Autowired
    DurationRepository durationRepository;
    @Autowired
    UserSource userSource;

    @Override
    public List<DurationDTO_out> getAllDurations() {
        List<Duration> resultFormDB = durationRepository.findAll();
        List<DurationDTO_out> resultFormDB_out = new ArrayList<>();

        if (resultFormDB.size() == 0) {
            resultFormDB_out.add(new DurationDTO_out());
        } else {
            resultFormDB_out = resultFormDB.stream()
                    .map(c -> DurationDTO_out.Duration_to_DurationDTO_out(c))
                    .collect(Collectors.toList());
        }

        return resultFormDB_out;
    }

    @Override
    public Duration getDuration(Long id) {
        return durationRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    @Override
    public Duration saveNewDuration(Duration duration) {
        duration.setUserLastChanged(userSource.getAuthenticatedUser());
        duration.setLastChange(ZonedDateTime.now());

        log.info("Длительность для сохранения = {}", duration);

        return durationRepository.save(duration);
    }

    @Override
    public Duration updateDuration(Long id, Duration duration) {
        duration.setId(id);

        Duration durationToUpdate = getDuration(id);

        BeanUtils.copyProperties(duration, durationToUpdate, AbstractModelClass_.LAST_CHANGE, AbstractModelClass_.USER_LAST_CHANGED);

        durationRepository.saveAndFlush(durationToUpdate);

        return durationToUpdate;
    }

    @Override
    public void delete(Long id) {
        durationRepository.deleteById(id);
    }
}