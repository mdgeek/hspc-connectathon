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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.ws.BindingProvider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.carewebframework.api.thread.ThreadUtil;
import org.carewebframework.common.MiscUtil;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.socraticgrid.hl7.services.eps.accessclients.broker.BrokerServiceSE;
import org.socraticgrid.hl7.services.eps.accessclients.publication.PublicationServiceSE;
import org.socraticgrid.hl7.services.eps.accessclients.subscription.SubscriptionServiceSE;
import org.socraticgrid.hl7.services.eps.interfaces.BrokerIFace;
import org.socraticgrid.hl7.services.eps.interfaces.PublicationIFace;
import org.socraticgrid.hl7.services.eps.interfaces.SubscriptionIFace;
import org.socraticgrid.hl7.services.eps.model.AccessModel;
import org.socraticgrid.hl7.services.eps.model.Durability;
import org.socraticgrid.hl7.services.eps.model.Message;
import org.socraticgrid.hl7.services.eps.model.MessageBody;
import org.socraticgrid.hl7.services.eps.model.MessageHeader;
import org.socraticgrid.hl7.services.eps.model.Options;
import org.socraticgrid.hl7.services.eps.model.PullRange;
import org.socraticgrid.hl7.services.eps.model.SubscriptionType;
import org.socraticgrid.hl7.services.eps.model.User;

import ca.uhn.fhir.context.FhirContext;

/**
 *
 */
public class EPSService {
    
    
    public interface IEventCallback {
        
        
        void onEvent(Message event);
    }
    
    private class EventPoller extends Thread {
        
        
        private final Object monitor = new Object();
        
        private boolean terminate;
        
        /**
         * Wakes up the background thread.
         *
         * @return True if request was successful.
         */
        public synchronized boolean wakeup() {
            try {
                synchronized (monitor) {
                    monitor.notify();
                }
                return true;
            } catch (Throwable t) {
                return false;
            }
        }
        
        public void terminate() {
            terminate = true;
            wakeup();
        }
        
        @Override
        public void run() {
            synchronized (monitor) {
                while (!terminate) {
                    try {
                        pollEvents();
                        monitor.wait(pollingInterval);
                    } catch (InterruptedException e) {}
                }
            }
            
            log.debug("Event poller has exited.");
        }
        
    }
    
    private class Subscription {
        
        
        private final Set<IEventCallback> callbacks = new HashSet<>();
        
        private final List<String> topic;
        
        private String subscriptionId;
        
        Subscription(String topic) {
            this.topic = Collections.singletonList(topic);
        }
        
        public synchronized boolean subscribe(IEventCallback callback) {
            if (subscriptionId == null) {
                subscribe();
            }
            
            return callbacks.add(callback);
        }
        
        public synchronized boolean unsubscribe(IEventCallback callback) {
            boolean result = callbacks.remove(callback);
            
            if (callbacks.isEmpty() && subscriptionId != null) {
                unsubscribe();
            }
            
            return result;
        }
        
        public boolean hasSubscribers() {
            return !callbacks.isEmpty();
        }
        
        public void deliverEvent(Message event) {
            Set<IEventCallback> cbs;
            
            synchronized (callbacks) {
                cbs = new HashSet<>(callbacks);
            }
            
            for (IEventCallback callback : cbs) {
                try {
                    callback.onEvent(event);
                } catch (Throwable e) {
                    log.error("Error during event delivery.", e);
                }
            }
        }
        
        private void subscribe() {
            Options options = new Options();
            options.setAccess(AccessModel.Open);
            options.setDurability(Durability.Transient);
            
            try {
                subscriptionId = getSubscriberPort().subscribe(topic, SubscriptionType.Pull, options, null);
            } catch (Exception e) {
                throw MiscUtil.toUnchecked(e);
            }
        }
        
        private void unsubscribe() {
            try {
                getSubscriberPort().unsubscribe(topic, subscriberId, subscriptionId);
            } catch (Exception e) {
                throw MiscUtil.toUnchecked(e);
            } finally {
                subscriptionId = null;
            }
        }
    }
    
    private static final Log log = LogFactory.getLog(EPSService.class);
    
    private final String subscriberId = UUID.randomUUID().toString();
    
    private final FhirContext fhirContext;
    
    private String serviceEndpoint;
    
    private User publisher;
    
    private PublicationIFace publisherPort;
    
    private SubscriptionIFace subscriberPort;
    
    private BrokerIFace brokerPort;
    
    private Date lastPoll = new Date();
    
    private final int pollingInterval = 5000;
    
    private final EventPoller eventPoller = new EventPoller();
    
    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    
    public EPSService(FhirContext fhirContext, String serviceEndpoint) {
        this.fhirContext = fhirContext;
        this.serviceEndpoint = serviceEndpoint;
    }
    
    public void init() {
        if (!serviceEndpoint.endsWith("/")) {
            serviceEndpoint += "/";
        }
        
        PublicationServiceSE ps = new PublicationServiceSE();
        publisherPort = ps.getPublicationPort();
        ((BindingProvider) publisherPort).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
            serviceEndpoint + "publication");
        
