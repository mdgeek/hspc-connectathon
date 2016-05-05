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

import org.carewebframework.api.domain.IUser;
import org.carewebframework.api.security.SecurityUtil;

/**
 * A scheduled message.
 */
public class ScheduledMessage implements IMessageWrapper<ScheduledMessage> {
    
    
    private String id;
    
    private Date deliveryDate;
    
    private String subject;
    
    private String sender;
    
    private final Properties extraInfo = new Properties();
    
    public ScheduledMessage() {
        
    }
    
    public ScheduledMessage(String id, Date deliveryDate, String subject, String... extraInfo) {
        this.id = id;
        this.deliveryDate = deliveryDate;
        this.subject = subject;
        IUser user = SecurityUtil.getAuthenticatedUser();
        ;
        sender = user == null ? "" : user.getFullName();
        
        for (String info : extraInfo) {
            String[] pcs = info.split("\\=", 2);
            this.extraInfo.setProperty(pcs[0], pcs.length == 1 ? "" : pcs[1]);
        }
    }
    
    /**
     * Returns the id of the scheduled message.
     * 
     * @return The id. Will be null if this is a new message.
     */
    @Override
    public String getId() {
        return id;
    }
    
    /**
     * Sets the id of the message.
     * 
     * @param id The id. Will be null if this is a new message.
     */
    protected void setId(String id) {
        this.id = id;
    }
    
    /**
     * Returns the urgency of this message.
     * 
     * @return Urgency of the message.
     */
    @Override
    public Urgency getUrgency() {
        return Urgency.fromString(getParam("PRI"));
    }
    
    /**
     * Sets the urgency of this message.
     * 
     * @param urgency Urgency of the message.
     */
    public void setUrgency(Urgency urgency) {
        setParam("PRI", urgency.ordinal() + 1);
    }
    
    /**
     * Returns the logical id of the associated patient, or null if no associated patient.
     * 
     * @return Id of the associated patient, if any.
     */
    @Override
    public String getPatientId() {
        return getParam("patientId");
    }
    
    /**
     * Sets the logical id of the associated patient. Use null if no associated patient.
     * 
     * @param patientId Logical id of the associated patient.
     */
    public void setPatientId(String patientId) {
        setParam("patientId", patientId);
    }
    
    @Override
    public Date getDeliveryDate() {
        return deliveryDate;
    }
    
    public void setDeliveryDate(Date date) {
        this.deliveryDate = date;
    }
    
    @Override
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String value) {
        this.subject = value;
    }
    
    /**
     * Extract a parameter from name/value pairs in extra info.
     * 
     * @param param Parameter name.
     * @return Parameter value, or null if not found.
     */
    @Override
    public String getParam(String param) {
        return extraInfo.getProperty(param);
    }
    
    /**
     * Sets the value for a parameter in extra info.
     * 
     * @param param Parameter name.
     * @param value Parameter value (null to remove).
     */
    public void setParam(String param, Object value) {
        if (value == null) {
            extraInfo.remove(param);
        } else {
            extraInfo.setProperty(param, value.toString());
        }
    }
    
    @Override
    public boolean hasPatient() {
        return getPatientId() != null;
    }
    
    @Override
    public String getPatientName() {
        return getParam("patientName");
    }
    
    public void setPatientName(String value) {
        setParam("patientName", value);
    }
    
    @Override
    public String getDisplayText() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public boolean isActionable() {
        return false;
    }
    
    @Override
    public boolean canDelete() {
        return true;
    }
    
    @Override
    public ScheduledMessage getMessage() {
        return this;
    }
    
    @Override
    public String getAlertId() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String getType() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String getBody() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String getSender() {
        return sender;
    }
    
}
