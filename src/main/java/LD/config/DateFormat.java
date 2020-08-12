package LD.config;

import lombok.extern.log4j.Log4j2;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Log4j2
public class DateFormat {

    static final String[] inputDateFormats = {"dd-MM-yyyy",
            "dd.MM.yyyy", "yyyy-MM-dd", "dd/MM/yyyy"};

    static final String outputDateFormat = "dd-MM-yyyy";

    public static ZonedDateTime parsingDate(String dateToParse) {
        ZonedDateTime parsedDate = null;

        for (String df : inputDateFormats) {
            try {
                parsedDate = parsingDate(dateToParse, df);
				if (parsedDate != null) {
					break;
				}
            }
            catch (java.time.format.DateTimeParseException e) {
                continue;
            }
        }

        return parsedDate;
    }

    public static String formatDate(ZonedDateTime dateTime) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(outputDateFormat);
        dateTime.format(dtf);

        return dateTime.format(dtf);
    }

    private static ZonedDateTime parsingDate(String dateTime, String dateformat) {
        ZonedDateTime parsedDate = null;

        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateformat);
            LocalDate localDate = LocalDate.parse(dateTime, dateTimeFormatter);
            parsedDate = ZonedDateTime.of(localDate, LocalTime.MIDNIGHT, ZoneId.of("UTC"));
        }
        catch (java.time.format.DateTimeParseException e) {

        }

        return parsedDate;
    }
}
