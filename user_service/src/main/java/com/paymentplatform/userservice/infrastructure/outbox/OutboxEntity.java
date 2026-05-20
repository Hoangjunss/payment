package com.paymentplatform.userservice.infrastructure.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateType;
    private String aggregateId;
    private String type;

    @Column(columnDefinition = "TEXT")
    private String payload;
    private String topic;

    @Builder.Default
    private String status = "PENDING"; // PENDING, SENT, FAILED

    private LocalDateTime createdAt;
    
    private LocalDateTime processedAt;

    private int retryCount;

    @Version
    private Long version;
}
