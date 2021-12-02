/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito.jitexecutor.process.api;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.kogito.jitexecutor.api.responses.ListIdentifierResponse;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class StatelessProcessResourceTest {

    @BeforeEach
    public void bootstrap() {
        Response response = given()
                .contentType(ContentType.JSON)
                .when().post("/runtime/compile");

        response.then()
                .statusCode(200);
    }

    @Test
    public void testApplicantWorkflow() throws Exception {

        try(KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(createProducerProperties())) {
            kafkaProducer.send(new ProducerRecord<String, String>("applicants", "applicants", "{ \"salary\" : 3500 } ")).get();
        }


        String value = null;
        try(KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(createConsumerProperties())) {
            kafkaConsumer.subscribe(Collections.singleton("decisions"));
            for(ConsumerRecord<String, String> record : kafkaConsumer.poll(Duration.ofMillis(5000))) {
                value = record.value();
            }
        }
        // straight through process
        Assertions.assertNotNull(value);
        Assertions.assertEquals("{\"salary\":3500,\"decision\":\"Approved\"}", value);
    }

    private Properties createConsumerProperties() {
        Properties props = createCommonProperties();
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        return props;
    }

    private Properties createProducerProperties() {
        Properties props = createCommonProperties();
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        return props;
    }

    private Properties createCommonProperties() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "127.0.0.1:9092");
        props.put("group.id", "group-id");
        return props;
    }

}
