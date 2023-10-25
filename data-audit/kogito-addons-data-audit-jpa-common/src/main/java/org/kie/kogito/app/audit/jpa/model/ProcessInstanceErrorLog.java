/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates.
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

package org.kie.kogito.app.audit.jpa.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.kogito.app.audit.jpa.model.ModelConstants.ERROR_LOG_LENGTH;

@Entity
@Table(name = "ProcessInstanceErrorLog")
@SequenceGenerator(name = "processInstanceErrorHistorySeq", sequenceName = "PROCESS_INSTANCE_ERROR_LOG_SEQ_ID")
public class ProcessInstanceErrorLog extends AbstractProcessInstanceLog {

    @Transient
    private static final Logger logger = LoggerFactory.getLogger(ProcessInstanceErrorLog.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "processInstanceErrorHistorySeq")
    private Long id;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "node_definition_id")
    private String nodeDefinitionId;

    @Column(name = "node_instance_id")
    private String nodeInstanceId;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        String trimmedErrorMessage = errorMessage;
        if (trimmedErrorMessage != null && trimmedErrorMessage.length() > ERROR_LOG_LENGTH) {
            trimmedErrorMessage = trimmedErrorMessage.substring(0, ERROR_LOG_LENGTH);
            logger.warn("Error message content was trimmed as it was too long (more than {} characters)", ERROR_LOG_LENGTH);
        }
        this.errorMessage = trimmedErrorMessage;
    }

    public String getNodeDefinitionId() {
        return nodeDefinitionId;
    }

    public void setNodeDefinitionId(String nodeDefinitionId) {
        this.nodeDefinitionId = nodeDefinitionId;
    }

    public String getNodeInstanceId() {
        return nodeInstanceId;
    }

    public void setNodeInstanceId(String nodeInstanceId) {
        this.nodeInstanceId = nodeInstanceId;
    }

}