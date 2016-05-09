/*
 * #%L
 * cwf-ui-mockuments
 * %%
 * Copyright (C) 2014 - 2016 Healthcare Services Platform Consortium
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
package org.hspconsortium.cwfdemo.ui.mockuments;

import java.util.Date;

import org.zkoss.zk.ui.Component;

import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Encounter.EncounterClass;
import org.hl7.fhir.dstu3.model.Encounter.EncounterLocationComponent;
import org.hl7.fhir.dstu3.model.Encounter.EncounterParticipantComponent;
import org.hl7.fhir.dstu3.model.Encounter.EncounterState;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hspconsortium.cwf.fhir.common.FhirUtil;
import org.hspconsortium.cwf.fhir.document.Document;
import org.hspconsortium.cwf.fhir.document.DocumentService;
import org.hspconsortium.cwfdemo.api.democonfig.DemoUtils;

public class NewbornAdmissionResponseHandler extends BaseQuestionnaireHandler {
    
    
    private final DocumentService service;
    
    NewbornAdmissionResponseHandler(DocumentService service) {
        super("newborn-admission");
        this.service = service;
    }
    
    @Override
    public void processResponses(Document document, final Component root, org.w3c.dom.Document responses) {
        final Encounter encounter = new Encounter();
        encounter.setPatient(document.getReference().getSubject());
        encounter.setPeriod(new Period());
        encounter.setStatus(EncounterState.INPROGRESS);
        encounter.setClass_(EncounterClass.INPATIENT);
        Identifier ident = DemoUtils.createIdentifier("patient", "mother");
        Patient mother = FhirUtil.getFirst(service.searchResourcesByIdentifier(ident, Patient.class));
        
        if (mother != null) {
            EncounterParticipantComponent participant = encounter.addParticipant();
            RelatedPerson rp = new RelatedPerson(new Reference(mother));
            participant.setIndividual(new Reference(rp));
            participant.addType(FhirUtil.createCodeableConcept("participant_type", "mother", "mother"));
        }
        
        DemoUtils.addDemoTag(encounter);
        
        processResponses(responses, new IResponseProcessor() {
            
            
            @Override
            public void processResponse(String value, String targetId) {
                if ("datEffective".equals(targetId)) {
                    encounter.getPeriod().setStart(new Date(Long.parseLong(value)));
                } else if ("cboLocation".equals(targetId)) {
                    EncounterLocationComponent loc = encounter.addLocation();
                    loc.setLocation(new Reference(value));
                }
                
            }
            
        });
        
        service.createResource(encounter);
    }
    
}
