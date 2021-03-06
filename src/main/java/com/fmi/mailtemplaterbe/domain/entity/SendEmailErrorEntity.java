package com.fmi.mailtemplaterbe.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "send_email_errors")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendEmailErrorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "sender_email", nullable = false)
    private String senderEmail;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "error", nullable = false)
    private String error;

    @Column(name = "category", nullable = false)
    private Long category;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
