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
import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Encounter.EncounterLocationComponent;
import org.hl7.fhir.dstu3.model.Reference;
import org.hspconsortium.cwf.fhir.document.Document;
import org.hspconsortium.cwf.api.scenario.Scenario;
import org.hspconsortium.cwf.api.scenario.ScenarioUtil;
import org.zkoss.zk.ui.Component;

public class NewbornAdmissionResponseHandler extends BaseQuestionnaireHandler {
    
    NewbornAdmissionResponseHandler() {
        super("newborn-admission");
    }
    
    @Override
    public void processResponses(Document document, final Component root, org.w3c.dom.Document responses) {
        Scenario scenario = ScenarioUtil.getScenario(document.getReference());
        Map<String, String> params = new HashMap<>();
        params.put("mother", "value/relatedperson-mother");
        final Encounter encounter = (Encounter) scenario.parseResource("resource/encounter-newborn-admission.xml", params);
        
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
        
        scenario.createOrUpdateResource(encounter);
    }
    
}
