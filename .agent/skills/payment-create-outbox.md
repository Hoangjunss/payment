---
name: payment-platform-outbox
description: Production-grade Outbox pattern with polling, retry, optimistic locking, and at-least-once delivery
version: 2.0
---

# Skill: Implement Outbox Pattern

## Goal
Ensure every event sent to Kafka is stored atomically with the business transaction, then delivered by a scheduler. Prevents event loss on crash.

## Step 1: Create Outbox JPA Entity
```java
@Entity
@Table(name = "outbox_messages",
       indexes = {
           @Index(name = "idx_status_created", columnList = "status, created_at"),
           @Index(name = "idx_aggregate_id", columnList = "aggregate_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String messageId;   // UUID

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private String status = "PENDING";

    private int retryCount = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Version
    private Long version;               // optimistic lock
}
```
Step 2: Outbox Repository
```java 
public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {
    @Query("SELECT o FROM OutboxMessage o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")
    List<OutboxMessage> findTop100Pending(Pageable pageable);

    @Modifying
    @Query("UPDATE OutboxMessage o SET o.status = :status, o.version = o.version + 1 WHERE o.messageId = :messageId AND o.version = :version")
    int updateStatus(@Param("messageId") String messageId, @Param("status") String status, @Param("version") Long version);
}
```
Step 3: Outbox Poller (Scheduler)
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {
    private final OutboxRepository outboxRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void poll() {
        List<OutboxMessage> messages = outboxRepo.findTop100Pending(PageRequest.of(0, 100));
        for (OutboxMessage msg : messages) {
            try {
                // Send synchronously to catch errors
                kafkaTemplate.send(msg.getTopic(), msg.getPayload()).get(5, TimeUnit.SECONDS);
                // Optimistic lock update
                int updated = outboxRepo.updateStatus(msg.getMessageId(), "SENT", msg.getVersion());
                if (updated == 0) {
                    log.warn("Outbox message {} was modified concurrently, skip", msg.getMessageId());
                }
            } catch (Exception e) {
                log.error("Failed to send outbox {}", msg.getMessageId(), e);
                msg.setRetryCount(msg.getRetryCount() + 1);
                msg.setStatus(msg.getRetryCount() >= 5 ? "FAILED" : "PENDING");
                outboxRepo.save(msg);
            }
        }
    }
}
```
Step 4: Use in Use Case (same transaction)
```java 
@Transactional
public void createPayment(PaymentCommand cmd) {
    Payment payment = new Payment(...);
    paymentRepo.save(payment);

    String payload = objectMapper.writeValueAsString(new PaymentCreatedEvent(payment.getId()));
    OutboxMessage outbox = OutboxMessage.builder()
        .messageId(UUID.randomUUID().toString())
        .aggregateId(payment.getId())
        .eventType("PaymentCreated")
        .payload(payload)
        .topic("payment.events.created")
        .build();
    outboxRepo.save(outbox);
    // NO direct Kafka call here
}
```
Testing
Use @DataJpaTest with H2 to test repository methods.

Mock KafkaTemplate and verify OutboxPoller calls it.

Use @EnableScheduling on test config or manually call poller.

Pitfalls
Missing @EnableScheduling in main app → poller never runs.

Not using @Transactional → outbox row not rolled back if business fails.

Optimistic lock failure is normal under high concurrency; just skip.