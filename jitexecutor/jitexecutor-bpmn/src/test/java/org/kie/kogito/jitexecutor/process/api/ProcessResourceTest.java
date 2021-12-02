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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.kogito.jitexecutor.api.requests.SendEventRequest;
import org.kie.kogito.jitexecutor.api.requests.StartProcessRequest;
import org.kie.kogito.jitexecutor.api.responses.IdentifierResponse;
import org.kie.kogito.jitexecutor.api.responses.ListIdentifierResponse;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class ProcessResourceTest {

    @BeforeEach
    public void bootstrap() {
        Response response = given()
                .contentType(ContentType.JSON)
                .when().post("/runtime/compile");

        response.then()
                .statusCode(200);
    }

    @Test
    public void testListProcess() {
        Response response = given()
                .contentType(ContentType.JSON)
                .when()
                .get("/process/");

        List<String> identifiers = response.then()
                .statusCode(200)
                .extract().as(ListIdentifierResponse.class).getIdentifiers();

        identifiers.forEach(e -> System.out.println(e));
        Assertions.assertEquals(3, identifiers.size());
    }

    @Test
    public void testScript() {
        StartProcessRequest request = new StartProcessRequest(Collections.emptyMap());
        Response response = given()
                .contentType(ContentType.JSON)
                .when()
                .body(request)
                .post("/process/{processId}/", "test.Script");

        String id = response.then()
                .statusCode(200)
                .extract().as(IdentifierResponse.class).getId();

        Assertions.assertNotNull(id);
    }


    
    
    @Test
    public void testSwitchWorkflow() {
        SendEventRequest event = new SendEventRequest("com.fasterxml.jackson.databind.JsonNode", "{}");
        Response response = given()
                .contentType(ContentType.JSON)
                .when()
                .body(event)
                .post("/runtime/signal/");

        response.then()
                .statusCode(200);

        response = given()
                .contentType(ContentType.JSON)
                .when()
                .get("/process/{processId}/", "eventswitchworkflow");

        List<String> ids = response.then()
                .statusCode(200)
                .extract()
                .as(ListIdentifierResponse.class).getIdentifiers();

        // straight through process
        Assertions.assertNotNull(ids);
        Assertions.assertEquals(1, ids.size());
    }

}
