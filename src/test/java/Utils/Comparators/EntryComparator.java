package Utils;

import LD.model.Entry.Entry;
import LD.model.Entry.EntryID;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;

@Log4j2
public class EntryComparator {

    public static void compare(Entry expected, Entry actual) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Class entryClass = Entry.class;
        Field[] declaredFields = entryClass.getDeclaredFields();

        for (Field f : declaredFields) {
            String fieldName = f.getName();

            if (!fieldName.equals("lastChange")) {
                String getMethodForField = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

                Method getMethod = entryClass.getMethod(getMethodForField, null);
                Object expectedValue = getMethod.invoke(expected, null);
                Object actualValue = getMethod.invoke(actual, null);

                if (expectedValue != null && actualValue != null) {
                    if (expectedValue instanceof BigDecimal && actualValue instanceof BigDecimal) {
                        if (((BigDecimal) expectedValue).compareTo((BigDecimal) actualValue) != 0) {
                            throwException(expectedValue, actualValue, fieldName);
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
                                    throwException(expectedeValue, actualeValue, fieldeName);
                                }
                            }
                        }
                    } else {
                        if (!expectedValue.equals(actualValue)) {
                            throwException(expectedValue, actualValue, fieldName);
                        }
                    }
                } else if (expectedValue != null || actualValue != null) {
                    throwException(expectedValue, actualValue, fieldName);
                }
            }
        }
    }

    static void throwException(Object expectedValue, Object actualValue, String fieldName) {
        String s = "Expected value " + expectedValue + " for field \"" + fieldName + "\" is not equal to actual value " + actualValue;
        throw new IllegalStateException(s);
    }
}
