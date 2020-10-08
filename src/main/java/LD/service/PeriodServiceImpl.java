package LD.service;

import LD.config.DateFormat;
import LD.config.Security.Repository.UserRepository;
import LD.config.Security.model.User.User;
import LD.model.Period.Period;
import LD.model.Period.PeriodDTO_out;
import LD.repository.PeriodRepository;
import LD.rest.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
public class PeriodServiceImpl implements PeriodService {

    @Autowired
    PeriodRepository periodRepository;
    @Autowired
    UserRepository userRepository;

    @Override
    public List<PeriodDTO_out> getAllPeriods() {
        List<Period> resultFormDB = periodRepository.findAll();
        List<PeriodDTO_out> resultFormDB_out = new ArrayList<>();

        if (resultFormDB.size() == 0) {
            resultFormDB_out.add(new PeriodDTO_out());
        } else {
            resultFormDB_out = resultFormDB.stream()
                    .map(per -> PeriodDTO_out.Period_to_PeriodDTO_out(per))
                    .collect(Collectors.toList());
        }

        return resultFormDB_out;
    }

    @Override
    public Period getPeriod(Long id) {
        return periodRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    @Override
    public Period saveNewPeriod(Period period) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        period.setUserLastChanged(userRepository.findByUsername(username));

        period.setLastChange(ZonedDateTime.now());

        log.info("Период для сохранения = {}", period);

        return periodRepository.save(period);
    }

    @Override
    public Period updatePeriod(Long id, Period period) {
        period.setId(id);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        period.setUserLastChanged(userRepository.findByUsername(username));

        period.setLastChange(ZonedDateTime.now());

        Period periodToUpdate = getPeriod(id);

        BeanUtils.copyProperties(period, periodToUpdate);

        periodRepository.saveAndFlush(periodToUpdate);

        return periodToUpdate;
    }

    @Override
    public boolean delete(Long id) {
        try {
            periodRepository.deleteById(id);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    @Override
    public void autoCreatePeriods(String dateFrom, String dateTo) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User userCreated = userRepository.findByUsername(username);

        ArrayList<Period> generatedPeriods = generatePeriods(dateFrom, dateTo, userCreated, periodRepository);

        periodRepository.saveAll(generatedPeriods);
    }

    public static ArrayList<Period> generatePeriods(String dateFrom, String dateTo, User userCreated, PeriodRepository periodRepository) {
        ArrayList<Period> periods = new ArrayList<>();

        LocalDate LDdateFrom = DateFormat.parsingDate(dateFrom);
        LocalDate LDdateTo = DateFormat.parsingDate(dateTo);

        if (LDdateFrom.isAfter(LDdateTo)) {
            LocalDate now = LocalDate.now();
            now = LDdateFrom;
            LDdateFrom = LDdateTo;
            LDdateTo = now;
        }

        LDdateFrom = LDdateFrom.plusMonths(1).withDayOfMonth(1).minusDays(1);
        LDdateTo = LDdateTo.plusMonths(1).withDayOfMonth(1);

        LDdateFrom.datesUntil(LDdateTo, java.time.Period.ofMonths(1)).forEach(date ->
        {
            //доводим до последнего дня месяца
            date = date.withDayOfMonth(date.lengthOfMonth());

            Period foundPeriod = periodRepository.findByDate(date);

            log.info("Для даты {} был обнаружен период {}", date, foundPeriod);

            if (foundPeriod == null) {
                log.info("Для даты {} был НЕ обнаружен период. Значит будет создан и сохранён", date);

                Period newPeriod = Period.builder()
                        .date(date)
                        .build();

                newPeriod.setLastChange(ZonedDateTime.now());
                newPeriod.setUserLastChanged(userCreated);

                log.info("Для даты {} был создан период {}", date, newPeriod);

                periods.add(newPeriod);
            }
        });

        return periods;
    }
}
