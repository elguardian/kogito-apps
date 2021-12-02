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

package org.kie.kogito.jitexecutor.process;

import java.util.List;
import java.util.Map;

public interface JITProcessService {

    void compile();

    List<String> listProcessIdentifiers();

    String startProcess(String processId, Map<String, Object> parameters);

    List<String> listProcessInstanceIdentifiers(String processId);

    void signal(String processInstanceId, String event, Object payload);

    void signal(String event, Object payload);

    Map<String, Object> getProcessInstanceState(String processId, String processInstanceId);

    void abortProcessInstance(String processId, String processInstanceId);



}
