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

public interface IMessageWrapper<T> {
    
    
    public String getId();
    
    boolean hasPatient();
    
    String getPatientName();
    
    String getPatientId();
    
    String getSubject();
    
    Date getDeliveryDate();
    
    String getDisplayText();
    
    boolean isActionable();
    
    Urgency getUrgency();
    
    boolean canDelete();
    
    T getMessage();
    
    String getAlertId();
    
    String getParam(String string);
    
    String getSender();
    
    String getType();
    
    public String getBody();
    
}
