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
package com.cogmedicine.flowsheet.controller;

import com.cogmedicine.flowsheet.bean.DesktopSession;
import com.cogmedicine.flowsheet.listener.FlowsheetSessionListener;
import com.cogmedicine.flowsheet.service.DataSubscription;
import com.cogmedicine.flowsheet.service.SubscriptionServiceDstu3;
import com.cogmedicine.flowsheet.socket.DesktopIdSocketHandler;
import com.cogmedicine.flowsheet.util.Utilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carewebframework.api.event.EventManager;
import org.carewebframework.api.event.IGenericEvent;
import org.carewebframework.ui.util.RequestUtil;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.web.socket.WebSocketSession;
import org.zkoss.zk.ui.ComponentNotFoundException;
import org.zkoss.zkplus.embed.Bridge;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("/subscription")
public class FlowsheetSubscriptionControllerDstu3 {

    private static final Log log = LogFactory.getLog(FlowsheetSubscriptionControllerDstu3.class);

    private SubscriptionServiceDstu3 subscriptionServiceDstu3;
    public static final String WEBSOCKET_URL_PATH = "/flowsheetSubscription/socket";

    public FlowsheetSubscriptionControllerDstu3() {
        subscriptionServiceDstu3 = new SubscriptionServiceDstu3();
    }

    /**
     * Creates a patient subscription and returns the websocket connection url
     * Typically used for clients that poll for data
     *
     * @param desktopId
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/patientContext")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPatientContextSubscription(
            @QueryParam("dtid") String desktopId,
            @Context HttpServletRequest request,
            @Context HttpServletResponse response) {
        if (desktopId == null || desktopId.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("dtid is a required parameter").build();
        }

        HttpSession httpSession = request.getSession();
        Map<String, DesktopSession> desktopSessionMap = Utilities.getParameter(FlowsheetSessionListener.DESKTOP_SESSION_MAP, httpSession, Map.class);
        if (!desktopSessionMap.containsKey(desktopId)) {
            Bridge bridge = null;

            try {
                bridge = RequestUtil.startExecution(request, response, desktopId);
                if (bridge == null) {
                    throw new IllegalArgumentException("Unable to create the bridge with desktop id: " + desktopId);
                }

                DesktopSession desktopSession = new DesktopSession();
                desktopSession.setDesktopId(desktopId);

                addPatientContextListener(desktopSession);
                //addUserContextListener(desktopSession);

                desktopSessionMap.put(desktopId, desktopSession);
            } catch (ComponentNotFoundException e) {
                e.printStackTrace();
                log.error(e.getMessage());
                String message = "Invalid desktop id, desktop not found: " + desktopId;
                return Response.status(Response.Status.BAD_REQUEST).entity(message).build();
            } finally {
                if (bridge != null) {
                    bridge.close();
                }
            }
        }

        return Response.ok(getWebsocketUrl(request, desktopId)).build();
    }

    /**
     * Creates a vitals subscription and a patient context subscription and returns the websocket connection url
     * Typically used for clients that need to be notified of data changes.
     *
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/vitals")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVitalsSubscription(
            @QueryParam("dtid") String desktopId,
            @QueryParam("tminus") String tminus,
            @Context HttpServletRequest request,
            @Context HttpServletResponse response) {
        if (desktopId == null || desktopId.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("dtid is a required parameter").build();
        }
        if (tminus == null || tminus.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("tminus is a required parameter").build();
        }

        HttpSession httpSession = request.getSession();
        Map<String, DesktopSession> desktopSessionMap = Utilities.getParameter(FlowsheetSessionListener.DESKTOP_SESSION_MAP, httpSession, Map.class);
        DesktopSession desktopSession = desktopSessionMap.get(desktopId);

        if (desktopSession == null) {
            String message = FlowsheetSessionListener.getNoDesktopIdMessage(httpSession);
            return Response.status(Response.Status.BAD_REQUEST).entity(message).build();
        }
        if (desktopSession.getVitalSubscription() != null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Vitals subscription already exists").build();
        }


        String patientId = desktopSession.getPatientId();
        DataSubscription dataSubscription = new DataSubscription();
        //save the tminus variable
        dataSubscription.setTminus(tminus);
        if (patientId != null) {
            //create the FHIR subscription
            String subscriptionId = subscriptionServiceDstu3.createVitalSubscription(patientId, tminus);
            dataSubscription.setPatient(patientId);
            dataSubscription.setEnabled(true);
            dataSubscription.setSubscriptionId(subscriptionId);
        } else {
            //if there is no patient id, we cannot create a FHIR subscription. However we need to save the tminus variable
            //so the patient change listener can create a new subscription when the user selects a patient
            dataSubscription.setPatient(null);
            dataSubscription.setEnabled(false);
            dataSubscription.setSubscriptionId(null);
        }

        desktopSession.setVitalSubscription(dataSubscription);
        return Response.ok("ok").build();
    }

    public static String getWebsocketUrl(HttpServletRequest request) {
        return getWebsocketUrl(request, null);
    }

    /**
     * Get the websocket url with the dtid parameter
     *
     * @param request
     * @return
     */
    public static String getWebsocketUrl(HttpServletRequest request, String desktopId) {
        String host = request.getServerName();
        int port = request.getServerPort();

        String websocketUrl = "ws://" + host + ":" + port + request.getServletContext().getContextPath() + WEBSOCKET_URL_PATH;
        if(desktopId != null) {
            websocketUrl = websocketUrl + "?dtdid=" + desktopId;
        }
        log.info(websocketUrl);
        return websocketUrl;
    }

    private void addPatientContextListener(final DesktopSession desktopSession) {
        IGenericEvent<Patient> patientChangeListener = new IGenericEvent<Patient>() {
            @Override
            public void eventCallback(String eventName, Patient patient) {
                if (patient == null) {
                    log.warn("User has unselected a patient");
                    return;
                }

                log.info("Patient Context Change: Patient " + patient.getId());
                String patientId = patient.getIdElement().getIdPart();
                DataSubscription subscription = desktopSession.getVitalSubscription();
                if (subscription != null) {
                    if (subscription.isEnabled()) {
                        //subscription was already created, just update the existing one
                        subscriptionServiceDstu3.updateVitalSubscription(subscription.getSubscriptionId(), patientId);
                        subscription.setPatient(patientId);
                    } else {
                        //subscription was not previously created because a patient was not selected earier
                        //we must create a new FHIR subscription and used the previous tminus variable when they subscribed earlier.
                        String subscriptionId = subscriptionServiceDstu3.createVitalSubscription(patientId, subscription.getTminus());
                        subscription.setEnabled(true);
                        subscription.setSubscriptionId(subscriptionId);
                        subscription.setPatient(patientId);
                    }
                }

                WebSocketSession socketSession = desktopSession.getWebsocketSession();
                if (socketSession != null && socketSession.isOpen()) {
                    DesktopIdSocketHandler.sendMessage(socketSession, "patient change");
                } else {
                    log.warn("Unable to notify to get the web socket session for the patient change notification");
                }

                desktopSession.setPatientId(patientId);
            }
        };

        EventManager.getInstance().subscribe(FlowsheetSessionListener.PATIENT_CHANGE_EVENT, patientChangeListener);
        desktopSession.setPatientChangeListener(patientChangeListener);
    }
}