package org.scrapper.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;
import org.scrapper.enums.ScrapperStrategy;

public record ScrapeRequestDto(
        @NotBlank(message = "URL nesmí být prázdné")
        @URL(message = "Musí být platná URL adresa")
        String url,

        @Min(value = 1, message = "Timeout musí být minimálně 1 sekunda")
        int timeoutSeconds,

        @NotBlank(message = "User-Agent nesmí být prázdný")
        String userAgent,

        @JsonSetter(nulls = Nulls.AS_EMPTY)
        ScrapperStrategy strategy
) {}
