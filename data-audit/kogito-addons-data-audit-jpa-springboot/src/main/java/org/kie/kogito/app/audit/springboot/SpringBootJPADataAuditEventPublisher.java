/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kie.kogito.app.audit.springboot;

import java.util.Collection;

import javax.persistence.EntityManager;

import org.kie.kogito.app.audit.api.DataAuditContext;
import org.kie.kogito.app.audit.api.DataAuditEventPublisher;
import org.kie.kogito.app.audit.api.DataAuditStoreProxyService;
import org.kie.kogito.app.audit.jpa.JPADataAuditStore;
import org.kie.kogito.event.DataEvent;
import org.kie.kogito.event.process.ProcessInstanceDataEvent;
import org.kie.kogito.event.usertask.UserTaskInstanceDataEvent;
import org.kie.kogito.jobs.service.api.Job;
import org.kie.kogito.jobs.service.api.event.JobCloudEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SpringBootJPADataAuditEventPublisher implements DataAuditEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringBootJPADataAuditEventPublisher.class);

    private DataAuditStoreProxyService proxy;

    @Autowired
    EntityManager entityManager;

    public SpringBootJPADataAuditEventPublisher() {
        proxy = DataAuditStoreProxyService.newAuditStoreSerice(new JPADataAuditStore());
    }

    @Override
    public void publish(Collection<DataEvent<?>> events) {
        events.forEach(this::publish);
    }

    @Override
    @Transactional
    public void publish(DataEvent<?> event) {
        if (event instanceof ProcessInstanceDataEvent) {
            LOGGER.debug("Processing process instance event {}", event);
            proxy.storeProcessInstanceDataEvent(DataAuditContext.of(entityManager), (ProcessInstanceDataEvent<?>) event);
            return;
        } else if (event instanceof UserTaskInstanceDataEvent) {
            LOGGER.debug("Processing user task instacne event {}", event);
            proxy.storeUserTaskInstanceDataEvent(DataAuditContext.of(entityManager), (UserTaskInstanceDataEvent<?>) event);
            return;
        }

        LOGGER.debug("Discard event {} as class {} is not supported by this", event, event.getClass().getName());
    }

    @Override
    public void publish(JobCloudEvent<Job> event) {
        LOGGER.debug("Processing job event {}", event);
        proxy.storeJobDataEvent(DataAuditContext.of(entityManager), event);    
    }

}
