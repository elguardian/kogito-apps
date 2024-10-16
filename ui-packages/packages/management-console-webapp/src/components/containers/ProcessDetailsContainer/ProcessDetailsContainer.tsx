/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from 'react';
import { OUIAProps } from '@kogito-apps/components-common';
import { EmbeddedProcessDetails } from '@kogito-apps/process-details';
import { ProcessDetailsGatewayApi } from '../../../channel/ProcessDetails';
import { useProcessDetailsGatewayApi } from '../../../channel/ProcessDetails/ProcessDetailsContext';

interface ProcessDetailsContainerProps {
  processId: string;
}

const ProcessDetailsContainer: React.FC<ProcessDetailsContainerProps &
  OUIAProps> = ({ processId }) => {
  const gatewayApi: ProcessDetailsGatewayApi = useProcessDetailsGatewayApi();
  return (
    <EmbeddedProcessDetails
      driver={gatewayApi}
      targetOrigin={window.location.origin}
      processId={processId}
    />
  );
};

export default ProcessDetailsContainer;
