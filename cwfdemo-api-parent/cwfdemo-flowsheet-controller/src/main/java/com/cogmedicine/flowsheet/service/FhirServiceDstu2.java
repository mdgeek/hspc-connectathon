package com.cogmedicine.flowsheet.service;

import ca.uhn.fhir.context.FhirContext;
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

public class FhirServiceDstu2 {

    public static String FHIR_DSTU2_SERVER;

    private static FhirContext fhirContextDstu2;
    private static IGenericClient clientDstu2;
    public static final int NUM_TO_DELETE_PER_QUERY = 100;

    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    static {
        //Properties properties = SimulatorUtil.getProperties();
        //FHIR_DSTU2_SERVER = properties.getProperty("dstu3.server");
        FHIR_DSTU2_SERVER = "http://localhost:9092/baseDstu2";

        fhirContextDstu2 = FhirContext.forDstu2();
        clientDstu2 = fhirContextDstu2.newRestfulGenericClient(FHIR_DSTU2_SERVER);
    }

    public static String createResource(String json) {
        IBaseResource bundle = fhirContextDstu2.newJsonParser().parseResource(json);
        MethodOutcome outcome = clientDstu2.create().resource(bundle).execute();

        return outcome.getId().getValueAsString();
    }

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

    public static String getReasourceAsString(EncodingEnum encoding, IBaseResource resource){
        return encoding.newParser(fhirContextDstu2).encodeResourceToString(resource);
    }

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

    public static List<DiagnosticReport> getLabsModel(String patientId, String startTime, String endTime){
        Date startDate = formatTime(startTime, "startTime");
        Date endDate = formatTime(endTime, "endTime");

        IQuery iquery = clientDstu2.search().forResource(DiagnosticReport.class);
        iquery.where(DiagnosticReport.PATIENT.hasId(patientId));
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

    public static List<MedicationAdministration> getMedicationAdministrationModel(String patientId, String startTime, String endTime){
        Date startDate = formatTime(startTime, "startTime");
        Date endDate = formatTime(endTime, "endTime");

        IQuery iquery = clientDstu2.search().forResource(MedicationAdministration.class);
        iquery.where(MedicationAdministration.PATIENT.hasId(patientId));
        if(startDate != null){
            iquery.and(MedicationAdministration.EFFECTIVETIME.afterOrEquals().second(startDate));
        }
        if(endDate != null){
            iquery.and(MedicationAdministration.EFFECTIVETIME.beforeOrEquals().second(endDate));
        }

        Bundle bundle = (Bundle) iquery.returnBundle(Bundle.class).execute();
        List<Bundle.Entry> entries = bundle.getEntry();
        List<MedicationAdministration> medicationAdministrations= getResourcesFromEntries(entries, MedicationAdministration.class);

        return medicationAdministrations;
    }

    public static List<Observation> getObservationModel(String patientId, String startTime, String endTime){
        Date startDate = formatTime(startTime, "startTime");
        Date endDate = formatTime(endTime, "endTime");

        IQuery iquery = clientDstu2.search().forResource(Observation.class);
        iquery.where(Observation.PATIENT.hasId(FhirServiceDstu2.FHIR_DSTU2_SERVER + "/" + patientId));
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
}