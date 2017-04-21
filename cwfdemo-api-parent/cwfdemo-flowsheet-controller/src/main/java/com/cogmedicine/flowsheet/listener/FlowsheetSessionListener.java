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

package com.cogmedicine.flowsheet.listener;

import com.cogmedicine.flowsheet.service.DataSubscription;
import com.cogmedicine.flowsheet.service.FhirServiceDstu3;
import com.cogmedicine.flowsheet.util.Utilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.dstu3.model.Subscription;
import org.springframework.web.socket.WebSocketSession;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.io.IOException;

public class FlowsheetSessionListener implements HttpSessionListener {

    private static final Log log = LogFactory.getLog(FlowsheetSessionListener.class);

    public final static String VITAL_SUBSCRIPTION = "vitalSubscription";
    public final static String PATIENT_CHANGE_LISTENER = "patientChangeListener";
    public final static String WEB_SOCKET_SESSION = "webSocketSession";

    public final static String PATIENT_ID = "patientId";
    public final static String DESKTOP_ID = "desktopId";
    public final static String HOST = "host";
    public final static String PORT = "port";
    public static final String PATIENT_CHANGE_EVENT = "CONTEXT.CHANGED.Patient";

    public void sessionCreated(HttpSessionEvent httpSessionEvent) {
        log.debug("session created ...");
    }

    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
        HttpSession httpSession = httpSessionEvent.getSession();
        destroyFlowsheetSessionValues(httpSession);
    }

    public static void destroyFlowsheetSessionValues(HttpSession httpSession){
        clearVitalSubscription(httpSession);
        //patient change listener will be set to null automatically by Careweb when the session is destroyed
        //clearPatientChangeListener(httpSession);
        clearWebSocketSession(httpSession);

        httpSession.setAttribute(PATIENT_ID, null);
        httpSession.setAttribute(DESKTOP_ID, null);
        httpSession.setAttribute(HOST, null);
        httpSession.setAttribute(PORT, null);
    }

    public static void clearWebSocketSession(HttpSession httpSession) {
        WebSocketSession socketSession = Utilities.getParameter(WEB_SOCKET_SESSION, httpSession, WebSocketSession.class);
        if (socketSession != null) {
            if (socketSession.isOpen()) {
                try {
                    socketSession.close();
                    httpSession.setAttribute(WEB_SOCKET_SESSION, null);
                } catch (IOException e) {
                    log.error("Unable to close the web socket session");
                }
            }
        }
    }

    public static void clearVitalSubscription(HttpSession httpSession) {
        DataSubscription subscription = Utilities.getParameter(VITAL_SUBSCRIPTION, httpSession, DataSubscription.class);
        if (subscription != null) {
            String subscriptionId = subscription.getSubscriptionId();
            if (subscriptionId != null && subscriptionId.isEmpty()) {
                if (subscription.isEnabled()) {
                    FhirServiceDstu3.removeResource(Subscription.class, subscription.getSubscriptionId());
                }
            }
            httpSession.setAttribute(VITAL_SUBSCRIPTION, null);
        }
    }
}