        SubscriptionServiceSE ss = new SubscriptionServiceSE();
        subscriberPort = ss.getSubscriptionPort();
        ((BindingProvider) subscriberPort).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
            serviceEndpoint + "subscriptionService");
        
        BrokerServiceSE bs = new BrokerServiceSE();
        brokerPort = bs.getBrokerPort();
        ((BindingProvider) brokerPort).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
            serviceEndpoint + "broker");
        
        ThreadUtil.startThread(eventPoller);
    }
    
    public void destroy() {
        eventPoller.terminate();
    }
    
    public FhirContext getFhirContext() {
        return fhirContext;
    }
    
    public PublicationIFace getPublisherPort() {
        return publisherPort;
    }
    
    public SubscriptionIFace getSubscriberPort() {
        return subscriberPort;
    }
    
    public BrokerIFace getBrokerPort() {
        return brokerPort;
    }
    
    //***************************** Publication *****************************
    
    public User getPublisher() {
        return publisher;
    }
    
    /**
     * Raw event publication
     * 
     * @param topic Topic for publication.
     * @param event The event to be published.
     * @return The event id.
     */
    public String publishEvent(String topic, Message event) {
        if (event.getTopics().indexOf(topic) == -1) {
            event.getTopics().add(topic);
        }
        
        try {
            return publisherPort.publishEvent(topic, event);
        } catch (Exception e) {
            throw MiscUtil.toUnchecked(e);
        }
    }
    
    /**
     * Raw event publication driven by topics declared in the message
     * 
     * @param event The event to be published.
     * @return A map of event id's keyed by topic.
     */
    public Map<String, String> publishEvent(Message event) {
        Map<String, String> msgIds = new HashMap<>();
        
        for (String topic : event.getTopics()) {
            msgIds.put(topic, publishEvent(topic, event));
        }
        
        return msgIds;
    }
    
    /**
     * Compose and publish an event on a topic
     * 
     * @param topic Topic for publication.
     * @param data Data to be published.
     * @param contentType The content type of the data.
     * @param subject Event subject.
     * @param title Event title.
     * @return The event id.
     */
    public String publishEvent(String topic, String data, String contentType, String subject, String title) {
        Date now = new Date();
        
        Message event = new Message();
        
        MessageHeader header = event.getHeader();
        header.setMessageId(UUID.randomUUID().toString()); // Not sure we need to generate an Id
        header.setTopicId(topic);
        header.setSubject(subject);
        header.setMessageCreatedTime(now);
        header.setMessagePublicationTime(now);
        header.setPublisher(publisher);
        event.setTitle(title);
        
        MessageBody body = new MessageBody();
        body.setType(contentType);
        body.setBody(data);
        event.getMessageBodies().add(body);
        
        return publishEvent(topic, event);
    }
    
    /**
     * Publish a FHIR resource to a topic (as JSON) using a default subject & title
     * 
     * @param topic Topic for publication.
     * @param resource The FHIR resource to be published.
     * @return The event id.
     */
    public String publishResourceToTopic(String topic, IBaseResource resource) {
        return publishResourceToTopic(topic, resource, "FHIR Resource", resource.getClass().getName());
    }
    
    /**
     * Publish a FHIR resource to a topic, proving a title and subject
     * 
     * @param topic Topic for publication.
     * @param resource The FHIR resource to be published.
     * @param subject Event subject.
     * @param title Event title.
     * @return The event id.
     */
    public String publishResourceToTopic(String topic, IBaseResource resource, String subject, String title) {
        String data = fhirContext.newJsonParser().encodeResourceToString(resource);
        return publishEvent(topic, data, "application/json+fhir", subject, title);
    }
    
    //**************************** Subscription *****************************
    
    private Subscription getSubscription(String topic, boolean forceCreate) {
        Subscription subscription = subscriptions.get(topic);
        
        if (subscription == null && forceCreate) {
            synchronized (subscriptions) {
                subscriptions.put(topic, subscription = new Subscription(topic));
            }
        }
        
        return subscription;
    }
    
    public boolean subscribe(String topic, IEventCallback callback) {
        Subscription subscription = getSubscription(topic, true);
        return subscription.subscribe(callback);
    }
    
    public boolean unsubscribe(String topic, IEventCallback callback) {
        Subscription subscription = getSubscription(topic, false);
        return subscription != null && subscription.unsubscribe(callback);
    }
    
    private void pollEvents() {
        Date start = lastPoll;
        Date end = new Date();
        lastPoll = end;
        
        for (Entry<String, Subscription> entry : subscriptions.entrySet()) {
            try {
                if (!entry.getValue().hasSubscribers()) {
                    continue;
                }
                
                List<Message> events = getSubscriberPort().retrieveEvents(entry.getKey(), PullRange.Specific, start, end,
                    Collections.<String> emptyList());
                
                for (Message event : events) {
                    entry.getValue().deliverEvent(event);
                }
            } catch (Exception e) {
                log.error("Exception while polling for events.", e);
            }
        }
    }
    
}
