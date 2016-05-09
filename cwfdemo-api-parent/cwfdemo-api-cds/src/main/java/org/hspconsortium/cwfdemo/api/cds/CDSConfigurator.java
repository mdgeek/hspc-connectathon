/*
 * #%L
 * UCS Messaging API
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

import org.springframework.beans.factory.annotation.Value;

public class CDSConfigurator {
    
    
    /**
     * The host where ucs-nifi is running.
     */
    @Value("${fhir.service.root.url}")
    private String fhirServiceRoot;
    
    @Value("${fhir.service.evaluate.url:}")
    private String fhirEvaluateRoot;
    
    @Value("${org.socraticgrid.hl7.ucs.nifiHost}")
    private String ucsServerUrl;
    
    @Value("${org.socraticgrid.hl7.ucs.clientHost}")
    private String ucsClientUrl;
    
    @Value("${org.socraticgrid.hl7.ucs.nifiClientCommandPort}")
    private int ucsClientPort;
    
    @Value("${org.socraticgrid.hl7.ucs.nifiAlertingCommandPort}")
    private int ucsAlertingPort;
    
    @Value("${org.socraticgrid.hl7.eps.endpoint}")
    private String epsEndpoint;
    
    @Value("${org.socraticgrid.hl7.eps.user}")
    private String epsUser;
    
    @Value("${org.socraticgrid.hl7.eps.password}")
    private String epsPassword;
    
    @Value("${org.socraticgrid.hl7.eps.polling}")
    private int epsPollingInterval;
    
    @Value("${org.socraticgrid.hl7.cds.topic}")
    private String cdsTopic;
    
    public String getFhirServiceRoot() {
        return fhirServiceRoot;
    }
    
    public String getFhirEvaluateRoot() {
        return fhirEvaluateRoot.isEmpty() ? fhirServiceRoot : fhirEvaluateRoot;
    }
    
    public String getUcsServerUrl() {
        return ucsServerUrl;
    }
    
    public String getUcsClientUrl() {
        return ucsClientUrl;
    }
    
    public int getUcsClientPort() {
        return ucsClientPort;
    }
    
    public int getUcsAlertingPort() {
        return ucsAlertingPort;
    }
    
    public String getEpsEndpoint() {
        return epsEndpoint;
    }
    
    public String getEpsUser() {
        return epsUser;
    }
    
    public String getEpsPassword() {
        return epsPassword;
    }
    
    public int getEpsPollingInterval() {
        return epsPollingInterval;
    }
    
    public String getCdsTopic() {
        return cdsTopic;
    }
    
}
