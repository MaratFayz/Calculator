package Utils.Comparators;

import LD.model.EntryIFRSAcc.EntryIFRSAcc;
import LD.model.EntryIFRSAcc.EntryIFRSAccID;
import lombok.NonNull;
import org.junit.platform.commons.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static java.util.Objects.*;

public class EntryIfrsAccountsComparator {

    private static Class entryIfrsAccClass = EntryIFRSAcc.class;
    private static Field[] entryIfrsAccClassfields = entryIfrsAccClass.getDeclaredFields();

    private EntryIfrsAccountsComparator() {
    }

    public static void compareExceptedAndCalculatedEntries(@NonNull List<EntryIFRSAcc> expectedEntries, @NonNull List<EntryIFRSAcc> calculatedEntries) throws IllegalAccessException {
        throwExceptionWhenSizesDiffer(expectedEntries, calculatedEntries);

        for (EntryIFRSAcc expectedEntry : expectedEntries) {
            EntryIFRSAcc foundCalculatedEntry = findEntryInEntryListByEntryIdOrThrowException(calculatedEntries, expectedEntry.getEntryIFRSAccID());
            compareExceptedAndCalculatedEntries(expectedEntry, foundCalculatedEntry);
        }
    }

    public static void compareExceptedAndCalculatedEntries(@NonNull EntryIFRSAcc expected, @NonNull EntryIFRSAcc calculated) throws IllegalAccessException {
        for (Field field : entryIfrsAccClassfields) {
            String fieldName = field.getName();

            if (isFieldNameIsLastChange(fieldName)) {
                continue;
            }

            Object expectedValue = getFieldValueFromObject(field, expected);
            Object calculatedValue = getFieldValueFromObject(field, calculated);

            if (isBothNotNull(expectedValue, calculatedValue)) {
                compareValues(fieldName, expectedValue, calculatedValue);
            } else if (isAnyNotNull(expectedValue, calculatedValue)) {
                throwException(expectedValue, calculatedValue, fieldName);
            }
        }
    }

    private static void throwExceptionWhenSizesDiffer(List<EntryIFRSAcc> expectedEntries, List<EntryIFRSAcc> calculatedEntries) {
        if (expectedEntries.size() != calculatedEntries.size()) {
            throw new IllegalStateException("Sizes of expectedEntries and calculatedEntries are not equal!");
        }
    }

    private static EntryIFRSAcc findEntryInEntryListByEntryIdOrThrowException(List<EntryIFRSAcc> entries, EntryIFRSAccID entryId) {
        EntryIFRSAcc result = null;

        for (EntryIFRSAcc entry : entries) {
            if (entry.getEntryIFRSAccID().equals(entryId)) {
                result = entry;
            }
        }

        requireNonNull(result, "There is no entry in calculated entries similar to expected one for account_id = " + entryId.getIfrsAccount().getId());

        return result;
    }

    private static boolean isFieldNameIsLastChange(String fieldName) {
        return fieldName.equals("lastChange");
    }

    private static Object getFieldValueFromObject(Field field, EntryIFRSAcc expected) throws IllegalAccessException {
        ReflectionUtils.makeAccessible(field);
        return field.get(expected);
    }

    private static boolean isBothNotNull(Object expectedValue, Object actualValue) {
        return nonNull(expectedValue) && nonNull(actualValue);
    }

    private static void compareValues(String fieldName, Object expectedValue, Object calculatedValue) {
        if (isBothBigDecimal(expectedValue, calculatedValue)) {
            if (isBigDecimalValuesDiffer(expectedValue, calculatedValue)) {
                throwException(expectedValue, calculatedValue, fieldName);
            }
        } else {
            if (!expectedValue.equals(calculatedValue)) {
                throwException(expectedValue, calculatedValue, fieldName);
            }
        }
    }

    private static boolean isBothBigDecimal(Object expectedValue, Object calculatedValue) {
        return expectedValue instanceof BigDecimal && calculatedValue instanceof BigDecimal;
    }

    private static boolean isBigDecimalValuesDiffer(Object expectedValue, Object calculatedValue) {
        return ((BigDecimal) expectedValue).compareTo(((BigDecimal) calculatedValue)) != 0;
    }

    private static boolean isAnyNotNull(Object expectedValue, Object actualValue) {
        return nonNull(expectedValue) || nonNull(actualValue);
    }

    private static void throwException(Object expectedValue, Object actualValue, String fieldName) {
        String s = "Expected value " + expectedValue + " for field \"" + fieldName + "\" is not equal to actual value " + actualValue;
        throw new IllegalStateException(s);
    }
}
