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

import java.util.Date;
import java.util.Properties;

import org.socraticgrid.hl7.services.uc.model.BaseAddress;
import org.socraticgrid.hl7.services.uc.model.DeliveryAddress;
import org.socraticgrid.hl7.services.uc.model.Message;
import org.socraticgrid.hl7.services.uc.model.MessageBody;

public class MessageWrapper implements IMessageWrapper<Message> {
    
    
    private final Message message;
    
    public MessageWrapper(Message message) {
        this.message = message;
    }
    
    @Override
    public boolean hasPatient() {
        return getPatientId() != null;
    }
    
    @Override
    public String getPatientName() {
        return getParam(MessageProperty.MESSAGE_ABOUT_DISPLAY);
    }
    
    @Override
    public String getPatientId() {
        return getParam(MessageProperty.MESSAGE_ABOUT_ID);
    }
    
    @Override
    public String getSender() {
        DeliveryAddress sender = message.getHeader().getSender();
        BaseAddress address = sender == null ? null : sender.getAddress();
        String s = address == null ? "" : address.toString();
        int i = s.indexOf("=") + 1;
        int j = s.indexOf("]", i);
        return i == 0 ? s : j == -1 ? s.substring(i) : s.substring(i, j);
    }
    
    @Override
    public String getSubject() {
        return message.getHeader().getSubject();
    }
    
    @Override
    public Date getDeliveryDate() {
        return message.getHeader().getLastModified();
    }
    
    @Override
    public String getDisplayText() {
        return message.getHeader().getSubject();
    }
    
    @Override
    public boolean isActionable() {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public Urgency getUrgency() {
        String urgency = getParam("urgency");
        return urgency != null ? Urgency.fromString(urgency) : Urgency.LOW;
    }
    
    @Override
    public boolean canDelete() {
        return true;
    }
    
    @Override
    public Message getMessage() {
        return message;
    }
    
    @Override
    public String getAlertId() {
        return null;
    }
    
    private String getParam(MessageProperty property) {
        return getParam(property.name());
    }
    
    @Override
    public String getParam(String param) {
        Properties properties = message.getHeader().getProperties();
        return properties == null ? null : properties.getProperty(param);
    }
    
    @Override
    public String getType() {
        return null;
    }
    
    @Override
    public String getBody() {
        StringBuilder sb = new StringBuilder();
        
        for (MessageBody body : message.getParts()) {
            sb.append(body.getContent());
        }
        
        return sb.toString();
    }
    
    @Override
    public String getId() {
        return message.getHeader().getMessageId();
    }
    
}
