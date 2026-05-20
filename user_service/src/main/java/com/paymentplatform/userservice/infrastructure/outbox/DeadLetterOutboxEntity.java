package com.paymentplatform.userservice.infrastructure.outbox;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "dead_letter_outbox")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterOutboxEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateType;
    private String aggregateId;
    private String type;
    private String payload;
    private String topic;
    private LocalDateTime createdAt;
    private LocalDateTime failedAt;
    private String errorMessage;
}
