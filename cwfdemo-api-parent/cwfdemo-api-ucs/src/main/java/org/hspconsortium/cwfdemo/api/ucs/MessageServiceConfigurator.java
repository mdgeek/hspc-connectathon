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
package org.hspconsortium.cwfdemo.api.ucs;

import org.springframework.beans.factory.annotation.Value;

public class MessageServiceConfigurator {
    
    
    /**
     * The host where ucs-nifi is running.
     */
    @Value("${org.socraticgrid.hl7.ucs.nifiHost}")
    private String nifiHost;
    
    /**
     * The port where ucs-nifi has registered its send message interface.
     */
    @Value("${org.socraticgrid.hl7.ucs.nifiSendMessageCommandPort:8888}")
    private int nifiSendMessageCommandPort;
    
    /**
     * The port where ucs-nifi has registered its client interface.
     */
    @Value("${org.socraticgrid.hl7.ucs.nifiClientCommandPort:8889}")
    private int nifiClientCommandPort;
    
    /**
     * The port where ucs-nifi has registered its alerting interface.
     */
    @Value("${org.socraticgrid.hl7.ucs.nifiAlertingCommandPort:8890}")
    private int nifiAlertingCommandPort;
    
    /**
     * The port where ucs-nifi has registered its management interface.
     */
    @Value("${org.socraticgrid.hl7.ucs.nifiManagementCommandPort:8891}")
    private int nifiManagementCommandPort;
    
    /**
     * The port where ucs-nifi has registered its conversation interface.
     */
    @Value("${org.socraticgrid.hl7.ucs.nifiConversationCommandPort:8892}")
    private int nifiConversationCommandPort;
    
    /**
     * Host that ucs-nifi will use to communicate to the client. This host must be accessible by
     * ucs-nifi.
     */
    @Value("${org.socraticgrid.hl7.ucs.clientHost:localhost}")
    private String clientHost;
    
    @Value("${org.socraticgrid.hl7.ucs.clientPort}")
    private int clientPort;
    
    @Value("${org.socraticgrid.hl7.ucs.alertingPort}")
    private int alertingPort;
    
    @Value("${org.socraticgrid.hl7.ucs.managementPort}")
    private int managementPort;
    
    @Value("${org.socraticgrid.hl7.ucs.conversationPort}")
    private int conversationPort;
    
    public MessageServiceConfigurator() {
    }
    
    public String getNifiHost() {
        return nifiHost;
    }
    
    public int getNifiClientCommandPort() {
        return nifiClientCommandPort;
    }
    
    public int getNifiAlertingCommandPort() {
        return nifiAlertingCommandPort;
    }
    
    public int getNifiSendMessageCommandPort() {
        return nifiSendMessageCommandPort;
    }
    
    public int getNifiManagementCommandPort() {
        return nifiManagementCommandPort;
    }
    
    public int getNifiConversationCommandPort() {
        return nifiConversationCommandPort;
    }
    
    public String getClientHost() {
        return clientHost;
    }
    
    public int getClientPort() {
        return clientPort;
    }
    
    public int getAlertingPort() {
        return alertingPort;
    }
    
    public int getManagementPort() {
        return managementPort;
    }
    
    public int getConversationPort() {
        return conversationPort;
    }
    
}
