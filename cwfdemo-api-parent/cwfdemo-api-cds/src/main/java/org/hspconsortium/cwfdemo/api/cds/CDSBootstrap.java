/*
 * #%L
 * EPS API
 * %%
 * Copyright (C) 2014 - 2016 Healthcare Services Platform Consortium
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.hspconsortium.cwfdemo.api.cds;

import com.cognitivemedicine.hl7.cds.CDSSystem;
import com.cognitivemedicine.hl7.cds.communication.CommunicationServiceClient;
import com.cognitivemedicine.hl7.cds.communication.ucs.UCSClient;
import com.cognitivemedicine.hl7.cds.datasource.FHIRDataSource;
import com.cognitivemedicine.hl7.cds.datasource.hapi.FHIRHapiDataSource;
import com.cognitivemedicine.hl7.cds.evaluate.EvaluateOperationExecutor;
import com.cognitivemedicine.hl7.cds.evaluate.hapi.HapiEvaluateOperationExecutor;
import com.cognitivemedicine.hl7.cds.pubsub.eps.EPSClient;

/**
 *
 */
public class CDSBootstrap {
    
    
    private final CDSConfigurator config;
    
    private CDSSystem cds;
    
    public CDSBootstrap(CDSConfigurator config) {
        this.config = config;
    }
    
    public void init() throws Exception {
        String fhirServerURL = config.getFhirServiceRoot();
        String fhirEvaluateURL = config.getFhirEvaluateRoot();
        
        String ucsServerURL = config.getUcsServerUrl();
        String ucsClientURL = config.getUcsClientUrl();
        int ucsClientPort = config.getUcsClientPort();
        int ucsAlertingPort = config.getUcsAlertingPort();
        
        String epsEndpoint = config.getEpsEndpoint();
        String epsUser = config.getEpsUser();
        String epsPassword = config.getEpsPassword();
        long epsPollTime = config.getEpsPollingInterval();
        
        String cdsTopic = config.getCdsTopic();
        
        FHIRDataSource dataSource = new FHIRHapiDataSource(fhirServerURL);
        EPSClient pubSubClient = EPSClient.getInstance();
        pubSubClient.init(epsEndpoint, epsUser, epsPassword, epsPollTime);
        EvaluateOperationExecutor evaluateOperationExecutor = new HapiEvaluateOperationExecutor(fhirEvaluateURL);
        CommunicationServiceClient communicationServiceClient = new UCSClient(ucsServerURL, ucsClientPort, ucsAlertingPort,
                ucsClientURL);
        
        cds = new CDSSystem(dataSource, pubSubClient, evaluateOperationExecutor, communicationServiceClient);
        cds.start(cdsTopic);
    }
    
    public void destroy() {
        cds.stop();
    }
}
