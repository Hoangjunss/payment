package com.paymentplatform.userservice.application.port.out;

public interface PublishEventPort {
    void publish(String topic, String aggregateId, String eventType, String payload);
}
