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

package org.kie.kogito.taskassigning.service;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.kie.kogito.taskassigning.user.service.UserServiceConnector;

@ApplicationScoped
public class UserServiceConnectorRegistry {

    private static final String TOO_MANY_CONNECTORS_ERROR = "Two different connectors for the same name: %s where found. " +
            " connector class1: %s, connector class2: %s";

    private final Map<String, UserServiceConnector> connectors = new HashMap<>();

    @Inject
    public UserServiceConnectorRegistry(Instance<UserServiceConnector> connectorInstance) {
        connectorInstance.stream()
                .forEach(connector -> {
                    UserServiceConnector existingConnector = connectors.get(connector.getName());
                    if (existingConnector != null) {
                        throw new IndexOutOfBoundsException(String.format(TOO_MANY_CONNECTORS_ERROR,
                                connector.getName(),
                                existingConnector.getClass().getName(),
                                connector.getClass().getName()));
                    } else {
                        connectors.put(connector.getName(), connector);
                    }
                });
    }

    public UserServiceConnector get(String name) {
        return connectors.get(name);
    }
}
