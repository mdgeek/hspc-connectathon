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
package com.cogmedicine.flowsheet.service;

import com.cogmedicine.flowsheet.socket.DesktopIdSocketHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carewebframework.api.domain.User;
import org.carewebframework.api.event.EventManager;
import org.carewebframework.api.event.IGenericEvent;
import org.carewebframework.ui.util.RequestUtil;
import org.hl7.fhir.dstu3.model.Patient;
import org.zkoss.zk.ui.ComponentNotFoundException;
import org.zkoss.zkplus.embed.Bridge;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;

@Path("/subscription")
public class FlowsheetSubscriptionController {

    private static final Log log = LogFactory.getLog(FlowsheetSubscriptionController.class);
    private SubscriptionService subscriptionService;
    private static final HashMap<String, DataSubscription> vitalsSubscriptionLookup = new HashMap<>();
    private static final HashMap<String, String> ioSubscriptionLookup = new HashMap<>();
    private static final HashMap<String, IGenericEvent<Patient>> patientContextListenerMap = new HashMap<>();
    private static final HashMap<String, IGenericEvent<User>> userContextListenerMap = new HashMap<>();
    private static final String PATIENT_CHANGE_EVENT = "CONTEXT.CHANGED.Patient";
    private static final String USER_CHANGE_EVENT = "CONTEXT.CHANGED.User";
    private static final String NOOP = "NOOP";

    public static final String WEBSOCKET_URL_PATH = "/flowsheetSubscription/socket";

