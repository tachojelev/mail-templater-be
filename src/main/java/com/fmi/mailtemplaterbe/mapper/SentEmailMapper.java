package com.fmi.mailtemplaterbe.mapper;

import com.fmi.mailtemplaterbe.domain.entity.SentEmailEntity;
import com.fmi.mailtemplaterbe.domain.resource.SentEmailResource;

public final class SentEmailMapper {

    private SentEmailMapper() {

    }

    public static SentEmailResource entityToResource(SentEmailEntity sentEmailEntity) {
        if (sentEmailEntity == null) {
            return null;
        }

        return SentEmailResource.builder()
                .id(sentEmailEntity.getId())
                /* Mapping of emailTemplateId is unnecessary. */
                .subject(sentEmailEntity.getSubject())
                .message(sentEmailEntity.getMessage())
                .senderEmail(sentEmailEntity.getSenderEmail())
                .recipientEmail(sentEmailEntity.getRecipientEmail())
                .sentSuccessfully(sentEmailEntity.isSentSuccessfully())
                /* Mapping of sendEmailErrorId is unnecessary. */
                .timestamp(sentEmailEntity.getTimestamp())
                .confirmation(sentEmailEntity.getConfirmation())
                /* Mapping of confirmation token is unnecessary. */
                /* Mapping of errorMessage is done separately. */
                .build();
    }

    public static SentEmailEntity resourceToEntity(SentEmailResource sentEmailResource) {
        if (sentEmailResource == null) {
            return null;
        }

        return SentEmailEntity.builder()
                .id(sentEmailResource.getId())
                .subject(sentEmailResource.getSubject())
                .message(sentEmailResource.getMessage())
                .senderEmail(sentEmailResource.getSenderEmail())
                .recipientEmail(sentEmailResource.getRecipientEmail())
                .sentSuccessfully(sentEmailResource.isSentSuccessfully())
                .timestamp(sentEmailResource.getTimestamp())
                .confirmation(sentEmailResource.getConfirmation())
                /* Mapping of confirmation token is unnecessary. */
                .build();
    }
}
