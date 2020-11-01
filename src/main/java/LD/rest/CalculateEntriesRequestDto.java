package LD.rest;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CalculateEntriesRequestDto {

    Long scenarioFrom;
    Long scenarioTo;
    LocalDate dateCopyStart;
}
