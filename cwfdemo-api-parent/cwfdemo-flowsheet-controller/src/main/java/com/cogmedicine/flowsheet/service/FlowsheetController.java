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

import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.MedicationAdministration;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.rest.server.EncodingEnum;
import org.hspconsortium.cwf.api.patient.PatientContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/")
public class FlowsheetController {

    private final static Map<String, String> registry = new HashMap<String, String>();

    public FlowsheetController() {
    }

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

    @GET
    @Path("/lab")
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

    @GET
    @Path("/medicationAdministration")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMedicationAdministrationModel(
            @QueryParam("patientId") String patientId,
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime) {
        try {
            List<MedicationAdministration> medicationAdministrations = FhirServiceDstu2.getMedicationAdministrationModel(patientId, startTime, endTime);
            String response = FhirServiceDstu2.getResourcesAsStringList(EncodingEnum.JSON, medicationAdministrations);
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/observation")
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

    @GET
    @Path("/vitals")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVitalsModel(
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime) {
            String json = "{\"type\":\"Vital Sign\",\"data\":[{\"generalSettings\":{\"provenance\":\"hospital\"},\"mBP\":[{\"timeStamp\":\"20170328154425\",\"value\":113}]},{\"generalSettings\":{\"provenance\":\"home\"},\"sBP\":[{\"timeStamp\":\"20170328154409\",\"value\":125},{\"timeStamp\":\"20170328154414\",\"value\":145},{\"timeStamp\":\"20170328154418\",\"value\":155}]},{\"generalSettings\":{\"provenance\":\"home\"},\"dBP\":[{\"timeStamp\":\"20170328154409\",\"value\":80},{\"timeStamp\":\"20170328154414\",\"value\":90},{\"timeStamp\":\"20170328154418\",\"value\":102}]},{\"generalSettings\":{\"provenance\":\"home\"},\"mBP\":[{\"timeStamp\":\"20170328154409\",\"value\":95},{\"timeStamp\":\"20170328154414\",\"value\":108},{\"timeStamp\":\"20170328154418\",\"value\":120}]},{\"generalSettings\":{\"provenance\":\"hospital\"},\"sBP\":[{\"timeStamp\":\"20170328154425\",\"value\":155}]},{\"generalSettings\":{\"provenance\":\"hospital\"},\"dBP\":[{\"timeStamp\":\"20170328154425\",\"value\":102}]}]}";
            return Response.ok(json).build();
    }

}
