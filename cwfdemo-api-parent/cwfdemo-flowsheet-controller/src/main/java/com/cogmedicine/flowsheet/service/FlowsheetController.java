/*-
 * #%L
 * Registry API
 * %%
 * Copyright (C) 2014 - 2017 Healthcare Services Platform Consortium
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
package com.cogmedicine.flowsheet.service;

import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.dstu2.composite.QuantityDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.*;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.rest.server.EncodingEnum;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carewebframework.ui.util.RequestUtil;
import org.hspconsortium.cwf.api.patient.PatientContext;
import org.zkoss.zkplus.embed.Bridge;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/")
public class FlowsheetController {

    private static final Log log = LogFactory.getLog(FlowsheetController.class);

    private final static Map<String, String> registry = new HashMap<>();

    public FlowsheetController() {
    }

    /**
     * Rest interface for {@link FhirServiceDstu2#getPatientModel(String)}
     * @param patientId
     * @return
     */
    @GET
    @Path("/patient")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPatientModel(@QueryParam("patientId") String patientId) {
        try {
            Patient patient = FhirServiceDstu2.getPatientModel(patientId);
            String jsonPatient = FhirServiceDstu2.getReasourceAsString(EncodingEnum.JSON, patient);
            return Response.ok(jsonPatient).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * Rest interface for {@link FhirServiceDstu2#getLabsModel(String, String, String)}
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
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime) {
        try {
            List<DiagnosticReport> diagnosticReports = FhirServiceDstu2.getLabsModel(patientId, startTime, endTime);
            String response = FhirServiceDstu2.getResourcesAsStringList(EncodingEnum.JSON, diagnosticReports);
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * Rest interface for {@link FhirServiceDstu2#getMedicationAdministrationModel(String, String, String)}
     * @param desktopId
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
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime,
            @Context HttpServletRequest request,
            @Context HttpServletResponse response) {

        if (desktopId == null || desktopId.trim().length() == 0) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Map<String, Object> map = new HashMap<>();
        map.put("type", "Medication Administration");

        org.hl7.fhir.dstu3.model.Patient patient = null;
        Bridge bridge = RequestUtil.startExecution(request, response, desktopId);
        if (bridge == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            if (PatientContext.getPatientContext() != null) {
                patient = PatientContext.getActivePatient();
            }
            if (patient == null) {
                log.info("No patient has been set in the patient context");
                map.put("data", new ArrayList<>());
                return Response.ok(map).build();
            } else {
                log.info("Current patient has id " + patient.getIdElement().getIdPart());
            }
        } finally {
            bridge.close();
        }

        String id = patient.getIdElement().getIdPart();
        List medicationAdministrationsAndMedications = FhirServiceDstu2.getMedicationAdministrationModel(id, startTime, endTime);

        List<Map<String, Object>> data = new ArrayList<>();
        if (medicationAdministrationsAndMedications != null) {
            for (Object object : medicationAdministrationsAndMedications) {
                if (object instanceof MedicationAdministration) {
                    MedicationAdministration medicationAdministration = (MedicationAdministration) object;
                    Date timestamp = getDateFromEffectiveTime(medicationAdministration.getEffectiveTime());

                    Map<String, Object> medicationDetail = new HashMap<>();
                    medicationDetail.put("value", medicationAdministration.getStatus());
                    medicationDetail.put("timestamp", FhirServiceDstu2.dateFormat.format(timestamp));

                    ResourceReferenceDt medicationReference = (ResourceReferenceDt) medicationAdministration.getMedication();
                    String tempKey = medicationReference.getReference().getIdPart();

                    List details = getWraperElement(data, tempKey);
                    details.add(medicationDetail);
                }
            }
            for (Object object : medicationAdministrationsAndMedications) {
                if (object instanceof Medication) {
                    Medication medication = (Medication) object;
                    String tempKey = medication.getId().getIdPart();
                    Map<String, Object> medicationMap = getMedicationMap(tempKey, data);
                    if (medicationMap != null) {
                        String key = medication.getCode().getCoding().get(0).getDisplay();
                        medicationMap.put(key, medicationMap.remove(tempKey));
                    }
                }
            }
        }
        map.put("data", data);
        return Response.ok(map).build();

    }

    public Map<String, Object> getMedicationMap(String tempKey, List<Map<String, Object>> data) {
        for (Map<String, Object> medicationMap : data) {
            if (medicationMap.containsKey(tempKey)) {
                return medicationMap;
            }
        }

        return null;
    }

    /**
     * Rest interface for {@link FhirServiceDstu2#getObservationModel(String, String, String)}
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
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime) {
        try {
            List<Observation> observations = FhirServiceDstu2.getObservationModel(patientId, startTime, endTime);
            String response = FhirServiceDstu2.getResourcesAsStringList(EncodingEnum.JSON, observations);
            //todo transform into a JSON grid model for the
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * Rest interface for vitals}
     * @param desktopId
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
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime,
            @Context HttpServletRequest request,
            @Context HttpServletResponse response) {
        if (desktopId == null || desktopId.trim().length() == 0) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Map<String, Object> map = new HashMap<>();
        map.put("type", "Vital Sign");

        org.hl7.fhir.dstu3.model.Patient patient = null;
        Bridge bridge = RequestUtil.startExecution(request, response, desktopId);
        if (bridge == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            if (PatientContext.getPatientContext() != null) {
                patient = PatientContext.getActivePatient();
            }
            if (patient == null) {
                log.info("No patient has been set in the patient context");
                map.put("data", new ArrayList<>());
                return Response.ok(map).build();
            } else {
                log.info("Current patient has id " + patient.getIdElement().getIdPart());
            }
        } finally {
            bridge.close();
        }

        String id = patient.getIdElement().getIdPart();
        List<Observation> observations = FhirServiceDstu2.getObservationModel(id, startTime, endTime);

        List<Map<String, Object>> data = new ArrayList<>();
        if (observations != null) {
            for (Observation observation : observations) {
                for (Observation.Component component : observation.getComponent()) {
                    Date timestamp = getDateFromEffectiveTime(observation.getEffective());
                    QuantityDt quantity = (QuantityDt) component.getValue();
                    String value = quantity.getValue().toString();

                    Map<String, Object> detail = new HashMap<>();
                    detail.put("timestamp", FhirServiceDstu2.dateFormat.format(timestamp));
                    detail.put("value", value);

                    String displayName = FhirServiceDstu2.getDisplayName(component.getCode().getCoding());
                    String name = parseDisplayName(displayName);

                    List details = getWraperElement(data, name);
                    details.add(detail);
                }
            }
        }
        map.put("data", data);
        return Response.ok(map).build();
    }

    public Date getDateFromEffectiveTime(IDatatype datatype){
        Date timestamp = null;

        if (datatype instanceof DateTimeDt) {
            timestamp = ((DateTimeDt) datatype).getValue();
        } else if (datatype instanceof PeriodDt) {
            timestamp = ((PeriodDt) datatype).getStart();
        }

        return timestamp;
    }

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
