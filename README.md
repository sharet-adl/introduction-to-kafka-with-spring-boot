# Introduction to Kafka with Spring Boot

This repository contains the code to support the [Introduction to Kafka with Spring Boot](https://www.udemy.com/course/introduction-to-kafka-with-spring-boot/?referralCode=15118530CA63AD1AF16D) online course.

The application code is for a message driven service which utilises Kafka and Spring Boot 3.

The code in this repository is used to help students learn how to produce and consume messages in Java using Spring Kafka and Spring Boot 3.
The course provides step by step instructions and detailed explanations for students to build up the code themselves.
This repository provides the code to support the course broken down section by section using branches, allowing students to
compare and contrast the code they create with the lesson code in the branches.

As you work through the course, please feel free to fork this repository to your own GitHub repo. Most lectures contain links
to source code changes. If you encounter a problem you can compare your code to the lecture code. See the [How to Compare Branches](https://github.com/lydtechconsulting/introduction-to-kafka-with-spring-boot/wiki#how-to-compare-branches) section in the Wiki.

## Introduction to Kafka with Spring Boot Course Wiki
Plenty of useful information about your Introduction to Kafka with Spring Boot course can be found in the [Wiki](https://github.com/lydtechconsulting/introduction-to-kafka-with-spring-boot/wiki).

## Getting Your Development Environment Setup
### Recommended Versions
| Recommended                | Reference                                                             | Notes                                                                                                                                                                                                                                                          |
|----------------------------|-----------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Apache Kafka 3.3 or higher | [Download](https://kafka.apache.org/downloads)                        |                                                                                                                                                                                    |
| Oracle Java 17 JDK         | [Download](https://www.oracle.com/java/technologies/downloads/#java17) | Java 17 or higher. We recommend using the most recent LTS (Long-Term Support) release                                                                                                                                                                          |
| IntelliJ 2022 or higher    | [Download](https://www.jetbrains.com/idea/download/)                  | Ultimate Edition recommended. Students can get a free 120 trial license, courtesy of the Spring Framework Guru, [here](https://github.com/springframeworkguru/spring5webapp/wiki/Which-IDE-to-Use%3F#how-do-i-get-the-free-120-day-trial-to-intellij-ultimate) |
| Maven 3.6 or higher        | [Download](https://maven.apache.org/download.cgi)                     | [Installation Instructions](https://maven.apache.org/install.html)                                                                                                                                                                                             |                                                                                                                 | **Note:** Use Version 5 or higher if using Java 11                                                                                                                                                                     |
| Git 2.39 or higher         | [Download](https://git-scm.com/downloads)                             |                                                                                                                                                                                                                                                                | 
| Git GUI Clients            | [Download](https://git-scm.com/downloads/guis)                        | Not required. But can be helpful if new to Git. SourceTree is a good option for Mac and Windows users.                                                                                                                                                         |

## Connect with the team at Lydtech Consulting
* Visit us at [lydtechconsulting.com](https://www.lydtechconsulting.com/)
* Visit our [LinkedIn](https://www.linkedin.com/company/lydtech-consulting) page

## Diagram

```md diagram services and topics 
                                                                                                                  
                                                                                                                  
                    OrderCreated MSG                      DispatchPreparing MSG                   TrackingStatus MSG                                                                              
                  ┌───────────────┐                     ┌───────────────┐                       ┌───────────────┐ 
         ┌───────►├┼┼┼┼┼┼┼┼┼┼┼┼┼┼┼┼──────┐       ┌─────►├┼┼┼┼┼┼┼┼┼┼┼┼┼┼┼┼────────┐      ┌──────►├┼┼┼┼┼┼┼┼┼┼┼┼┼┼┼│ 
         │        └───────────────┘      │       │      └───────────────┘        │      │       └───────────────┘ 
         │         order.created         │       │       dispatch.tracking       │      │        tracking.status  
         │                               │       │                               │      │                         
 ┌───────┴───┐                        ┌──▼───────┴─┐                           ┌─▼──────┴─┐                       
 │  ORDER    │                        │  DISPATCH  │                           │ TRACKING │                       
 │   MS      │                        │    MS      │                           │   MS     │                       
 └─────────▲─┘                        └──────────┬─┘                           └──────────┘                       
           │                                     │                                                                
           │                                     │                                                                
           │                                     │                                                                
           │                                     │                                                                
           │                                     │      OrderDispatched MSG                                                        
           │                                     │     ┌───────────────┐                                          
           │                                     └────►├┼┼┼┼┼┼┼┼┼┼┼┼┼┼┼│                                          
           │                                           └───────────────┘                                          
           │                                            order.dispatched                                          
           │                                              │                                                       
           └──────────────────────────────────────────────┘                                                       
                                                                                                                  
```

```md Sample integration test
                                                             
                         ┌───────────────┐                   
         ┌──────────────►├┼┼┼┼┼┼┼┼┼┼┼┼┼┼┼┼────────┐          
         │               └───────────────┘        │          
         │                 dispatch.tracking      │          
         │                                        │          
 ┌───────┴────────┐                             ┌─▼────────┐ 
 │   INTEGRATION  │                             │ TRACKING │ 
 │     TEST       │                             │   MS     │ 
 └───────────▲────┘                             └───────┬──┘ 
             │           ┌───────────────┐              │    
             └───────────┼┼┼┼┼┼┼┼┼┼┼┼┼┼┼┼┤◄─────────────┘    
                         └───────────────┘                   
                          tracking.status                    
                                                             
```
```md shared consumer group

```


![img.png](img.png)

![img_1.png](img_1.png)

## Sample message
{"orderId": "2be645fd-3c0f-4fec-b21f-2af26b3d5f77", "item": "item1"}

## CLI

$ kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic order.created --property parse.key=true --property key.separator=:
> "123":{"orderId": "2be645fd-3c0f-4fec-b21f-2af26b3d5f77", "item": "item1"}

$ kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic order.created
$ kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --topic order.created --alter --partitions 5

$ kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group dispatch.order.created.consumer


## Testing
- test execution thread vs. application execution thread :: assure the breakpoint has 'Suspend=Thread' ( not All )

- "200":{"orderId": "2be645fd-3c0f-4fec-b21f-2af26b3d5f77", "item": "item_200"}    // expect no error, outbound event
- "400":{"orderId": "2be645fd-3c0f-4fec-b21f-2af26b3d5f77", "item": "item_400"}    // expect error, with no retry (maxAttempts=0), no outboud events Order.dispatched
- "502":{"orderId": "2be645fd-3c0f-4fec-b21f-2af26b3d5f77", "item": "item_502"}    // expect 1 error (Bad Gateway), with retry and then success

## DLT

Default topic for DLQ/DLT is the original topic name, followed by ".DLT" - using spring kafak dead-letter publishing Recoverer.
 Target for events that are not retryable, or that are retryable not exhausted the max retries.
 Need to update the DefaultErrorHandler(), to pass a DeadLetterPublishingRecoverer() - which is using KafkaTemplate..


## Notes

The producer calculates a hash of the provided message key ( if provided ) and uses it to determine which partition to write the message to.
The console-producer will add a Header with the key, when it writes it to kafka.

## Assignments

### Consume and produce events using Spring Kafka
..

### Integration Test Assignment

Create a new integration test class in the Tracking Service, with the appropriate class annotations that are needed for a spring boot integration test

Setup a KafkaTestListener to consume from the tracking.status topic. The listener will need to increment a counter every time a message is consumed

Add the listener to the spring context and autowire it into the integration test

The test will send a DispatchPreparing event to the dispatch.tracking topic, so a KafkaTemplate will need to be autowired into the integration test class

Use the Awaitility test library in the test to wait until the message received count, defined in the KafkaTestListener, has been incremented.

Add a BeforeEach test setup method to ensure the partitions for the listener have had sufficient time to be assigned

Create an application-test.properties file in order to specify the embedded kafka broker address

### Consume Multiple Event Types from the same Topic

The Dispatch Service will be updated to produce a new event that will be sent to the existing topic, 
and the Tracking Service consumer will be updated to consume both event types from this topic. 

The Tracking Service will also be updated to produce a new TrackingStatusUpdated event.
