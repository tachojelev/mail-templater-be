package com.fmi.mailtemplaterbe.service;

import com.fmi.mailtemplaterbe.config.EmailTemplatesConfiguration;
import com.fmi.mailtemplaterbe.config.SmtpConfiguration;
import com.fmi.mailtemplaterbe.domain.entity.SendEmailErrorEntity;
import com.fmi.mailtemplaterbe.domain.enums.EmailErrorCategory;
import com.fmi.mailtemplaterbe.domain.resource.*;
import com.fmi.mailtemplaterbe.exception.CredentialsAuthenticationFailedException;
import com.fmi.mailtemplaterbe.util.ConfirmationTokenUtil;
import com.fmi.mailtemplaterbe.util.EmailMessageUtil;
import com.fmi.mailtemplaterbe.util.ExceptionsUtil;
import com.fmi.mailtemplaterbe.util.SentEmailsLocalDateTimeComparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailManagerService {

    private final EmailTemplatesConfiguration emailTemplatesConfiguration;
    private final EmailTemplateService emailTemplateService;
    private final EmailHistoryService emailHistoryService;
    private final SmtpService smtpService;
    private final EmailMessageUtil emailMessageUtil;

    /**
     * Get the default smtp server that is being used for sending emails.
     *
     * @return default smtp server
     */
    public SmtpServerResource getDefaultSmtpServer() {
        SmtpConfiguration.SmtpServer smtpServer = smtpService.getDefaultSmtpServer();

        return SmtpServerResource.builder()
                .host(smtpServer.getHost())
                .name(smtpServer.getName())
                .build();
    }

    /**
     * Get all smtp servers from the configuration.
     *
     * @return smtp servers
     */
    public List<SmtpServerResource> getAllSmtpServers() {
        return smtpService.getAllSmtpServer().stream().map(
                smtpServer ->
                        SmtpServerResource.builder()
                                .host(smtpServer.getHost())
                                .name(smtpServer.getName())
                                .build())
                .collect(Collectors.toList());
    }

    /**
     * Sends an email to multiple recipients based on the same email template and a different implementation
     * of the placeholders for that email template for each recipient.
     *
     * @param sendEmailResource SendEmailResource
     * @return number of successfully sent emails
     */
    public int sendEmails(SendEmailResource sendEmailResource) {
        return sendEmailToRecipients(sendEmailResource);
    }

    /**
     * Returns a list of preview emails based on the same email template and a different implementation
     * of the placeholders for that email template for each recipient.
     *
     * @param previewEmailResource PreviewEmailResource
     * @return list of preview emails
     */
    public List<RecipientEmailPreview> getPreviewEmails(PreviewEmailResource previewEmailResource) {
        return buildPreviewEmails(previewEmailResource);
    }

    /**
     * Get a list with information about the sent emails, filtered by the respective parameters.
     *
     * @param subject          Subject
     * @param senderEmail      Sender email
     * @param recipientEmail   Recipient email
     * @param sentSuccessfully Sent successfully or not
     * @param confirmation     Confirmation value
     * @param startDate        Start date
     * @param endDate          End date
     * @return list with information about the sent emails
     */
    public List<SentEmailResource> getSentEmails(
            String subject,
            String senderEmail,
            String recipientEmail,
            Boolean sentSuccessfully,
            Long confirmation,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        final List<SentEmailResource> sentEmails = emailHistoryService.getSentEmails(
                subject, senderEmail, recipientEmail, sentSuccessfully, confirmation, startDate, endDate);

        Collections.sort(sentEmails, new SentEmailsLocalDateTimeComparator());

        return sentEmails;
    }

    private int sendEmailToRecipients(SendEmailResource sendEmailResource) {
        validateEmailTemplateId(sendEmailResource.getId());
        /* If credentials are provided, we need to validate them first. */
        validateSmtpServerIfNecessary(sendEmailResource.getCredentials());

        int errorsCount = 0;

        for (Recipient recipient : sendEmailResource.getRecipients()) {
            try {
                final String emailSubject = sendEmailResource.getTitle();
                final String emailMessage =
                        emailMessageUtil.buildEmailMessage(
                                sendEmailResource.getMessage(),
                                recipient.getPlaceholders(),
                                emailTemplatesConfiguration.getPlaceholderPrefix(),
                                emailTemplatesConfiguration.getPlaceholderSuffix());
                final boolean isHtmlMessage = sendEmailResource.getIsHtml();

                sendEmailToRecipient(
                        sendEmailResource.getId(),
                        sendEmailResource.getCredentials(),
                        recipient.getEmail(),
                        emailSubject,
                        emailMessage,
                        isHtmlMessage,
                        sendEmailResource.getIncludeConfirmationLink());
            } catch (CredentialsAuthenticationFailedException e) {
                /*
                 * If we encounter an authentication failed exception, we do not need to attempt sending an email
                 * to all recipients. It is clear they will all fail with that error.
                 * Instead of that, we break the flow.
                 */
                throw e;
            } catch (Exception e) {
                errorsCount++;
            }
        }

        return sendEmailResource.getRecipients().size() - errorsCount;
    }

    private void sendEmailToRecipient(
            Long emailTemplateId,
            CredentialsResource credentials,
            String to,
            String subject,
            String content,
            boolean isHtml,
            boolean includeConfirmationLink) {
        final String confirmationToken = ConfirmationTokenUtil.generateToken();
        content = includeConfirmationLink
                    ? emailMessageUtil.appendConfirmationAppLink(subject, content, to, confirmationToken, isHtml)
                    : content;
        String from = null;
        Session session = null;

        try {
            /*
             * Optional credentials and smtp server.
             * If they are not provided, the default credentials (config vars) and default smtp server will be used.
             */
            if (areCredentialsProvided(credentials)) {
                from = credentials.getUsername();
                session = smtpService.createSMTPSession(
                        credentials.getUsername(), credentials.getPassword(), credentials.getSmtpServerName());
            } else {
                from = smtpService.getUsername();
                session = smtpService.createSMTPSession();
            }

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject, "UTF-8");

            if (isHtml) {
                message.setContent(content, "text/html;charset=UTF-8");
            } else {
                message.setText(content, "UTF-8");
            }

            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            final SendEmailErrorEntity sendEmailErrorEntity =
                    emailHistoryService.persistSendEmailError(from, to, subject, content, e.getMessage(), EmailErrorCategory.MESSAGING);
            final Long emailErrorId = sendEmailErrorEntity.getId();

            emailHistoryService.persistSentEmail(
                    emailTemplateId, from, to, subject, content, false, confirmationToken, emailErrorId);

            if (e instanceof AuthenticationFailedException) {
                throw ExceptionsUtil.getCredentialsAuthenticationFailedException(e.getMessage());
            }

            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            e.printStackTrace();
            final SendEmailErrorEntity sendEmailErrorEntity =
                    emailHistoryService.persistSendEmailError(from, to, subject, content, e.getMessage(), EmailErrorCategory.RUNTIME);
            final Long emailErrorId = sendEmailErrorEntity.getId();

            emailHistoryService.persistSentEmail(
                    emailTemplateId, from, to, subject, content, false, confirmationToken, emailErrorId);

            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            final SendEmailErrorEntity sendEmailErrorEntity =
                    emailHistoryService.persistSendEmailError(from, to, subject, content, e.getMessage(), EmailErrorCategory.UNKNOWN);
            final Long emailErrorId = sendEmailErrorEntity.getId();

            emailHistoryService.persistSentEmail(
                    emailTemplateId, from, to, subject, content, false, confirmationToken, emailErrorId);

            throw new RuntimeException(e);
        }

        emailHistoryService.persistSentEmail(
                emailTemplateId, from, to, subject, content, true, confirmationToken, null);
    }

    private List<RecipientEmailPreview> buildPreviewEmails(PreviewEmailResource previewEmailResource) {
        List<RecipientEmailPreview> recipientEmailPreviews = new ArrayList<>();

        for (Recipient recipient : previewEmailResource.getRecipients()) {
            RecipientEmailPreview recipientEmailPreview = RecipientEmailPreview.builder()
                    .email(recipient.getEmail())
                    .subject(previewEmailResource.getTitle())
                    .message(
                            emailMessageUtil.buildEmailMessage(
                                    previewEmailResource.getMessage(),
                                    recipient.getPlaceholders(),
                                    emailTemplatesConfiguration.getPlaceholderPrefix(),
                                    emailTemplatesConfiguration.getPlaceholderSuffix()))
                    .build();

            recipientEmailPreviews.add(recipientEmailPreview);
        }

        return recipientEmailPreviews;
    }

    private boolean areCredentialsProvided(CredentialsResource credentialsResource) {
        return credentialsResource != null &&
               credentialsResource.getUsername() != null &&
               credentialsResource.getPassword() != null &&
               credentialsResource.getSmtpServerName() != null;
    }

    private void validateEmailTemplateId(Long id) {
        if (id == null) {
            throw ExceptionsUtil.getCustomBadRequestException(
                    "Missing value for field: id. Please provide id of the email template.");
        }

        if (!emailTemplateService.emailTemplateExistsById(id)) {
            throw ExceptionsUtil.getCustomBadRequestException(
                    "Email template with id: " + id + " does not exist. Please provide id of an existing template.");
        }
    }

    private void validateSmtpServerIfNecessary(CredentialsResource credentialsResource) {
        if (areCredentialsProvided(credentialsResource) &&
            !smtpService.smtpServerByNameExists(credentialsResource.getSmtpServerName())) {
            throw ExceptionsUtil.getCustomBadRequestException(
                    "Smtp server with name " + credentialsResource.getSmtpServerName() + " was not found.");
        }
    }
}
