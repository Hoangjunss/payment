package com.paymentplatform.userservice.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeadLetterOutboxRepository extends JpaRepository<DeadLetterOutboxEntity, Long> {
}
