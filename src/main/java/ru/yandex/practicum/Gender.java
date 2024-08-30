package ru.yandex.practicum;

import lombok.Getter;

import java.util.Set;

@Getter
public enum Gender {

    MALE("m", "ый"),
    FEMALE("w", "ая");

    private final String shortName;
    private final String postfix;

    Gender(String shortName, String postfix) {
        this.shortName = shortName;
        this.postfix = postfix;
    }

    public static Gender getByShortName(String shortName) {
        if (shortName == null || !Set.of("w", "m").contains(shortName)) {
            return null;
        }

        return shortName.equals("m") ? MALE : FEMALE;
    }

}
