package integration;

import dev.lydtech.dispatch.DispatchConfiguration;
import dev.lydtech.dispatch.message.DispatchPreparing;
import dev.lydtech.dispatch.message.OrderCreated;
import dev.lydtech.dispatch.message.OrderDispatched;
import dev.lydtech.dispatch.util.TestEventData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Slf4j
@SpringBootTest(classes = {DispatchConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@EmbeddedKafka(controlledShutdown = true)
public class OrderDispatchIntegrationTest {

    private final static String ORDER_CREATED_TOPIC = "order.created";          // IN

    private final static String ORDER_DISPATCHED_TOPIC = "order.dispatched";    // OUT

    private final static String DISPATCH_TRACKING_TOPIC = "dispatch.tracking";  // OUT

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Autowired
    private KafkaTestListener testListener;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Configuration
    static class TestConfig {

        @Bean
        public KafkaTestListener testListener() {
            return new KafkaTestListener();
        }
    }

    @Configuration
    public static class KafkaTestListener {

        AtomicInteger dispathPreparingCount = new AtomicInteger(0);
        AtomicInteger orderDispatchedCounter = new AtomicInteger(0);

        @KafkaListener(groupId = "KafkaIntegrationTest", topics = DISPATCH_TRACKING_TOPIC)
        void receiveDispatchPreparing(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload DispatchPreparing payload) {
            log.debug("Received DispatchPreparing: key: " + key + " - payload: " + payload);
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            dispathPreparingCount.incrementAndGet();
        }

        @KafkaListener(groupId = "KafkaIntegrationTest", topics = ORDER_DISPATCHED_TOPIC)
        void receiveDispatchPreparing(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload OrderDispatched payload) {
            log.debug("Received OrderDispatched: key: " + key + " - payload: " +  payload);
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            orderDispatchedCounter.incrementAndGet();
        }

    }

    @BeforeEach
    public void setUp() {
        testListener.dispathPreparingCount.set(0);
        testListener.orderDispatchedCounter.set(0);

        // instead of hard-coding delay, wire the registry bean from where we can get the listener container beans
        //  and wait until the embedded broker ( also wired in ) has had its excepted partitions assigned

        registry.getListenerContainers().stream().forEach(container ->
                ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic()));
    }

    @Test
    public void testOrderDispathFlow() throws Exception {
        OrderCreated orderCreated = TestEventData.buildOrderCreatedEvent(UUID.randomUUID(), "one-item");
        String key = randomUUID().toString();

        sendMessage(ORDER_CREATED_TOPIC, key, orderCreated);

        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispathPreparingCount::get, equalTo(1));

        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispathPreparingCount::get, equalTo(1));
    }

    private void sendMessage(String topic, String key, Object data) throws Exception {
        kafkaTemplate.send(MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.KEY, key)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build()).get();
    }
}
