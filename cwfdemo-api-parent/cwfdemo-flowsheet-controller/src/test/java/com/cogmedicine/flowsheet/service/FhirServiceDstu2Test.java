package com.cogmedicine.flowsheet.service;

import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import org.hl7.fhir.instance.model.DateTimeType;
import org.hl7.fhir.instance.model.DateType;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;
import java.util.List;

/**
 * Created by Jeff on 3/27/2017.
 */
@Ignore
public class FhirServiceDstu2Test {

    private static long dayAsMilis = 1000 * 60 * 60 * 24;

    @Test
    public void getObservationModel(){
        //Patient patient = FhirDstu2Util.getPatient();
        //String patientId = FhirServiceDstu2.createResource(patient);
        String patientId = FhirServiceDstu2.FHIR_DSTU2_SERVER + "/Patient/1";

        ResourceReferenceDt patientReference = new ResourceReferenceDt();
        patientReference.setReference(patientId);

        long time = new Date().getTime();
        time = time + (dayAsMilis * 2);

        DateTimeDt effective = new DateTimeDt();
        effective.setValue(new Date(time));

        Observation observation = FhirDstu2Util.getSnomedObservation();
        observation.setSubject(patientReference);
        observation.setEffective(effective);

        String id = FhirServiceDstu2.createResource(observation);
        //List<Observation> observations = FhirServiceDstu2.getObservationModel(id, (Date)null, (Date)null);

        //Assert.assertNotNull(observations);
        //Assert.assertFalse(observations.isEmpty());
    }


    @Test
    public void aaa(){
        //long time = new Date().getTime();
        //time = time + (dayAsMilis * 2);

        //String id = FhirServiceDstu2.FHIR_DSTU2_SERVER + "/Patient/1";
        String id = FhirServiceDstu2.FHIR_DSTU2_SERVER + "1";
        List<Observation> observations = FhirServiceDstu2.getObservationModel(id, null, null);

        Assert.assertNotNull(observations);
        Assert.assertFalse(observations.isEmpty());

        System.out.println(observations.size());
    }
}
