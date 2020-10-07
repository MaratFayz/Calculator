package LD.model.Enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum STATUS_X {
    X;

    @JsonCreator
    public static STATUS_X fromString(String string) {
        if (string.equals("X") || string.equals("x") || string.equals("Х") || string.equals("х")) {
            return STATUS_X.X;
        } else {
            return null;
        }
    }
}
