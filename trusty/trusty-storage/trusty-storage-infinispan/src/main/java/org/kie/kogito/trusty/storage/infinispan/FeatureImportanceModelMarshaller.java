/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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
package org.kie.kogito.trusty.storage.infinispan;

import java.io.IOException;

import org.kie.kogito.trusty.storage.api.model.FeatureImportanceModel;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FeatureImportanceModelMarshaller extends AbstractModelMarshaller<FeatureImportanceModel> {

    public FeatureImportanceModelMarshaller(ObjectMapper mapper) {
        super(mapper, FeatureImportanceModel.class);
    }

    @Override
    public FeatureImportanceModel readFrom(ProtoStreamReader reader) throws IOException {
        return new FeatureImportanceModel(
                reader.readString(FeatureImportanceModel.FEATURE_NAME_FIELD),
                reader.readDouble(FeatureImportanceModel.SCORE_FIELD));
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, FeatureImportanceModel input) throws IOException {
        writer.writeString(FeatureImportanceModel.FEATURE_NAME_FIELD, input.getFeatureName());
        writer.writeDouble(FeatureImportanceModel.SCORE_FIELD, input.getScore());
    }
}
