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
package org.kie.kogito.explainability.messaging;

import java.util.Collections;

import org.junit.jupiter.api.Disabled;
import org.kie.kogito.explainability.api.BaseExplainabilityRequestDto;
import org.kie.kogito.explainability.api.BaseExplainabilityResultDto;
import org.kie.kogito.explainability.api.CounterfactualExplainabilityRequestDto;
import org.kie.kogito.explainability.api.CounterfactualExplainabilityResultDto;
import org.kie.kogito.testcontainers.quarkus.KafkaQuarkusTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(KafkaQuarkusTestResource.class)
@Disabled("Counterfactuals Explainability requests are not currently handled.")
class ExplainabilityMessagingHandlerCounterfactualsIT extends BaseExplainabilityMessagingHandlerIT {

    @Override
    protected BaseExplainabilityRequestDto buildRequest() {
        return new CounterfactualExplainabilityRequestDto(EXECUTION_ID,
                SERVICE_URL,
                MODEL_IDENTIFIER_DTO,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap());
    }

    @Override
    protected BaseExplainabilityResultDto buildResult() {
        return CounterfactualExplainabilityResultDto.buildSucceeded(EXECUTION_ID);
    }

    @Override
    protected void assertResult(BaseExplainabilityResultDto result) {
        assertTrue(result instanceof CounterfactualExplainabilityResultDto);
    }
}
