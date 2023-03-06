# Intro

This is a sample project on how to send [CloudEvents](https://cloudevents.io/) with [Spring Cloud Stream](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/) and [Apache Kafka](https://kafka.apache.org/)

For a version of this project using pure Spring Boot and Spring Kafka integration check [here](https://1v0.dev/posts/17-spring-kafka-cloudevents/)

# Initial Configuration

## Running Kafka

The simplest way to run Kafka is using Docker containers and Docker Compose. Here is my compose file using [bitnami](https://bitnami.com/) images:
```yaml
version: "3"  
services:  
  zookeeper:  
    image: 'bitnami/zookeeper:latest'  
    container_name: zookeeper  
    ports:  
      - '2181:2181'  
    environment:  
      - ALLOW_ANONYMOUS_LOGIN=yes  
  kafka:  
    image: 'bitnami/kafka:latest'  
    container_name: kafka  
    ports:  
      - '9092:9092'  
    environment:  
      - KAFKA_BROKER_ID=1  
      - KAFKA_CFG_LISTENERS=PLAINTEXT://:9092  
      - KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://127.0.0.1:9092  
      - KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper:2181  
      - ALLOW_PLAINTEXT_LISTENER=yes  
    depends_on:  
      - zookeeper
```

## Dependancies

We need to add our libraries. I will be using Gradle with Kotlin:

```kotlin
dependencies {  
    implementation("org.springframework.cloud:spring-cloud-stream") // Cloud Stream
    implementation("org.springframework.cloud:spring-cloud-stream-binder-kafka") // We need the binder to use Kafka
    implementation("io.cloudevents:cloudevents-spring:2.3.0") // Cloudevents Spring integration
    implementation("io.cloudevents:cloudevents-json-jackson:2.4.1") //CloudEvents Jackson bindings for event data
    implementation("com.github.javafaker:javafaker:1.0.2") //Using faker to generate some data   
}
```

## Configuration

In case of Spring Cloud Stream, most of what we need is autoconfigured. When we included the Kafka binder in the previous
step, all the connection configuration is provided for us. Since I am using local installation with default ports,
everything will work our of the box.

To our `@SpringBootApplication` class we need to set up only a couple of things.

First we need ObjectMapper instance to handle serialization:
```java
private final ObjectMapper mapper = new ObjectMapper();
```

Then we need a message converter. CloudEvents provide pre configured converter to work with Spring, so we only 
need to provide it as a bean:
```java
@Bean
public CloudEventMessageConverter cloudEventMessageConverter() {
        return new CloudEventMessageConverter();
}
```

# Producing events

Now we are ready to create our producer:
```java
@Bean
public Supplier<CloudEvent> kafkaSender() {
    return () -> {
        SampleData data = new SampleData();
        try {
            return CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withSource(URI.create("https://1v0dev/producer"))
                    .withType("com.dev1v0.producer")
                    .withData(mapper.writeValueAsBytes(data))
                    .withExtension("name", data.getName())
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Error serializing message", e);
            return null;
        }
    };
}
```

It's just a simple `Supplier` implementation exposed as a Bean that creates a `CloudEvent` object. 
We also need to provide the method name to Cloud Stream so it can autowire our function and also any additional provider
specific configuration we require (in this case the message topic):
In `application.properties` add
```properties
spring.cloud.function.definition=kafkaSender
spring.cloud.stream.bindings.kafkaSender-out-0.destination=main-topic
```
The name of the second property comes from [Cloud Stream auto bindings](spring.cloud.stream.bindings.kafkaReceiver-in-0.destination=main-topic).

Spring will handle most of the rest.

I also haven't added any scheduling or manual calling of this function. Spring Cloud Stream will call it automatically
every second using the default [poller](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream.html#_supplier_threading)

Note how we serialise the `data` object to JSON byte array using Jackson's `ObjectMapper`. The CloudEvents API require using byte array for the data property. In the consumer we will use a class provided by CloudEvents to convert the data back to `SampleData`.

Of course using JSON is just one way of doing this. [Apache Avro](https://avro.apache.org/) or [ProtocolBuffers](https://developers.google.com/protocol-buffers) are also good fits here, especially if you need some kind of schema repository for synchronisation of the data format.

# Receiving Events

Receiving messages is also simple. Again we provide a standard java functional interface as a bean - this time it is `Consumer`:

```java
@Bean
public Consumer<CloudEvent> kafkaReceiver() {
    return (message) -> {
            PojoCloudEventData<SampleData> deserializedData = CloudEventUtils
            .mapData(message, PojoCloudEventDataMapper.from(mapper, SampleData.class));

            if (deserializedData != null) {
                SampleData data = deserializedData.getValue();
                log.info("Received message. Id: {}; Data: {}", message.getId(), data.toString());
            } else {
                log.warn("No data in message {}", message.getId());
            }
        };
    }
```
Again we need to add the function definition and topic configuration. In `application.properties`:
```properties
spring.cloud.function.definition=kafkaSender;kafkaReceiver
spring.cloud.stream.bindings.kafkaReceiver-in-0.destination=main-topic
```

The configured message converter will take care of the event conversion. But this is only for the CloudEvent itself. In order to get `SampleData` object from the `data` field of the event we need to use `PojoCloudEventDataMapper` provided by the CloudEvents Jackson library. Here is the relevant bit of the official documentation https://cloudevents.github.io/sdk-java/json-jackson.html#mapping-cloudeventdata-to-pojos-using-jackson-objectmapper

# Conclusion

And that is it. If you have Kafka running, you can start the sample app and you will send and receive CloudEvent messages through Kafka.