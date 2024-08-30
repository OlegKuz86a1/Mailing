package ru.yandex.practicum;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class Test {

    public static void main(String[] args) {
        String company = "ПАО \"ММК\"".replace("\"", "");
        System.out.println("исх. %d_%s %s %s.pdf".formatted(12, LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMM")),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), company));

        //System.out.println(createRecipient("Валяс", "Вадим", null, Gender.MALE));
    }

    private static String createRecipient(String lastName, String firstName, String middleName, Gender gender) {
        return lastName != null ? switch (gender) {
            case MALE -> Set.of("ов", "ев", "ин", "цын").stream().anyMatch(lastName::endsWith) ? lastName + "y" :
                    (lastName.endsWith("ий") ? lastName.replace("ий", "ому") : lastName);
            case FEMALE -> Set.of("ова", "ева", "ина", "цына").stream().anyMatch(lastName::endsWith) ?
                    lastName.substring(0, lastName.length() - 1) + "ой" : (lastName.endsWith("ая") ?
                    lastName.substring(0, lastName.length() - 2) + "ой" : lastName);
        } + " " + firstName.charAt(0) + "." + (middleName != null ? "" + middleName.charAt(0) + "." : "") : "";
    }

}
