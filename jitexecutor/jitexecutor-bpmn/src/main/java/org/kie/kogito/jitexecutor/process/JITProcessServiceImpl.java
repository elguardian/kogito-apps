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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jbpm.compiler.canonical.VariableDeclarations;
import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.process.instance.LightProcessRuntime;
import org.jbpm.ruleflow.core.Metadata;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.kie.api.definition.process.Node;
import org.kie.kogito.Addons;
import org.kie.kogito.Application;
import org.kie.kogito.StaticApplication;
import org.kie.kogito.StaticConfig;
import org.kie.kogito.internal.process.runtime.KogitoNodeInstance;
import org.kie.kogito.internal.process.runtime.KogitoProcessInstance;
import org.kie.kogito.internal.process.runtime.KogitoWorkItem;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemHandler;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemManager;
import org.kie.kogito.internal.process.runtime.KogitoWorkflowProcessInstance;
import org.kie.kogito.process.ProcessConfig;
import org.kie.kogito.process.bpmn2.BpmnProcesses;
import org.kie.kogito.process.impl.AbstractProcess;
import org.kie.kogito.process.impl.ConfiguredProcessServices;
import org.kie.kogito.process.impl.DefaultProcessEventListenerConfig;
import org.kie.kogito.process.impl.DefaultWorkItemHandlerConfig;
import org.kie.kogito.process.impl.StaticProcessConfig;
import org.kie.kogito.process.workitems.InternalKogitoWorkItemManager;
import org.kie.kogito.process.workitems.impl.KogitoWorkItemImpl;
import org.kie.kogito.services.uow.CollectingUnitOfWorkFactory;
import org.kie.kogito.services.uow.DefaultUnitOfWorkManager;
import org.kie.services.jobs.impl.InMemoryJobService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Functions;

@Singleton
public class JITProcessServiceImpl implements JITProcessService {

    @Inject
    protected ProcessRepository processRepository;

    @Inject
    @Any
    protected Instance<ProcessFactory> processFactories;

    @Inject
    protected KafkaManager kafkaManager;

    private LightProcessRuntime processRuntime;

    private Map<String, ProcessBuild> processBuild;


    @Override
    public void compile() {
        rebuild();
    }

