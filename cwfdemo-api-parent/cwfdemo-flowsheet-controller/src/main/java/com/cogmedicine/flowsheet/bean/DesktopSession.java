/*
 * Copyright 2017 Cognitive Medical Systems, Inc (http://www.cognitivemedicine.com).
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Jeff Chung
 */

package com.cogmedicine.flowsheet.bean;

import com.cogmedicine.flowsheet.service.DataSubscription;
import org.carewebframework.api.event.IGenericEvent;
import org.springframework.web.socket.WebSocketSession;

public class DesktopSession {

    private String desktopId;
    private String patientId;
    private IGenericEvent patientChangeListener;
    private IGenericEvent userChangeListener;
    private DataSubscription vitalSubscription;
    private WebSocketSession websocketSession;

    public String getDesktopId() {
        return desktopId;
    }

    public void setDesktopId(String desktopId) {
        this.desktopId = desktopId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public IGenericEvent getPatientChangeListener() {
        return patientChangeListener;
    }

    public void setPatientChangeListener(IGenericEvent patientChangeListener) {
        this.patientChangeListener = patientChangeListener;
    }

    public IGenericEvent getUserChangeListener() {
        return userChangeListener;
    }

    public void setUserChangeListener(IGenericEvent userChangeListener) {
        this.userChangeListener = userChangeListener;
    }

    public DataSubscription getVitalSubscription() {
        return vitalSubscription;
    }

    public void setVitalSubscription(DataSubscription vitalSubscription) {
        this.vitalSubscription = vitalSubscription;
    }

    public WebSocketSession getWebsocketSession() {
        return websocketSession;
    }

    public void setWebsocketSession(WebSocketSession websocketSession) {
        this.websocketSession = websocketSession;
    }
}
