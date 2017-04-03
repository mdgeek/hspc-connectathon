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

import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class FhirServiceDstu2IT {

    private static long dayAsMilis = 1000 * 60 * 60 * 24;

    @Test
    public void getObservationModel() {
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
    }


    @Test
    public void test1() {
        //if the id was created by the FHIR server
        String id = "http://localhost:9092/baseDstu2/Patient/1";
        observationCall(id);
    }

    @Test
    public void test2() {
        String id = "http://localhost:9092/baseDstu2/Patient/PatientId-1234";
        observationCall(id);
    }

    @Test
    public void test3() {
        String id = "Patient/1";
        observationCall(id);
    }

    @Test
    public void test4() {
        String id = "Patient/PatientId-1234";
        observationCall(id);
    }

    @Test
    public void aaa() {
        System.out.println(FhirServiceDstu2.getFormattedId("1"));
        System.out.println(FhirServiceDstu2.getFormattedId("Patient/1"));
        System.out.println(FhirServiceDstu2.getFormattedId("a1"));
        System.out.println(FhirServiceDstu2.getFormattedId("Patienta1"));
    }

    public void observationCall(String id) {
        List<Observation> observations = FhirServiceDstu2.getObservationModel(id, null, null);

        Assert.assertNotNull(observations);
        Assert.assertFalse(observations.isEmpty());

        System.out.println(observations.size());
    }
}
