package com.paymentplatform.userservice.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEntity, Long> {

    @Query(value = "SELECT * FROM outbox WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT 100", nativeQuery = true)
    List<OutboxEntity> findPendingMessages();
}