    public FlowsheetSubscriptionController() {
        subscriptionService = new SubscriptionService();
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
        try {
            if (!patientContextListenerMap.containsKey(desktopId)) {
                addPatientContextListener(desktopId, request, response);
            }
            if (!userContextListenerMap.containsKey(desktopId)) {
                addUserContextListener(desktopId, request, response);
            }
            return Response.ok(getWebsocketUrl(request)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * Creates a vitals subscription and a patient context subscription and returns the websocket connection url
     * Typically used for clients that need to be notified of data changes.
     *
     * @param desktopId
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

        try {
            if (!patientContextListenerMap.containsKey(desktopId)) {
                addPatientContextListener(desktopId, request, response);
            }
            if (!userContextListenerMap.containsKey(desktopId)) {
                addUserContextListener(desktopId, request, response);
            }
            if (vitalsSubscriptionLookup.containsKey(desktopId)) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Vitals subscription already exists").build();
            }

            Patient patient = FhirServiceDstu2.getPatientFromContext(desktopId, request, response);
            DataSubscription dataSubscription = new DataSubscription();
            dataSubscription.setTminus(tminus);
            if (patient != null) {
                String patientId = patient.getIdElement().getIdPart();
                String subscriptionId = subscriptionService.createVitalSubscription(patientId, tminus);
                dataSubscription.setPatient(patientId);
                dataSubscription.setEnabled(true);
                dataSubscription.setSubscriptionId(subscriptionId);
            } else {
                dataSubscription.setPatient(null);
                dataSubscription.setEnabled(false);
                dataSubscription.setSubscriptionId(null);
            }
            vitalsSubscriptionLookup.put(desktopId, dataSubscription);
            return Response.ok(getWebsocketUrl(request)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }


    /**
     * Deletes the subscription by the desktop id
     *
     * @param desktopId
     * @return
     */
    @GET
    @Path("/deletePatientContext/{dtid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deletePatientContextSubscription(
            @PathParam("dtid") String desktopId,
            @Context HttpServletRequest request,
            @Context HttpServletResponse response) {
        if (desktopId == null || desktopId.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("dtid is a required parameter").build();
        }
        deletePatientAndUserContext(desktopId, request, response);

        return Response.ok().build();
    }

    @GET
    @Path("/deleteVitals/{dtid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteVitalsSubscription(
            @PathParam("dtid") String desktopId,
            @Context HttpServletRequest request,
            @Context HttpServletResponse response) {
        if (desktopId == null || desktopId.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("dtid is a required parameter").build();
        }

        DataSubscription subscription = vitalsSubscriptionLookup.get(desktopId);
        if(subscription != null) {
            if (subscription.isEnabled()) {
                subscriptionService.deleteSubscription(subscription.getSubscriptionId());
            }
            vitalsSubscriptionLookup.remove(desktopId);
        }

        //only delete the patient context listener and user context listener if there are no other subscriptions (io) for that desktop id
        if (!ioSubscriptionLookup.containsKey(desktopId)) {
            deletePatientAndUserContext(desktopId, request, response);
        }
        return Response.ok().build();
    }

    public static String getWebsocketUrl(HttpServletRequest request) {
        String host = request.getServerName();
        int port = request.getServerPort();

        String websocketUrl = "ws://" + host + ":" + port + request.getServletContext().getContextPath() + WEBSOCKET_URL_PATH;
        log.info(websocketUrl);
        return websocketUrl;
    }

    private void addPatientContextListener(
            final String desktopId,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (desktopId == null || desktopId.trim().length() == 0) {
            throw new IllegalArgumentException("Desktop id parameter is empty");
        }

        Bridge bridge = null;
        try {
            bridge = RequestUtil.startExecution(request, response, desktopId);
            if (bridge == null) {
                throw new IllegalArgumentException("Unable to create the bridge with desktop id: " + desktopId);
            }
            IGenericEvent<Patient> patientChangeListener = new IGenericEvent<Patient>() {
                @Override
                public void eventCallback(String eventName, Patient patient) {
                    //todo can a user unselect a patient?
                    if (patient == null) {
                        log.warn("User has unselected a patient");
                        return;
                    }

                    log.info("Patient Context Change: Patient " + patient.getId() + " for desktop id " + desktopId);
                    String patientId = patient.getIdElement().getIdPart();
                    DataSubscription subscription = vitalsSubscriptionLookup.get(desktopId);
                    if (subscription != null) {
                        if (subscription.isEnabled()) {
                            subscriptionService.updateVitalSubscription(subscription.getSubscriptionId(), patientId);
                            subscription.setPatient(patientId);
                        } else {
                            String subscriptionId = subscriptionService.createVitalSubscription(patientId, subscription.getTminus());
                            subscription.setEnabled(true);
                            subscription.setSubscriptionId(subscriptionId);
                            subscription.setPatient(patientId);
                        }
                    }
                    DesktopIdSocketHandler.sendMessage(desktopId, "patient change");
                }
            };
            EventManager.getInstance().subscribe(PATIENT_CHANGE_EVENT, patientChangeListener);
            patientContextListenerMap.put(desktopId, patientChangeListener);
        } catch (ComponentNotFoundException e) {
            throw new IllegalArgumentException("Invalid desktop id, desktop not found: " + desktopId);
        } finally {
            if (bridge != null) {
                bridge.close();
            }
        }
    }

    private void addUserContextListener(
            final String desktopId,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (desktopId == null || desktopId.trim().length() == 0) {
            throw new IllegalArgumentException("Desktop id parameter is empty");
        }

        Bridge bridge = null;
        try {
            bridge = RequestUtil.startExecution(request, response, desktopId);
            if (bridge == null) {
                throw new IllegalArgumentException("Unable to create the bridge with desktop id: " + desktopId);
            }
            IGenericEvent<User> userChangeListener = new IGenericEvent<User>() {
                @Override
                public void eventCallback(String eventName, User user) {
                    if (user == null) {
                        DesktopIdSocketHandler.clearSession(desktopId);
                        log.info("User has logged out");
                    }
                    else {
                        log.info("User change event");
                    }
                }
            };
            EventManager.getInstance().subscribe(USER_CHANGE_EVENT, userChangeListener);
            userContextListenerMap.put(desktopId, userChangeListener);
        } catch (ComponentNotFoundException e) {
            throw new IllegalArgumentException("Invalid desktop id, desktop not found: " + desktopId);
        } finally {
            if (bridge != null) {
                bridge.close();
            }
        }
    }

    private void deletePatientAndUserContext(String desktopId, HttpServletRequest request, HttpServletResponse response) {
        //only delete the patient context listener and user context listener if there are no other subscriptions (io) for that desktop id
        if (!ioSubscriptionLookup.containsKey(desktopId)) {
            Bridge bridge = null;
            try {
                bridge = RequestUtil.startExecution(request, response, desktopId);
                if (patientContextListenerMap.containsKey(desktopId)) {
                    IGenericEvent<Patient> patientListener = patientContextListenerMap.get(desktopId);
                    EventManager.getInstance().unsubscribe(PATIENT_CHANGE_EVENT, patientListener);

                    patientContextListenerMap.remove(desktopId);
                }
                if (userContextListenerMap.containsKey(desktopId)) {
                    IGenericEvent<User> userListener = userContextListenerMap.get(desktopId);
                    EventManager.getInstance().unsubscribe(USER_CHANGE_EVENT, userListener);

                    userContextListenerMap.remove(desktopId);
                }
            } finally {
                if (bridge != null) {
                    bridge.close();
                }
            }
        }
    }
}