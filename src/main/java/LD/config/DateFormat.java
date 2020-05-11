package LD.config;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateFormat
{
	static final String dateformat = "dd-MM-yyyy";

	public static ZonedDateTime parsingDate(String dateTime)
	{
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateformat);
		LocalDate localDate = LocalDate.parse(dateTime, dateTimeFormatter);
		return ZonedDateTime.of(localDate, LocalTime.MIDNIGHT, ZoneId.of("UTC"));
	}

	public static String formatDate(ZonedDateTime dateTime)
	{
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateformat);
		dateTime.format(dtf);

		return dateTime.format(dtf);
	}
}
