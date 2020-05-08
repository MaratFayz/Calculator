package LD.config;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateFormat
{
	public static ZonedDateTime parsingDate(String dateTime)
	{
		return ZonedDateTime.parse(dateTime); //format 2020-04-24T23:56:19+00:00[UTC]
	}

	public static String formatDate(ZonedDateTime dateTime)
	{
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		dateTime.format(dtf);

		return dateTime.format(dtf);
	}
}
