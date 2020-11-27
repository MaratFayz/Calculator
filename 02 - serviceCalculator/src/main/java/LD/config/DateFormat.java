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

    public static LocalDate parsingDate(String dateToParse) {
        LocalDate parsedDate = null;

        for (String df : inputDateFormats) {
            try {
                parsedDate = parsingDate(dateToParse, df);
                if (parsedDate != null) {
                    break;
                }
            } catch (java.time.format.DateTimeParseException e) {
                continue;
            }
        }

        return parsedDate;
    }

    public static ZonedDateTime parsingZonedDateTime(String dateToParse) {
        LocalDate parsedDate = parsingDate(dateToParse);
        return ZonedDateTime.of(parsedDate, LocalTime.MIDNIGHT, ZoneId.of("UTC"));
    }

    public static String formatDate(LocalDate dateTime) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(outputDateFormat);
        dateTime.format(dtf);

        return dateTime.format(dtf);
    }

    public static String formatDate(ZonedDateTime dateTime) {
        return formatDate(dateTime.toLocalDate());
    }

    private static LocalDate parsingDate(String dateTime, String dateformat) {
        LocalDate parsedDate = null;

        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateformat);
            parsedDate = LocalDate.parse(dateTime, dateTimeFormatter);
        } catch (java.time.format.DateTimeParseException e) {

        }

        return parsedDate;
    }
}
