package dev.lydtech.dispatch.integration;

import dev.lydtech.dispatch.DispatchConfiguration;
import dev.lydtech.dispatch.message.DispatchCompleted;
import dev.lydtech.dispatch.message.DispatchPreparing;
import dev.lydtech.dispatch.message.OrderCreated;
import dev.lydtech.dispatch.message.OrderDispatched;
import dev.lydtech.dispatch.util.TestEventData;
import dev.lydtech.dispatch.util.WiremockUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaHandler;
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

import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static dev.lydtech.dispatch.util.WiremockUtils.stubWiremock;
import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Slf4j
@SpringBootTest(classes = {DispatchConfiguration.class})
@AutoConfigureWireMock(port=0)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@EmbeddedKafka(controlledShutdown = true)
public class OrderDispatchIntegrationTest {

    private final static String ORDER_CREATED_TOPIC = "order.created";          // IN

    private final static String ORDER_DISPATCHED_TOPIC = "order.dispatched";    // OUT

    private final static String DISPATCH_TRACKING_TOPIC = "dispatch.tracking";  // OUT

    private final static String ORDER_CREATED_DLT_TOPIC = "order.created.DLT";  // OUT - auto name

    @Autowired
    private KafkaTemplate kafkaTemplate;

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

    /**
     * Use this receiver to consume messages from the outbound topics.
     */
    @KafkaListener(groupId = "KafkaIntegrationTest", topics = { DISPATCH_TRACKING_TOPIC, ORDER_DISPATCHED_TOPIC, ORDER_CREATED_DLT_TOPIC })
    public static class KafkaTestListener {
        AtomicInteger dispatchPreparingCounter = new AtomicInteger(0);
        AtomicInteger orderDispatchedCounter = new AtomicInteger(0);
        AtomicInteger dispatchCompletedCounter = new AtomicInteger(0);
        AtomicInteger orderCreatedDLTCounter = new AtomicInteger(0);

        @KafkaHandler
        void receiveDispatchPreparing(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload DispatchPreparing payload) {
            log.debug("Received DispatchPreparing key: " + key + " - payload: " + payload);
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            dispatchPreparingCounter.incrementAndGet();
        }

        @KafkaHandler
        void receiveOrderDispatched(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload OrderDispatched payload) {
            log.debug("Received OrderDispatched key: " + key + " - payload: " + payload);
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            orderDispatchedCounter.incrementAndGet();
        }

        @KafkaHandler
        void receiveDispatchCompleted(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload DispatchCompleted payload) {
            log.debug("Received DispatchCompleted key: " + key + " - payload: " + payload);
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            dispatchCompletedCounter.incrementAndGet();
        }

        @KafkaHandler
        void receiveOrderCreatedDLT(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload OrderCreated payload) {
            log.debug("Received OrderCreated DLT key: " + key + " - payload: " + payload);
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            orderCreatedDLTCounter.incrementAndGet();
        }
    }

    @BeforeEach
    public void setUp() {
        testListener.dispatchPreparingCounter.set(0);
        testListener.orderDispatchedCounter.set(0);
        testListener.dispatchCompletedCounter.set(0);
        testListener.orderCreatedDLTCounter.set(0);

        WiremockUtils.reset();

        // instead of hard-coding delay, wire the registry bean from where we can get the listener container beans
        //  and wait until the embedded broker ( also wired in ) has had its excepted partitions assigned

        registry.getListenerContainers().stream().forEach(container ->
                ContainerTestUtils.waitForAssignment(container, container.getContainerProperties().getTopics().length * embeddedKafkaBroker.getPartitionsPerTopic()));
    }

    /**
     * Send in an order.created event and ensure the expected outbound events are emitted.  The call to the stock service
     * is stubbed to return a 200 Success.
     */
    @Test
    public void testOrderDispatchFlow_Success() throws Exception {
        stubWiremock("/api/stock?item=one-item", 200, "true");

        OrderCreated orderCreated = TestEventData.buildOrderCreatedEvent(UUID.randomUUID(), "one-item");
        sendMessage(ORDER_CREATED_TOPIC, randomUUID().toString(), orderCreated);

        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchPreparingCounter::get, equalTo(1));
        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderDispatchedCounter::get, equalTo(1));
        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchCompletedCounter::get, equalTo(1));
        assertThat(testListener.orderCreatedDLTCounter.get(), equalTo(0));
    }

    /**
     * The call to the stock service is stubbed to return a 400 Bad Request which results in a not-retryable exception
     * being thrown, so the outbound events are never sent.
     */
    @Test
    public void testOrderDispatchFlow_NotRetryableException() throws Exception {
        stubWiremock("/api/stock?item=one-item", 400, "Bad Request");

        OrderCreated orderCreated = TestEventData.buildOrderCreatedEvent(UUID.randomUUID(), "one-item");
        sendMessage(ORDER_CREATED_TOPIC, randomUUID().toString(), orderCreated);

        //TimeUnit.SECONDS.sleep(3);
        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderCreatedDLTCounter::get, equalTo(1));
        assertThat(testListener.dispatchPreparingCounter.get(), equalTo(0));
        assertThat(testListener.orderDispatchedCounter.get(), equalTo(0));
        assertThat(testListener.dispatchCompletedCounter.get(), equalTo(0));
    }

    /**
     * The call to the stock service is stubbed to initially return a 503 Service Unavailable response, resulting in a
     * retryable exception being thrown.  On the subsequent attempt it is stubbed to then succeed, so the outbound events
     * are sent.
     */
    @Test
    public void testOrderDispatchFlow_RetryThenSuccess() throws Exception {
        stubWiremock("/api/stock?item=one-item", 503, "Service unavailable", "failOnce", STARTED, "succeedNextTime");
        stubWiremock("/api/stock?item=one-item", 200, "true", "failOnce", "succeedNextTime", "succeedNextTime");

        OrderCreated orderCreated = TestEventData.buildOrderCreatedEvent(UUID.randomUUID(), "one-item");
        sendMessage(ORDER_CREATED_TOPIC, randomUUID().toString(), orderCreated);

        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchPreparingCounter::get, equalTo(1));
        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderDispatchedCounter::get, equalTo(1));
        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchCompletedCounter::get, equalTo(1));
        assertThat(testListener.orderCreatedDLTCounter.get(), equalTo(0));
    }

    /**
     * The call to the stock service is stubbed to initially return a 503 Service Unavailable response.  This results in
     * retryable exceptions being thrown continually, eventually exceeding the retry limit.  The event is sent to the
     * dead letter topic, and the outbound events are never sent.
     */
    @Test
    public void testOrderDispatchFlow_RetryUntilFailure() throws Exception {
        stubWiremock("/api/stock?item=one-item", 503, "Service unavailable");

        OrderCreated orderCreated = TestEventData.buildOrderCreatedEvent(UUID.randomUUID(), "one-item");
        sendMessage(ORDER_CREATED_TOPIC, randomUUID().toString(), orderCreated);

        //TimeUnit.SECONDS.sleep(3);
        await().atMost(5, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderCreatedDLTCounter::get, equalTo(1));
        assertThat(testListener.dispatchPreparingCounter.get(), equalTo(0));
        assertThat(testListener.orderDispatchedCounter.get(), equalTo(0));
        assertThat(testListener.dispatchCompletedCounter.get(), equalTo(0));
    }

    private void sendMessage(String topic, String key, Object data) throws Exception {
        kafkaTemplate.send(MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.KEY, key)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build()).get();
    }
}
