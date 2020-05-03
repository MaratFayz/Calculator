package LD.config;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DataParsing
{
	public static ZonedDateTime parsingDate(String dateTime)
	{
		return ZonedDateTime.parse(dateTime); //format 2020-04-24T23:56:19+00:00[UTC]
	}
}
