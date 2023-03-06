plugins {
    java
    id("org.springframework.boot") version "3.0.4"
    id("io.spring.dependency-management") version "1.1.0"
}

group = "com.dev1v0"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_19

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2022.0.1"

dependencies {
    implementation("org.springframework.cloud:spring-cloud-stream") // Cloud Stream
    implementation("org.springframework.cloud:spring-cloud-stream-binder-kafka") // We need the binder to use Kafka
    implementation("io.cloudevents:cloudevents-spring:2.3.0") // Cloudevents Spring integration
    implementation("io.cloudevents:cloudevents-json-jackson:2.4.1") //CloudEvents Jackson bindings for event data
    implementation("com.github.javafaker:javafaker:1.0.2") //Using faker to generate some data
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.cloud:spring-cloud-stream-test-binder")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
