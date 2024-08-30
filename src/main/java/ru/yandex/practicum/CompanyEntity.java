package ru.yandex.practicum;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class CompanyEntity {

    private Long id;
    private String companyName;
    private String firstName;
    private String middleName;
    private String lastName;
    private Gender gender;
    private Set<String> emails;

}
