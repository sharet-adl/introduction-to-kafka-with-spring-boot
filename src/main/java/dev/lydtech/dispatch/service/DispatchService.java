package dev.lydtech.dispatch.service;

import dev.lydtech.dispatch.message.DispatchCompleted;
import dev.lydtech.dispatch.message.DispatchPreparing;
import dev.lydtech.dispatch.message.OrderCreated;
import dev.lydtech.dispatch.message.OrderDispatched;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static java.util.UUID.randomUUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService {

    private static final String DISPATCH_TRACKING_TOPIC = "dispatch.tracking";
    private static final String ORDER_DISPATCHED_TOPIC = "order.dispatched";
    private static final UUID APPLICATION_ID = randomUUID();

    private final KafkaTemplate<String, Object> kafkaProducer;

    public void process(String key, OrderCreated orderCreated) throws ExecutionException, InterruptedException {
        DispatchPreparing dispatchPreparing = DispatchPreparing.builder()
                .orderId(orderCreated.getOrderId())
                .build();
        // default is async, make it sync using the .get()
        kafkaProducer.send(DISPATCH_TRACKING_TOPIC, key, dispatchPreparing).get();

        OrderDispatched orderDispatched = OrderDispatched.builder()
                .orderId(orderCreated.getOrderId())
                .processedById(APPLICATION_ID)
                .notes("Dispatched: " + orderCreated.getItem())
                .build();
        // default is async, make it sync using the .get()
        kafkaProducer.send(ORDER_DISPATCHED_TOPIC, key, orderDispatched).get();

        DispatchCompleted dispatchCompleted = DispatchCompleted.builder()
                .orderId(orderCreated.getOrderId())
                .dispatchedDate(LocalDateTime.now().toString())
                .build();
        kafkaProducer.send(DISPATCH_TRACKING_TOPIC, key, dispatchCompleted).get();

        log.info("Sent messages: key: " + key + " - orderId: " + orderCreated.getOrderId() + " - processedById: " + APPLICATION_ID);
    }
}
