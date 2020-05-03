package LD.model.Period;

import LD.config.DataParsing;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Data
public class PeriodDTO
{
	private String date;

	public static Period PeriodDTO_to_Period(PeriodDTO periodDTO)
	{
		return Period.builder()
				.date(DataParsing.parsingDate(periodDTO.getDate())) //format yyyy-mm-dd
				.build();
	}
}