    private void rebuild() {
        if(this.processRuntime != null) {
            this.processRuntime.removeProcessEventListeners();
            this.processRuntime.dispose();
        }

        BpmnProcesses processes = new BpmnProcesses();

        List<ProcessFile> processFiles = processRepository.processes();
        List<ProcessBuild> processBuild = new ArrayList<>();

        for (ProcessFile processFile : processFiles) {
            for (ProcessFactory processFactory : processFactories) {
                if (processFactory.accept(processFile)) {
                    processBuild.add(processFactory.createProcessDefinition(processFile));
                }
            }
        }

        processBuild.stream().filter(e -> !e.hasErrors()).map(ProcessBuild::process).forEach(processes::addProcess);

        ProcessConfig processConfig = new StaticProcessConfig(
                new DefaultWorkItemHandlerConfig(),
                new DefaultProcessEventListenerConfig(),
                new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory()),
                InMemoryJobService.get(processes, new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory())));

        StaticConfig appConfig = new StaticConfig(Addons.EMTPY, processConfig);
        Application application = new StaticApplication(appConfig, processes);

        List<org.kie.api.definition.process.Process> processDefinitions = processBuild.stream()
                .filter(e -> !e.hasErrors())
                .map(ProcessBuild::process)
                .map(e -> ((AbstractProcess) e).process())
                .collect(Collectors.toList());

        for (org.kie.api.definition.process.Process process : processDefinitions) {
            RuleFlowProcess ruleFlowProcess = (RuleFlowProcess) process;
            Boolean isServerLessWorkflow = (Boolean) ruleFlowProcess.getMetaData("ServerlessWorkflow");
            if(isServerLessWorkflow != null && isServerLessWorkflow) {
                System.out.println("PROCESS " + ruleFlowProcess.getType() + "  " + ruleFlowProcess.getProcessType() + " " + ruleFlowProcess.getName());
                for (Node node : ruleFlowProcess.getStartNodes()) {
                    String type = (String) node.getMetaData().get(Metadata.TRIGGER_REF);
                    if("kafka".equals(type)) {
                        String topicName = (String) node.getMetaData().get("Event.Source");
                        kafkaManager.subscribeTopic(topicName, (topic, payload) -> {
                            Object data = payload;
                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                data = mapper.readTree((String) payload);
                                this.processRuntime.signalEvent("com.fasterxml.jackson.databind.JsonNode", data);
                            } catch (JsonProcessingException e) {
                                // do nothing
                            }
                        });
                    }
                }
            }
        }

        // we build runtime and build map
        this.processBuild = processBuild.stream().collect(Collectors.toMap(ProcessBuild::id, Functions.identity()));
        this.processRuntime = LightProcessRuntime.of(application, processDefinitions, new ConfiguredProcessServices(processConfig));
        AuditProcessEventListener processListener = new AuditProcessEventListener();
        this.processRuntime.addEventListener(processListener);
        this.processRuntime.initProcessEventListeners();
        this.processRuntime.initStartTimers();
        ((InternalKogitoWorkItemManager) this.processRuntime.getWorkItemManager()).registerWorkItemHandler("Send Task", new KogitoWorkItemHandler() {

            @Override
            public void executeWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager manager) {
                KogitoWorkItemImpl kogitoWorkItem = (KogitoWorkItemImpl) workItem;
                WorkflowProcessInstance instance = (WorkflowProcessInstance) processRuntime.getProcessInstance(kogitoWorkItem.getProcessInstanceStringId());
                KogitoNodeInstance nodeInstance = instance.getNodeInstance(kogitoWorkItem.getNodeInstanceStringId());
                String topic = (String) nodeInstance.getNode().getMetaData().get("Event.Source");
                String variable = (String )  workItem.getParameter("Message");
                Object data = instance.getVariable(variable);
                kafkaManager.pushEvent(topic, data);
            }

            @Override
            public void abortWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager manager) {
                System.out.println(workItem);
            }
        });
    }



    @Override
    public List<String> listProcessIdentifiers() {
        return processBuild.entrySet().stream()
                .filter(e -> !e.getValue().hasErrors())
                .map(e -> e.getValue().id())
                .collect(Collectors.toList());
    }

    private void validate(String processId) {
        if (!processBuild.containsKey(processId)) {
            throw new IllegalArgumentException("Process " + processId + " does not exists");
        }

        if (processBuild.get(processId).hasErrors()) {
            throw new IllegalArgumentException(String.join(",", processBuild.get(processId).errors()));
        }
    }
    @Override
    public String startProcess(String processId, Map<String, Object> parameters) {
        validate(processId);
        WorkflowProcessInstance processInstance = (WorkflowProcessInstance) this.processRuntime.startProcess(processId, parameters);
        return processInstance.getStringId();
    }

    @Override
    public void abortProcessInstance(String processId, String processInstanceId) {
        validate(processId);
        this.processRuntime.abortProcessInstance(processInstanceId);
    }
    @Override
    public List<String> listProcessInstanceIdentifiers(String processId) {
        return processRuntime.getProcessInstances().stream()
                .filter(e -> e.getProcessId().equals(processId))
                .map(e -> ((KogitoWorkflowProcessInstance) e).getStringId())
                .collect(Collectors.toList());
    }

    @Override
    public void signal(String event, Object payload) {
        this.processRuntime.signalEvent(event, payload);
    }

    @Override
    public void signal(String processInstanceId, String event, Object payload) {
        this.processRuntime.signalEvent(event, payload, processInstanceId);
    }

    @Override
    public Map<String, Object> getProcessInstanceState(String processId, String processInstanceId) {
        validate(processId);

        KogitoProcessInstance processInstance = this.processRuntime.getProcessInstance(processInstanceId);
        VariableDeclarations varDecl = VariableDeclarations.ofOutput(
                (VariableScope) ((org.jbpm.process.core.Process) processInstance.getProcess()).getDefaultContext(VariableScope.VARIABLE_SCOPE));
        Map<String, Object> results = new HashMap<String, Object>();
        Map<String, Object> vars = ((WorkflowProcessInstance) processInstance).getVariables();
        for (String varName : varDecl.getTypes().keySet()) {
            results.put(varName, vars.get(varName));
        }
        return results;

    }

}
