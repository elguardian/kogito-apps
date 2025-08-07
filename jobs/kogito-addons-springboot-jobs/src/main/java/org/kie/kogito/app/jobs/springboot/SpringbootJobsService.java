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
package org.kie.kogito.app.jobs.springboot;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.kie.kogito.app.jobs.api.JobExecutor;
import org.kie.kogito.app.jobs.api.JobScheduler;
import org.kie.kogito.app.jobs.api.JobSchedulerBuilder;
import org.kie.kogito.app.jobs.api.JobSchedulerListener;
import org.kie.kogito.app.jobs.api.JobTimeoutInterceptor;
import org.kie.kogito.app.jobs.integregations.ProcessInstanceJobDescriptionJobInstanceEventAdapter;
import org.kie.kogito.app.jobs.integregations.ProcessJobDescriptionJobInstanceEventAdapter;
import org.kie.kogito.app.jobs.integregations.UserTaskInstanceJobDescriptionJobInstanceEventAdapter;
import org.kie.kogito.app.jobs.spi.JobContextFactory;
import org.kie.kogito.app.jobs.spi.JobStore;
import org.kie.kogito.event.EventPublisher;
import org.kie.kogito.jobs.JobDescription;
import org.kie.kogito.jobs.JobsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
@Transactional
public class SpringbootJobsService implements JobsService {

    private static final Logger LOG = LoggerFactory.getLogger(SpringbootJobsService.class);

    protected JobScheduler jobScheduler;

    @Autowired(required = false)
    protected List<JobExecutor> jobExecutors;

    @Autowired(required = false)
    protected List<EventPublisher> eventPublisher;

    @Autowired(required = false)
    protected List<JobSchedulerListener> jobSchedulerListeners;

    @Autowired
    protected JobStore jobStore;

    @Autowired
    protected JobContextFactory jobContextFactory;

    @Value("${kogito.jobs-service.maxNumberOfRetries:3}")
    protected Integer maxNumberOfRetries;

    @Value("${kogito.jobs-service.maxIntervalLimitToRetryMillis:60000}")
    protected Long maxIntervalLimitToRetryMillis;

    @Value("${kogito.jobs-service.schedulerChunkInMinutes:10}")
    protected Long maxRefreshJobsIntervalWindow;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PostConstruct
    public void init() {
        JobTimeoutInterceptor txInterceptor = new JobTimeoutInterceptor() {

            @Override
            public Callable<Void> chainIntercept(Callable<Void> callable) {
                TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                return new Callable<Void>() {

                    @Override
                    public Void call() throws Exception {
                        return transactionTemplate.execute(status -> {
                            try {
                                return callable.call();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }

                };
            }
        };

        this.jobScheduler = JobSchedulerBuilder.newJobSchedulerBuilder()
                .withEventPublishers(ofNullable(eventPublisher).toArray(EventPublisher[]::new))
                .withJobSchedulerListeners(ofNullable(jobSchedulerListeners).stream().toArray(JobSchedulerListener[]::new))
                .withJobStore(jobStore)
                .withJobContextFactory(jobContextFactory)
                .withJobEventAdapters(
                        new ProcessInstanceJobDescriptionJobInstanceEventAdapter(),
                        new ProcessJobDescriptionJobInstanceEventAdapter(),
                        new UserTaskInstanceJobDescriptionJobInstanceEventAdapter())
                .withJobExecutors(ofNullable(jobExecutors).toArray(JobExecutor[]::new))
                .withMaxRefreshJobsIntervalWindow(maxRefreshJobsIntervalWindow * 60 * 1000L)
                .withRetryInterval(maxIntervalLimitToRetryMillis)
                .withMaxNumberOfRetries(maxNumberOfRetries)
                .withRefreshJobsInterval(maxRefreshJobsIntervalWindow * 60 * 1000L)
                .withTimeoutInterceptor(txInterceptor)
                .build();
        this.jobScheduler.init();

        LOG.info("Initializing Job Service Logic \n" +
                "MaxRefreshJobsIntervalWindow: {} (millis)\n" +
                "MaxIntervalLimitToRetryMillis: {} (millis)\n" +
                "MaxNumberOfRetries: {}\n" +
                "RefreshJobsInterval: {} (millis)\n" +
                "Store: {}",
                maxRefreshJobsIntervalWindow * 60 * 1000L,
                maxIntervalLimitToRetryMillis,
                maxNumberOfRetries,
                maxRefreshJobsIntervalWindow * 60 * 1000L,
                jobStore);
    }

    private <T> List<T> ofNullable(List<T> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }

    @PreDestroy()
    public void destroy() {
        this.jobScheduler.close();
    }

    @Override
    public String scheduleJob(JobDescription jobDescription) {
        return jobScheduler.schedule(jobDescription);
    }

    @Override
    public boolean cancelJob(String jobId) {
        jobScheduler.cancel(jobId);
        return true;
    }

    @Override
    public String rescheduleJob(JobDescription jobDescription) {
        return jobScheduler.reschedule(jobDescription);
    }

}
