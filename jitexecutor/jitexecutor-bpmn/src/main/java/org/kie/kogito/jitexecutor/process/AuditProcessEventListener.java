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

import java.util.Collection;

import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.process.ProcessNodeLeftEvent;
import org.kie.api.event.process.ProcessNodeTriggeredEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.event.process.ProcessVariableChangedEvent;
import org.kie.kogito.Addons;
import org.kie.kogito.event.DataEvent;
import org.kie.kogito.services.event.impl.ProcessInstanceEventBatch;

public class AuditProcessEventListener implements ProcessEventListener {

    private ProcessInstanceEventBatch batch = new ProcessInstanceEventBatch("BPMNJITEvaluator", Addons.EMTPY);

    @Override
    public void beforeProcessStarted(ProcessStartedEvent event) {
        batch.append(event);
    }

    @Override
    public void afterProcessStarted(ProcessStartedEvent event) {
        batch.append(event);
    }

    @Override
    public void beforeProcessCompleted(ProcessCompletedEvent event) {
        batch.append(event);
    }

    @Override
    public void afterProcessCompleted(ProcessCompletedEvent event) {
        batch.append(event);
    }

    @Override
    public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
        batch.append(event);
    }

    @Override
    public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
        batch.append(event);
    }

    @Override
    public void beforeNodeLeft(ProcessNodeLeftEvent event) {
        batch.append(event);
    }

    @Override
    public void afterNodeLeft(ProcessNodeLeftEvent event) {
        batch.append(event);
    }

    @Override
    public void beforeVariableChanged(ProcessVariableChangedEvent event) {
        batch.append(event);
    }

    @Override
    public void afterVariableChanged(ProcessVariableChangedEvent event) {
        batch.append(event);
    }

    public Collection<DataEvent<?>> getAudit() {
        return batch.events();
    }

}