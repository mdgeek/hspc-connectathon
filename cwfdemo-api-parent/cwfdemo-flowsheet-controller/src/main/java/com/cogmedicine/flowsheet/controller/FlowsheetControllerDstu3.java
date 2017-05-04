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

import ca.uhn.fhir.rest.server.EncodingEnum;
import com.cogmedicine.flowsheet.bean.DesktopSession;
import com.cogmedicine.flowsheet.listener.FlowsheetSessionListener;
import com.cogmedicine.flowsheet.service.FhirServiceDstu3;
import com.cogmedicine.flowsheet.util.Utilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;

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
import java.util.*;

@Path("/")
public class FlowsheetControllerDstu3 {

    private static final Log log = LogFactory.getLog(FlowsheetControllerDstu3.class);

    public FlowsheetControllerDstu3() {
    }

    /**
     * Rest interface for {@link FhirServiceDstu3#getPatientModel(String)}
     *
     * @param patientId
     * @return
     */
    @GET
    @Path("/patient")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPatientModel(@QueryParam("patientId") String patientId) {
        try {
            Patient patient = FhirServiceDstu3.getPatientModel(patientId);
            String jsonPatient = FhirServiceDstu3.getReasourceAsString(EncodingEnum.JSON, patient);
            return Response.ok(jsonPatient).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * Rest interface for {@link FhirServiceDstu3#getLabsModel(String, String, String)}
     *
     * @param patientId
     * @param startTime
     * @param endTime
     * @return
     */
    @GET
    @Path("/labs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLabsModel(
            @QueryParam("patientId") String patientId,
            @QueryParam("starttime") String startTime,
            @QueryParam("endtime") String endTime) {
        try {
            List<DiagnosticReport> diagnosticReports = FhirServiceDstu3.getLabsModel(patientId, startTime, endTime);
            String response = FhirServiceDstu3.getResourcesAsStringList(EncodingEnum.JSON, diagnosticReports);
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * Rest interface for {@link FhirServiceDstu3#getMedicationAdministrationModel(String, String, String)}
     *
     * @param startTime
     * @param endTime
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/medicationAdministrations")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMedicationAdministrationModel(
            @QueryParam("dtid") String desktopId,
            @QueryParam("starttime") String startTime,
            @QueryParam("endtime") String endTime,
            @Context HttpServletRequest request,
            @Context HttpServletResponse response) {
        if (desktopId == null || desktopId.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("dtid is a required parameter").build();
        }

        Map<String, Object> map = new HashMap<>();
        map.put("type", "Medication Administration");

        HttpSession httpSession = request.getSession();
        Map<String, DesktopSession> desktopSessionMap = Utilities.getParameter(FlowsheetSessionListener.DESKTOP_SESSION_MAP, httpSession, Map.class);
        DesktopSession desktopSession = desktopSessionMap.get(desktopId);

        if (desktopSession == null) {
            String message = FlowsheetSessionListener.getNoDesktopIdMessage(httpSession);
            return Response.status(Response.Status.BAD_REQUEST).entity(message).build();
        }

        String patientId = desktopSession.getPatientId();
        if (patientId == null) {
            log.info("No patient has been set in the patient context");
            map.put("data", new ArrayList<>());
            return Response.ok(map).build();
        } else {
            log.info("Current patient has id " + patientId);
        }

        List medicationAdministrationsAndMedications = FhirServiceDstu3.getMedicationAdministrationModel(patientId, startTime, endTime);
        List<Map<String, Object>> data = new ArrayList<>();
        //list currently contains both Medication objects and MedicationAdministration objects
        if (medicationAdministrationsAndMedications != null) {
            //create a map of medication ids and their display names
            HashMap<String, String> medicationDisplayNameMap = new HashMap<>();
            for (Object object : medicationAdministrationsAndMedications) {
                if (object instanceof Medication) {
                    Medication medication = (Medication) object;
                    String medId = medication.getId();
                    String medName = medication.getCode().getCoding().get(0).getDisplay();
                    medicationDisplayNameMap.put(medId, medName);
                }
            }

            //create a list of medication administrations
            for (Object object : medicationAdministrationsAndMedications) {
                if (object instanceof MedicationAdministration) {
                    MedicationAdministration medicationAdministration = (MedicationAdministration) object;
                    Date timestamp = getDateFromEffectiveTime(medicationAdministration.getEffective());

                    Map<String, Object> medicationDetail = new HashMap<>();
                    medicationDetail.put("value", medicationAdministration.getStatus());
                    medicationDetail.put("timestamp", FhirServiceDstu3.dateFormat.format(timestamp));

                    Reference medicationReference = (Reference) medicationAdministration.getMedication();
                    String medId = medicationReference.getReference();
                    String medName = medicationDisplayNameMap.get(medId);

                    List details = getWraperElement(data, medName);
                    details.add(medicationDetail);
                }
            }
        }
        map.put("data", data);
        return Response.ok(map).build();

    }

    /**
     * Rest interface for {@link FhirServiceDstu3#getObservationModel(String, String, String)}
     *
     * @param patientId
     * @param startTime
     * @param endTime
     * @return
     */
    @GET
    @Path("/observations")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObservationModel(
            @QueryParam("patientId") String patientId,
            @QueryParam("starttime") String startTime,
            @QueryParam("endtime") String endTime) {
        try {
            List<Observation> observations = FhirServiceDstu3.getObservationModel(patientId, startTime, endTime);
            String response = FhirServiceDstu3.getResourcesAsStringList(EncodingEnum.JSON, observations);
            //todo transform into a JSON grid model for the
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * Rest interface for vitals}
     *
     * @param startTime
     * @param endTime
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/vitals")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVitalsModel(
            @QueryParam("dtid") String desktopId,
            @QueryParam("starttime") String startTime,
            @QueryParam("endtime") String endTime,
            @Context HttpServletRequest request,
            @Context HttpServletResponse response) {

        Map<String, Object> map = new HashMap<>();
        map.put("type", "Vital Sign");

        if (desktopId == null || desktopId.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("dtid is a required parameter").build();
        }

        HttpSession httpSession = request.getSession();
        Map<String, DesktopSession> desktopSessionMap = Utilities.getParameter(FlowsheetSessionListener.DESKTOP_SESSION_MAP, httpSession, Map.class);
        DesktopSession desktopSession = desktopSessionMap.get(desktopId);

        if (desktopSession == null) {
            String message = FlowsheetSessionListener.getNoDesktopIdMessage(httpSession);
            return Response.status(Response.Status.BAD_REQUEST).entity(message).build();
        }

        String patientId = desktopSession.getPatientId();
        if (patientId == null) {
            log.info("No patient has been set in the patient context");
            map.put("data", new ArrayList<>());
            return Response.ok(map).build();
        } else {
            log.info("Current patient has id " + patientId);
        }

        List<Observation> observations = FhirServiceDstu3.getObservationModel(patientId, startTime, endTime);
        List<Map<String, Object>> data = new ArrayList<>();

        if (observations != null) {
            for (Observation observation : observations) {
                //Some observations are made up of multiple quantities which are stored as components
                for (Observation.ObservationComponentComponent component : observation.getComponent()) {
                    Date timestamp = getDateFromEffectiveTime(observation.getEffective());
                    Quantity quantity = (Quantity) component.getValue();
                    String value = quantity.getValue().toString();

                    Map<String, Object> detail = new HashMap<>();
                    detail.put("timestamp", FhirServiceDstu3.dateFormat.format(timestamp));
                    detail.put("value", value);

                    String displayName = FhirServiceDstu3.getDisplayName(component.getCode().getCoding());

                    List details = getWraperElement(data, displayName);
                    details.add(detail);
                }
                //Some observations have a single quantity
                try {
                    if (observation.hasValueQuantity() && observation.getValueQuantity().hasValue()) {
                        Quantity quantity = observation.getValueQuantity();
                        String value = quantity.getValue().toString();
                        String displayName = FhirServiceDstu3.getDisplayName(observation.getCode().getCoding());
                        Map<String, Object> detail = new HashMap<>();
                        Date timestamp = getDateFromEffectiveTime(observation.getEffective());
                        detail.put("timestamp", FhirServiceDstu3.dateFormat.format(timestamp));
                        detail.put("value", value);
                        List details = getWraperElement(data, displayName);
                        details.add(detail);
                    }
                } catch (FHIRException e) {
                    //skip as some observations use multipe quantities as components instead of a single quantity
                }
            }
        }
        map.put("data", data);
        return Response.ok(map).build();
    }

    /**
     * Rest interface for vitals}
     *
     * @param startTime
     * @param endTime
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/I_O")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIOModel(
            @QueryParam("dtid") String desktopId,
            @QueryParam("starttime") String startTime,
            @QueryParam("endtime") String endTime,
            @Context HttpServletRequest request,
            @Context HttpServletResponse response) {
        if (desktopId == null || desktopId.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("dtid is a required parameter").build();
        }
        if (startTime == null || startTime.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("starttime is a required parameter").build();
        }
        if (endTime == null || endTime.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("endtime is a required parameter").build();
        }

        HttpSession httpSession = request.getSession();
        Map<String, DesktopSession> desktopSessionMap = Utilities.getParameter(FlowsheetSessionListener.DESKTOP_SESSION_MAP, httpSession, Map.class);
        DesktopSession desktopSession = desktopSessionMap.get(desktopId);

        if (desktopSession == null) {
            String message = FlowsheetSessionListener.getNoDesktopIdMessage(httpSession);
            return Response.status(Response.Status.BAD_REQUEST).entity(message).build();
        }

        String patientId = desktopSession.getPatientId();
        if (patientId == null) {
            log.info("No patient has been set in the patient context");

            Map<String, Object> map = new HashMap<>();
            map.put("type", "I_O");
            map.put("data", new ArrayList<>());

            return Response.ok(map).build();
        } else {
            log.info("Current patient has id " + patientId);
        }

        Map data = FhirServiceDstu3.getIOData(patientId, startTime, endTime);
        data.put("type", "I_O");

        return Response.ok(data).build();
    }

    public Date getDateFromEffectiveTime(Type datatype) {
        Date timestamp = null;

        if (datatype instanceof DateTimeType) {
            timestamp = ((DateTimeType) datatype).getValue();
        } else if (datatype instanceof Period) {
            timestamp = ((Period) datatype).getStart();
        }

        return timestamp;
    }

    /**
     * Sets the first letter to lower case and the rest to upper case for sBP and dBP
     *
     * @param displayName
     * @return
     */
    public String parseDisplayName(String displayName) {
        String[] words = displayName.split(" ");
        StringBuilder name = new StringBuilder();

        name.append((words[0].charAt(0) + "").toLowerCase());
        if (words.length > 1) {
            for (int i = 1; i < words.length; i++) {
                name.append((words[i].charAt(0) + "").toUpperCase());
            }
        }

        return name.toString();
    }

    public List getWraperElement(List<Map<String, Object>> data, String key) {
        for (Map<String, Object> map : data) {
            if (map.containsKey(key)) {
                return (List) map.get(key);
            }
        }

        List list = new ArrayList<>();
        Map<String, Object> wraperMap = new HashMap<>();
        wraperMap.put(key, list);

        data.add(wraperMap);

        return list;
    }
}
