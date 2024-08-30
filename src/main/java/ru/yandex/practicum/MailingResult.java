package ru.yandex.practicum;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString(exclude = {"content"})
public class MailingResult {

    private Long companyId;
    private boolean success;
    private String errorMessage;
    private byte[] content;
    private String email;
    private String filename;
    private Long orderNumber;

}
