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

package org.kie.kogito.jitexecutor.process.api;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.kie.kogito.jitexecutor.api.responses.ListIdentifierResponse;
import org.kie.kogito.jitexecutor.process.JITProcessService;

@Path("/process/{processId}/instance/{processInstanceId}")
public class ProcessInstanceResource {

    @Inject
    JITProcessService jitProcessService;

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response listVariables(@PathParam("processId") String processId, @PathParam("processInstanceId") String processInstanceId) {
        return Response.ok(jitProcessService.getProcessInstanceState(processId, processInstanceId)).build();
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{processId}/")
    public Response listProcessInstanceByProcessId(String processId) {
        return Response.ok(new ListIdentifierResponse(jitProcessService.listProcessInstanceIdentifiers(processId))).build();
    }



    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response abortProcessInstanceByProcessId(@PathParam("processId") String processId, @PathParam("processInstanceId") String processInstanceId) {
        jitProcessService.abortProcessInstance(processId, processInstanceId);
        return Response.ok().build();
    }

}
