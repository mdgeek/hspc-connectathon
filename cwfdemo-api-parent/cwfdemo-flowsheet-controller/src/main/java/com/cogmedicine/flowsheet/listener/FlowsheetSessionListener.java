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

import com.cogmedicine.flowsheet.bean.DesktopSession;
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
import java.util.HashMap;
import java.util.Map;

public class FlowsheetSessionListener implements HttpSessionListener {

    private static final Log log = LogFactory.getLog(FlowsheetSessionListener.class);

    public final static String DESKTOP_SESSION_MAP = "desktopSessionMap";
    public final static String HOST = "host";
    public final static String PORT = "port";
    public static final String PATIENT_CHANGE_EVENT = "CONTEXT.CHANGED.Patient";

    public void sessionCreated(HttpSessionEvent httpSessionEvent) {
        HttpSession httpSession = httpSessionEvent.getSession();
        httpSession.setAttribute(DESKTOP_SESSION_MAP, new HashMap<String, DesktopSession>());
    }

    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
        HttpSession httpSession = httpSessionEvent.getSession();
        destroyFlowsheetSessionValues(httpSession);
    }

    public static void destroyFlowsheetSessionValues(HttpSession httpSession) {
        Map<String, DesktopSession> desktopSessionMap = Utilities.getParameter(DESKTOP_SESSION_MAP, httpSession, Map.class);
        DesktopSession desktopSession;
        for (Map.Entry<String, DesktopSession> entry : desktopSessionMap.entrySet()) {
            desktopSession = entry.getValue();

            clearWebSocketSession(desktopSession);
            clearVitalSubscription(desktopSession);
        }

        desktopSessionMap.clear();

        httpSession.setAttribute(HOST, null);
        httpSession.setAttribute(PORT, null);
        httpSession.setAttribute(DESKTOP_SESSION_MAP, null);
    }

    public static void clearWebSocketSession(DesktopSession desktopSession) {
        WebSocketSession socketSession = desktopSession.getWebsocketSession();
        if (socketSession != null) {
            if (socketSession.isOpen()) {
                try {
                    socketSession.close();
                    desktopSession.setWebsocketSession(null);
                } catch (IOException e) {
                    log.error("Unable to close the web socket session");
                }
            }
        }
    }

    public static void clearVitalSubscription(DesktopSession desktopSession) {
        DataSubscription subscription = desktopSession.getVitalSubscription();
        if (subscription != null) {
            String subscriptionId = subscription.getSubscriptionId();
            if (subscriptionId != null && subscriptionId.isEmpty()) {
                if (subscription.isEnabled()) {
                    FhirServiceDstu3.removeResource(Subscription.class, subscription.getSubscriptionId());
                }
            }

            desktopSession.setVitalSubscription(null);
        }
    }

    public static String getNoDesktopIdMessage(HttpSession httpSession) {
        String host = Utilities.getParameter(FlowsheetSessionListener.HOST, httpSession, String.class);
        String port = Utilities.getParameter(FlowsheetSessionListener.PORT, httpSession, String.class);
        String servicePath = "/service/flowsheet/subscription/patientContext";
        String parameter = "?dtid={your-desktop-id}";
        String url = "http://" + host + ":" + port + httpSession.getServletContext().getContextPath() + servicePath + parameter;
        return "Register the context id first. Call: " + url;
    }
}