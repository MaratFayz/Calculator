package Utils;

import LD.model.Entry.Entry;
import LD.model.Entry.EntryID;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.List;

@Log4j2
public class EntryComparator {

    private EntryComparator() {
    }

    public static void compare(@NonNull Entry expected, @NonNull Entry actual, int entryNumberScale) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Class entryClass = Entry.class;
        Field[] declaredFields = entryClass.getDeclaredFields();
        String period = expected.getEntryID().getPeriod().getDate().toLocalDate().toString();

        for (Field f : declaredFields) {
            String fieldName = f.getName();

            if (!fieldName.equals("lastChange")) {
                String getMethodForField = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

                Method getMethod = entryClass.getMethod(getMethodForField, null);
                Object expectedValue = getMethod.invoke(expected, null);
                Object actualValue = getMethod.invoke(actual, null);

                if (expectedValue != null && actualValue != null) {
                    if (expectedValue instanceof BigDecimal && actualValue instanceof BigDecimal) {
                        if ((((BigDecimal) expectedValue).setScale(entryNumberScale, RoundingMode.HALF_UP)).compareTo(((BigDecimal) actualValue).setScale(entryNumberScale, RoundingMode.HALF_UP)) != 0) {
                            throwException(expectedValue, actualValue, fieldName, period);
                        }
                    } else if (expectedValue instanceof EntryID && actualValue instanceof EntryID) {
                        for (Field fe : EntryID.class.getDeclaredFields()) {
                            String fieldeName = fe.getName();

                            if (!fieldeName.equals("CALCULATION_TIME") && !fieldeName.equals("serialVersionUID")) {
                                String getMethodForeField = "get" + fieldeName.substring(0, 1).toUpperCase() + fieldeName.substring(1);

                                Method geteMethod = EntryID.class.getMethod(getMethodForeField, null);
                                Object expectedeValue = geteMethod.invoke(expectedValue, null);
                                Object actualeValue = geteMethod.invoke(actualValue, null);

                                if (!expectedeValue.equals(actualeValue)) {
                                    throwException(expectedeValue, actualeValue, fieldeName, period);
                                }
                            }
                        }
                    } else {
                        if (!expectedValue.equals(actualValue)) {
                            throwException(expectedValue, actualValue, fieldName, period);
                        }
                    }
                } else if (expectedValue != null || actualValue != null) {
                    throwException(expectedValue, actualValue, fieldName, period);
                }
            }
        }
    }

    private static void throwException(Object expectedValue, Object actualValue, String fieldName, String period) {
        String s = "Expected value " + expectedValue + " for field \"" + fieldName + "\" is not equal to actual value " + actualValue + " for period " + period;
        throw new IllegalStateException(s);
    }

    public static void compare(@NonNull List<Entry> entries_expected, @NonNull List<Entry> calculatedEntries, int entryNumberScale) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (entries_expected.size() != calculatedEntries.size()) {
            throw new IllegalStateException("Sizes of expected Entries " + entries_expected.size() + " and Calculated Entries " + calculatedEntries.size() + " are not equal!");
        }

        for (Entry exEntry : entries_expected) {
            EntryID exEntryId = exEntry.getEntryID();

            boolean isFoundExpectedEntryInCaclulatedEntry = false;
            for (Entry calcEntry : calculatedEntries) {
                EntryID calcEntryId = calcEntry.getEntryID();
                ZonedDateTime calcEntryIdCalcTime = calcEntryId.getCALCULATION_TIME();
                calcEntryId.setCALCULATION_TIME(exEntryId.getCALCULATION_TIME());

                if (exEntryId.equals(calcEntryId)) {
                    EntryComparator.compare(exEntry, calcEntry, entryNumberScale);
                    isFoundExpectedEntryInCaclulatedEntry = true;
                }

                calcEntryId.setCALCULATION_TIME(calcEntryIdCalcTime);
            }

            if (isFoundExpectedEntryInCaclulatedEntry == false) {
                throw new IllegalStateException("There is no expected Entry in calculated Entries!");
            }
        }
    }

    public static void compare(@NonNull Entry expected, @NonNull Entry actual) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        compare(expected, actual, 100);
    }

    public static void compare(@NonNull List<Entry> entries_expected, @NonNull List<Entry> calculatedEntries) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        compare(entries_expected, calculatedEntries, 100);
    }
}
