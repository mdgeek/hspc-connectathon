/*
 *  Copyright 2017 Cognitive Medical Systems, Inc (http://www.cognitivemedicine.com).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  @author Jeff Chung
 */
package com.cogmedicine.flowsheet.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.resource.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import ca.uhn.fhir.rest.server.EncodingEnum;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FhirServiceDstu2 {

    public static String FHIR_DSTU2_SERVER;

    private static FhirContext fhirContextDstu2;
    private static IGenericClient clientDstu2;
    public static final int NUM_TO_DELETE_PER_QUERY = 100;

    public final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private static final String AUTOGENERATED_FHIR_ID_PATTERN = "^Patient/[0-9]+$";

    static {
        //Properties properties = SimulatorUtil.getProperties();
        //FHIR_DSTU2_SERVER = properties.getProperty("dstu3.server");
        FHIR_DSTU2_SERVER = "http://localhost:9092/baseDstu2";

        fhirContextDstu2 = FhirContext.forDstu2();
        clientDstu2 = fhirContextDstu2.newRestfulGenericClient(FHIR_DSTU2_SERVER);
    }

    /**
     * Create a fhir resource from json string
     * @param json
     * @return
     */
    public static String createResource(String json) {
        IBaseResource bundle = fhirContextDstu2.newJsonParser().parseResource(json);
        MethodOutcome outcome = clientDstu2.create().resource(bundle).execute();

        return outcome.getId().getValueAsString();
    }

    /**
     * Create a fhir resource
     * @param resource
     * @return
     */
    public static String createResource(IBaseResource resource) {
        MethodOutcome outcome = clientDstu2.create().resource(resource).execute();

        return outcome.getId().getValueAsString();
    }

    /**
     * Get resources from specified class and tag
     *
     * @param clazz
     * @param tag
     * @param limit
     * @param <T>
     * @return
     */
    public static <T extends IBaseResource> Bundle searchResources(Class<T> clazz, IBaseCoding tag, Integer limit) {
        IQuery iquery = clientDstu2.search().forResource(clazz);

        if (tag != null) {
            iquery.withTag(tag.getSystem(), tag.getCode());
        }

        if (limit != null) {
            iquery.count(limit);
        }

        return (Bundle) iquery.returnBundle(Bundle.class).execute();
    }

    /**
     * Convert resource to String based on encoding
     * @param encoding
     * @param resource
     * @return
     */
    public static String getReasourceAsString(EncodingEnum encoding, IBaseResource resource){
        return encoding.newParser(fhirContextDstu2).encodeResourceToString(resource);
    }

    /**
     * Convert resource list to String based on encoding
     * @param encoding
     * @param resources
     * @return
     */
    public static String getResourcesAsStringList(EncodingEnum encoding, List resources){
        StringBuilder builder = new StringBuilder();

        if(!resources.isEmpty()) {
            builder.append("[");
            for (int i = 0; i < resources.size(); i++) {
                IBaseResource resource = (IBaseResource) resources.get(i);
                builder.append(encoding.newParser(fhirContextDstu2).encodeResourceToString(resource));
                if(i < resources.size() - 1){
                    builder.append(",");
                }
            }
            builder.append("]");
        }

        return builder.toString();
    }

    public static Patient getPatientModel(String patientId){
        IQuery iquery = clientDstu2.search().forResource(Patient.class);
        iquery.where(Patient.RES_ID.matchesExactly().value(patientId));

        Bundle bundle = (Bundle) iquery.returnBundle(Bundle.class).execute();
        List<Bundle.Entry> entries = bundle.getEntry();

        if(entries.size() > 1) {
            throw new RuntimeException("...");
        }else if(entries.size() > 0) {
            return (Patient) entries.get(0).getResource();
        }else{
            return null;
        }
    }

    /**
     * Get DiagnostirReport list from Fhir server
     * @param patientId
     * @param startTime
     * @param endTime
     * @return
     */
    public static List<DiagnosticReport> getLabsModel(String patientId, String startTime, String endTime){
        Date startDate = formatTime(startTime, "startTime");
        Date endDate = formatTime(endTime, "endTime");

        IQuery iquery = clientDstu2.search().forResource(DiagnosticReport.class);
        iquery.where(DiagnosticReport.PATIENT.hasId(getFormattedId(patientId)));
        if(startDate != null){
            iquery.and(DiagnosticReport.DATE.afterOrEquals().second(startDate));
        }
        if(endDate != null){
            iquery.and(DiagnosticReport.DATE.beforeOrEquals().second(endDate));
        }

        Bundle bundle = (Bundle) iquery.returnBundle(Bundle.class).execute();
        List<Bundle.Entry> entries = bundle.getEntry();
        List<DiagnosticReport> diagnosticReports = getResourcesFromEntries(entries, DiagnosticReport.class);

        return diagnosticReports;
    }

    /**
     * Get Medication and MedicationAdministration list from Fhir server
     * @param patientId
     * @param startTime
     * @param endTime
     * @return
     */
    public static List getMedicationAdministrationModel(String patientId, String startTime, String endTime){
        Date startDate = formatTime(startTime, "startTime");
        Date endDate = formatTime(endTime, "endTime");

        IQuery iquery = clientDstu2.search().forResource(MedicationAdministration.class);
        iquery.where(MedicationAdministration.PATIENT.hasId(getFormattedId(patientId)));
        if(startDate != null){
            iquery.and(MedicationAdministration.EFFECTIVETIME.afterOrEquals().second(startDate));
        }
        if(endDate != null){
            iquery.and(MedicationAdministration.EFFECTIVETIME.beforeOrEquals().second(endDate));
        }

        iquery.include(new Include("MedicationAdministration:medication"));

        Bundle bundle = (Bundle) iquery.returnBundle(Bundle.class).execute();
        List<Bundle.Entry> entries = bundle.getEntry();
        List medicationAdministrations= getResourcesFromEntries(entries);

        return medicationAdministrations;
    }

    /**
     * Get observation list from fhir server
     * @param patientId
     * @param startTime
     * @param endTime
     * @return
     */
    public static List<Observation> getObservationModel(String patientId, String startTime, String endTime) {
        Date startDate = formatTime(startTime, "startTime");
        Date endDate = formatTime(endTime, "endTime");

        IQuery iquery = clientDstu2.search().forResource(Observation.class);
        iquery.where(Observation.PATIENT.hasId(getFormattedId(patientId)));
        if(startDate != null){
            iquery.and(Observation.DATE.afterOrEquals().second(startDate));
        }
        if(endDate != null){
            iquery.and(Observation.DATE.beforeOrEquals().second(endDate));
        }

        Bundle bundle = (Bundle) iquery.returnBundle(Bundle.class).execute();
        List<Bundle.Entry> entries = bundle.getEntry();
        List<Observation> observations = getResourcesFromEntries(entries, Observation.class);

        return observations;
    }

    /**
     * Get resources from bundle entry as a simple list
     * @param entries
     * @return
     */
    public static List getResourcesFromEntries(List<Bundle.Entry> entries){
        List resources = null;

        if(entries != null && !entries.isEmpty()) {
            resources = new ArrayList<>();
            for (Bundle.Entry entry : entries) {
                resources.add(entry.getResource());
            }
        }

        return resources;
    }

    /**
     * Get resources from bundle entry as a generic list
     * @param entries
     * @return
     */
    public static <T extends IBaseResource> List<T> getResourcesFromEntries(List<Bundle.Entry> entries, Class<T> clazz){
        List<T> resources = null;

        if(entries != null && !entries.isEmpty()) {
            resources = new ArrayList<>();
            for (Bundle.Entry entry : entries) {
                resources.add(clazz.cast(entry.getResource()));
            }
        }

        return resources;
    }

    /**
     * Parse date String with specific format
     * @param time
     * @param fieldName
     * @return
     */
    public static Date formatTime(String time, String fieldName){
        if (time != null) {
            try {
                return dateFormat.parse(time);
            } catch (ParseException e) {
                throw new RuntimeException("Invalid " + fieldName + " format. Use: yyyyMMddHHmmss");
            }
        }
        return null;
    }

    /**
     * Checking and update for id
     * @param id
     * @return
     */
    public static String getFormattedId(String id){
        if(!id.startsWith("Patient/")){
            id = "Patient/" + id;
        }

        Pattern idPattern = Pattern.compile(AUTOGENERATED_FHIR_ID_PATTERN);
        Matcher idMatcher = idPattern.matcher(id);
        if (idMatcher.find( )) {
            return FhirServiceDstu2.FHIR_DSTU2_SERVER + "/" + id;
        }

        return id;
    }

    /**
     * Returns the displayName, prefers LOINC display names
     * @param codingList
     * @return
     */
    public static String getDisplayName(List<CodingDt> codingList) {
        String displayName = "";
        for (CodingDt codingDt : codingList) {
            displayName = codingDt.getDisplay();
            if (codingDt.getSystem().equals("http://loinc.org")) {
                return displayName;
            }
        }
        return displayName;
    }
}