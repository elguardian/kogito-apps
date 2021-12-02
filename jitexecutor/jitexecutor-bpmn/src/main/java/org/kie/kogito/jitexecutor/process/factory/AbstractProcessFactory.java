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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.drools.compiler.compiler.ReturnValueDescr;
import org.jbpm.compiler.canonical.ActionNodeVisitor;
import org.jbpm.process.builder.ProcessBuildContext;
import org.jbpm.process.builder.dialect.ProcessDialect;
import org.jbpm.process.builder.dialect.ProcessDialectRegistry;
import org.jbpm.process.core.context.variable.Variable;
import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.process.instance.impl.Action;
import org.jbpm.process.instance.impl.ReturnValueConstraintEvaluator;
import org.jbpm.process.instance.impl.ReturnValueEvaluator;
import org.jbpm.process.instance.impl.RuleConstraintEvaluator;
import org.jbpm.workflow.core.Constraint;
import org.jbpm.workflow.core.NodeContainer;
import org.jbpm.workflow.core.WorkflowProcess;
import org.jbpm.workflow.core.impl.ConnectionRef;
import org.jbpm.workflow.core.impl.ConstraintImpl;
import org.jbpm.workflow.core.impl.DroolsConsequenceAction;
import org.jbpm.workflow.core.impl.NodeImpl;
import org.jbpm.workflow.core.node.ActionNode;
import org.jbpm.workflow.core.node.CompositeContextNode;
import org.jbpm.workflow.core.node.Join;
import org.jbpm.workflow.core.node.Split;
import org.kie.api.definition.process.Connection;
import org.kie.api.definition.process.Node;
import org.kie.api.definition.process.Process;
import org.kie.api.runtime.process.ProcessContext;
import org.kie.kogito.expr.jsonpath.JsonPathParsedExpression;
import org.kie.kogito.internal.process.runtime.KogitoProcessContext;
import org.kie.kogito.jitexecutor.process.ProcessBuild;
import org.mvel2.ErrorDetail;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractProcessFactory {

    protected Process compileProcess(ProcessBuild processBuild, Process process) {
        VariableScope variableScope = (VariableScope) ((org.jbpm.process.core.Process) process).getDefaultContext(VariableScope.VARIABLE_SCOPE);
        compileNodes(processBuild, ((WorkflowProcess) process).getNodes(), variableScope);
        return process;
    }

    private void compileNodes(ProcessBuild processBuild, Node[] nodes, VariableScope variableScope) {
        for (Node node : nodes) {
            if (node instanceof ActionNode) {
                ActionNode actionNode = (ActionNode) node;
                if (actionNode.getAction() instanceof DroolsConsequenceAction) {
                    DroolsConsequenceAction action = (DroolsConsequenceAction) actionNode.getAction();
                    String dialect = action.getDialect();
                    if ("java".equals(dialect) || "mvel".equals(dialect)) {
                        String consequence = action.getConsequence();
                        final StringBuilder expression = new StringBuilder();
                        List<Variable> variables = variableScope.getVariables();
                        variables.stream()
                                .filter(v -> consequence.contains(v.getName()))
                                .map(ActionNodeVisitor::makeAssignment)
                                .forEach(x -> expression.append(x));
                        expression.append(consequence);
                        Serializable s = compileExpression(processBuild, expression.toString());
                        action.setMetaData("Action", (Action) kcontext -> {
                            Map<String, Object> vars = new HashMap<String, Object>();
                            vars.put("kcontext", kcontext);
                            MVEL.executeExpression(s, vars);
                        });
                    } else {
                        throw new IllegalArgumentException("Unsupported dialect found: " + dialect);
                    }
                }
            } else if (node instanceof NodeContainer) {
                NodeContainer nodeContainer = (NodeContainer) node;
                VariableScope scope = variableScope;
                if (node instanceof CompositeContextNode) {
                    if (((CompositeContextNode) node).getDefaultContext(VariableScope.VARIABLE_SCOPE) != null
                            && !((VariableScope) ((CompositeContextNode) node).getDefaultContext(VariableScope.VARIABLE_SCOPE)).getVariables().isEmpty()) {
                        scope = (VariableScope) ((CompositeContextNode) node).getDefaultContext(VariableScope.VARIABLE_SCOPE);
                    }
                }
                compileNodes(processBuild, nodeContainer.getNodes(), scope);
            } else if (node instanceof Split) {
                buildSplitNode(processBuild, (Split) node);
            } else if (node instanceof Join) {
                System.out.println("Join TO BE IMPLEMENTED");
            }
        }
    }

    private void buildSplitNode(ProcessBuild processBuild, Split splitNode) {
        // we need to clone the map, so we can update the original while iterating.
        Map<ConnectionRef, Constraint> map = new HashMap<ConnectionRef, Constraint>(splitNode.getConstraints());
        for (Iterator<Map.Entry<ConnectionRef, Constraint>> it = map.entrySet().iterator(); it.hasNext();) {
            Map.Entry<ConnectionRef, Constraint> entry = it.next();
            ConnectionRef connection = entry.getKey();
            ConstraintImpl constraint = (ConstraintImpl) entry.getValue();
            Connection outgoingConnection = null;
            for (Connection out : splitNode.getDefaultOutgoingConnections()) {
                if (out.getToType().equals(connection.getToType())
                        && out.getTo().getId() == connection.getNodeId()) {
                    outgoingConnection = out;
                }
            }
            if (outgoingConnection == null) {
                throw new IllegalArgumentException("Could not find outgoing connection");
            }
            if (constraint == null && splitNode.isDefault(outgoingConnection)) {
                // do nothing since conditions are ignored for default sequence flow
            } else if (constraint != null) {
                ReturnValueConstraintEvaluator contraintEvaluator = new ReturnValueConstraintEvaluator();
                contraintEvaluator.setDialect(constraint.getDialect());
                contraintEvaluator.setName(constraint.getName());
                contraintEvaluator.setPriority(constraint.getPriority());
                contraintEvaluator.setDefault(constraint.isDefault());
                contraintEvaluator.setType(constraint.getType());
                contraintEvaluator.setConstraint(constraint.getConstraint());
                splitNode.setConstraint(outgoingConnection, contraintEvaluator);
                String variable = (String) constraint.getMetaData("Variable");
                if (constraint.getDialect().equals("jsonpath")) {
                    contraintEvaluator.setEvaluator(new ReturnValueEvaluator() {
                        private JsonPathParsedExpression expr = new JsonPathParsedExpression(constraint.getConstraint());

                        @Override
                        public Object evaluate(KogitoProcessContext processContext) throws Exception {
                            Object data = processContext.getVariable(variable);
                            return expr.eval(data, Boolean.class);
                        }

                    });
                } else {
                    ProcessDialect dialect = ProcessDialectRegistry.getDialect(constraint.getDialect());
                    dialect.getReturnValueEvaluatorBuilder().build(null, contraintEvaluator, null, splitNode);
                }
            }
        }
    }

    private Serializable compileExpression(ProcessBuild processBuild, String expression) {
        ParserContext context = new ParserContext();
        context.addInput("kcontext", ProcessContext.class);
        Serializable result = MVEL.compileExpression(expression, context);
        for (ErrorDetail detail : context.getErrorList()) {
            processBuild.addError(detail.toString());
        }
        return result;
    }


    public static void main(String[] args) {
        try {
            // "$.[?(@.salary >= 3000)]";
            JsonPathParsedExpression expr = new JsonPathParsedExpression("$.[?(@.salary >= 3000)]");
            ObjectMapper mapper = new ObjectMapper();
            Object data = mapper.readTree("{ \"salary\" : 3500 } ");
            Object ok = expr.eval(data, Object.class);
            System.out.println(ok);
        } catch (JsonProcessingException e) {
            // do nothing
        }
    }
}
