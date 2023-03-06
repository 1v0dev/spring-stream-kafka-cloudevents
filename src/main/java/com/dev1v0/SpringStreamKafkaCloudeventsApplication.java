package com.dev1v0;

import com.dev1v0.kafka.SampleData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.CloudEventUtils;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;
import io.cloudevents.jackson.PojoCloudEventDataMapper;
import io.cloudevents.spring.messaging.CloudEventMessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.net.URI;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SpringBootApplication
public class SpringStreamKafkaCloudeventsApplication {

    private static final Logger log = LoggerFactory.getLogger(SpringStreamKafkaCloudeventsApplication.class);
    private final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        SpringApplication.run(SpringStreamKafkaCloudeventsApplication.class, args);
    }

    @Bean
    public CloudEventMessageConverter cloudEventMessageConverter() {
        return new CloudEventMessageConverter();
    }

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

}
