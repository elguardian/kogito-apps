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

package org.kie.kogito.jitexecutor.process.factory;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.drools.core.io.impl.ByteArrayResource;
import org.jbpm.process.core.validation.ProcessValidationError;
import org.jbpm.ruleflow.core.validation.RuleFlowProcessValidator;
import org.kie.kogito.jitexecutor.process.ProcessBuild;
import org.kie.kogito.jitexecutor.process.ProcessFactory;
import org.kie.kogito.jitexecutor.process.ProcessFile;
import org.kie.kogito.process.bpmn2.BpmnProcess;

@ApplicationScoped
public class BPMNProcessFactory extends AbstractProcessFactory implements ProcessFactory {

    @Override
    public boolean accept(ProcessFile processFile) {
        return processFile.name().endsWith(".bpmn");
    }

    @Override
    public ProcessBuild createProcessDefinition(ProcessFile processFile) {
        List<String> errorMessages = new ArrayList<>();
        BpmnProcess process = BpmnProcess.from(new ByteArrayResource(processFile.content().getBytes())).get(0);
        ProcessBuild processBuild = new ProcessBuild(process.id());
        for (ProcessValidationError e : RuleFlowProcessValidator.getInstance().validateProcess(process.process())) {
            errorMessages.add(e.toString());
        }

        if(errorMessages.isEmpty()) {
            compileProcess(processBuild, process.process());
            processBuild.setProcess(process);
        } else {
            errorMessages.forEach(processBuild::addError);
        }

        return processBuild;
    }


}
