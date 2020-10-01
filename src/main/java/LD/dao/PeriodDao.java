package LD.dao;

import LD.model.Period.Period;

import java.time.LocalDate;

public interface PeriodDao {

    Period findPeriodByDate(LocalDate date);

    LocalDate findMinPeriodDateInDatabase();

    LocalDate findMaxPeriodDateInDatabase();
}
