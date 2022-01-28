package com.fmi.mailtemplaterbe.domain.resource;

import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SendEmailResource extends EmailTemplateResource {

    @NotNull(message = "Recipients must not be null")
    private List<RecipientResource> recipients;

    @NotNull(message = "isHtml must not be null")
    private Boolean isHtml;
}