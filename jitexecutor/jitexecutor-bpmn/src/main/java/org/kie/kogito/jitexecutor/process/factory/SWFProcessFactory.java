package org.kie.kogito.jitexecutor.process.factory;

import javax.enterprise.context.ApplicationScoped;

import org.jbpm.ruleflow.core.Metadata;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.workflow.core.node.EndNode;
import org.jbpm.workflow.core.node.StartNode;
import org.kie.api.definition.process.Node;
import org.kie.api.definition.process.Process;
import org.kie.kogito.jitexecutor.process.ProcessBuild;
import org.kie.kogito.jitexecutor.process.ProcessFactory;
import org.kie.kogito.jitexecutor.process.ProcessFile;
import org.kie.kogito.process.bpmn2.BpmnProcess;
import org.kie.kogito.serverless.workflow.parser.ServerlessWorkflowParser;
import org.kie.kogito.serverless.workflow.utils.ServerlessWorkflowUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.events.EventDefinition;

@ApplicationScoped
public class SWFProcessFactory extends AbstractProcessFactory implements ProcessFactory {

    @Override
    public boolean accept(ProcessFile processFile) {
        return processFile.name().endsWith(".sw.json");
    }

    @Override
    public ProcessBuild createProcessDefinition(ProcessFile processFile) {
        try {
            Process processDefinition = getWorkflowParser(processFile);
            ProcessBuild processBuild = new ProcessBuild(processDefinition.getId());
            compileProcess(processBuild, processDefinition);
            BpmnProcess process = new BpmnProcess(processDefinition);
            processBuild.setProcess(process);
            return processBuild;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Process getWorkflowParser(ProcessFile processFile) throws JsonProcessingException {
        Workflow workflow = ServerlessWorkflowUtils.getObjectMapper("json").readValue(processFile.content(), Workflow.class);
        ServerlessWorkflowParser parser = ServerlessWorkflowParser.of(workflow);
        RuleFlowProcess process =  (RuleFlowProcess) parser.getProcess();
        for (Node node : process.getStartNodes()) {
            if(node.getMetaData().get(Metadata.TRIGGER_REF) != null) {
                EventDefinition eventDefinition = ServerlessWorkflowUtils.getWorkflowEventFor(workflow, node.getName());
                node.getMetaData().put("Event.Source", eventDefinition.getSource());
            }
        }
        for (Node node : process.getEndNodes()) {
            if(node.getMetaData().get(Metadata.TRIGGER_REF) != null) {
                EventDefinition eventDefinition = ServerlessWorkflowUtils.getWorkflowEventFor(workflow, node.getName());
                node.getMetaData().put("Event.Source", eventDefinition.getSource());
            }
        }
        process.setMetaData("ServerlessWorkflow", Boolean.TRUE);
        return process;
    }
}
