package LD.rest;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
@Builder
public class CalculateEntriesRequestDto {

    @NotNull
    @Min(value = 0L)
    Long scenarioFrom;
    @NotNull
    @Min(value = 0L)
    Long scenarioTo;
    LocalDate dateCopyStart;
}
