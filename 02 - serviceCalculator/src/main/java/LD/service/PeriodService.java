package LD.service;

import LD.model.Period.Period;
import LD.model.Period.PeriodDTO_out;

import java.time.LocalDate;
import java.util.List;

public interface PeriodService {

    List<PeriodDTO_out> getAllPeriods();

    Period getPeriod(Long id);

    Period saveNewPeriod(Period period);

    Period updatePeriod(Long id, Period period);

    void delete(Long id);

    void autoCreatePeriods(String dateFrom, String dateTo);
}