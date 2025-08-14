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
package org.kie.kogito.app.jobs.jpa.hibernate;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgreSQLJsonPGObjectJsonbType;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.spi.JsonJavaType;
import org.hibernate.type.descriptor.jdbc.JsonAsStringJdbcType;
import org.hibernate.usertype.UserTypeSupport;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.vertx.core.json.JsonObject;

public class KogitoTypeContributor implements TypeContributor {

    @Override
    public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        JdbcServices jdbcServices = serviceRegistry.getService(JdbcServices.class);
        Dialect dialect = jdbcServices.getDialect();
        // only register if we have the postgresql support
        UserTypeSupport<ObjectNode> userTypeSupport = new JsonUserType();
        userTypeSupport.setTypeConfiguration(typeContributions.getTypeConfiguration());
        typeContributions.contributeType(userTypeSupport);

        // we tell the contributions how to map the json type to a column
        if (dialect instanceof PostgreSQLDialect) {
            typeContributions.getTypeConfiguration().getJdbcTypeRegistry().addDescriptor(SqlTypes.JSON, new PostgreSQLJsonPGObjectJsonbType());
        } else {
            JsonJavaType<JsonObject> jsonJavaType = new JsonJavaType<JsonObject>(
                    JsonObject.class,
                    new ImmutableMutabilityPlan<JsonObject>(),
                    typeContributions.getTypeConfiguration());
            typeContributions.getTypeConfiguration().getJdbcTypeRegistry().addDescriptor(SqlTypes.JSON, JsonAsStringJdbcType.NVARCHAR_INSTANCE);
            typeContributions.getTypeConfiguration().getJavaTypeRegistry().addDescriptor(jsonJavaType);
        }

    }

}
