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
package org.hspconsortium.cwfdemo.api.eps;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.carewebframework.api.event.AbstractGlobalEventDispatcher;
import org.carewebframework.common.JSONUtil;
import org.carewebframework.common.StrUtil;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hspconsortium.cwfdemo.api.eps.EPSService.IEventCallback;
import org.socraticgrid.hl7.services.eps.model.Message;

public class GlobalEventDispatcher extends AbstractGlobalEventDispatcher {
    
    
    private static final Log log = LogFactory.getLog(GlobalEventDispatcher.class);
    
    private final EPSService epsService;
    
    private final String nodeId = UUID.randomUUID().toString();
    
    private final Map<String, Integer> subscriptions = new ConcurrentHashMap<>();
    
    private final IEventCallback eventCallback = new IEventCallback() {
        
        
        @Override
        public void onEvent(Message event) {
            processEvent(event);
        }
        
    };
    
    public GlobalEventDispatcher(EPSService epsService) {
        this.epsService = epsService;
    }
    
    @Override
    protected String getNodeId() {
        return nodeId;
    }
    
    private int subscriptionCount(String topic, int increment) {
        synchronized (subscriptions) {
            Integer count = subscriptions.get(topic);
            
            if (count == null) {
                subscriptions.put(topic, count = new Integer(0));
            }
            
            count += increment;
            count = count < 0 ? 0 : count;
            return count;
        }
    }
    
    @Override
    public void subscribeRemoteEvent(String eventName, boolean subscribe) {
        String topic = StrUtil.piece(eventName, ".");
        
        if (subscribe) {
            subscriptionCount(topic, 1);
            epsService.subscribe(topic, eventCallback);
        } else if (subscriptionCount(topic, -1) == 0) {
            epsService.unsubscribe(topic, eventCallback);
        }
    }
    
    @Override
    public void fireRemoteEvent(String eventName, Serializable eventData, String recipients) {
        String topic = StrUtil.piece(eventName, ".");
        
        if (eventData instanceof IBaseResource) {
            epsService.publishResourceToTopic(topic, (IBaseResource) eventData, eventName, "FHIR");
            return;
        }
        
        String contentType;
        String data;
        String encoding;
        
        if (eventData == null || eventData instanceof String) {
            encoding = "NONE";
            contentType = "text/plain";
            data = (String) eventData;
        } else {
            encoding = "JSON";
            contentType = "application/json";
            data = JSONUtil.serialize(eventData);
        }
        
        epsService.publishEvent(topic, data, contentType, eventName, encoding);
    }
    
    /**
     * Process a dequeued event by forwarding it to the local event manager for local delivery. If
     * the message is a ping request, send the response.
     * 
     * @param event Event to process.
     */
    private void processEvent(Message event) {
        try {
            String eventName = event.getHeader().getSubject();
            String encoding = event.getTitle();
            String body = event.getMessageBodies().get(0).getBody();
            Object eventData;
            
            if ("FHIR".equalsIgnoreCase(encoding)) {
                eventData = epsService.getFhirContext().newJsonParser().parseResource(body);
            } else if ("JSON".equalsIgnoreCase(encoding)) {
                eventData = JSONUtil.deserialize(body);
            } else {
                eventData = body;
            }
            
            localEventDelivery(eventName, eventData);
        } catch (Exception e) {
            log.error("Error during local dispatch of global event.", e);
        }
    }
    
}
