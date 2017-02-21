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

import java.util.HashMap;
import java.util.Map;

import org.carewebframework.web.component.BaseComponent;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestPriority;
import org.hspconsortium.cwf.api.scenario.Scenario;
import org.hspconsortium.cwf.api.scenario.ScenarioUtil;
import org.hspconsortium.cwf.fhir.common.FhirUtil;
import org.hspconsortium.cwf.fhir.document.Document;

public class ProcedureRequestHandler extends BaseQuestionnaireHandler {

    ProcedureRequestHandler() {
        super("procedure-request");
    }

    @Override
    public void processResponses(Document document, final BaseComponent root, org.w3c.dom.Document responses) {
        Scenario scenario = ScenarioUtil.getScenario(document.getReference());
        Map<String, String> params = new HashMap<>();
        params.put("orderedOn", "time/N");
        final ProcedureRequest pr = (ProcedureRequest) scenario
                .parseResource("resource/procedurerequest-lactation-education.xml", params);

        processResponses(responses, new IResponseProcessor() {

            @Override
            public void processResponse(String value, String targetId) {
                BaseComponent target = root.findByName(targetId);
                String type = (String) target.getAttribute("type");

                if ("coding".equals(type)) {
                    String[] pcs = value.split("\\|", 3);
                    CodeableConcept code = FhirUtil.createCodeableConcept(pcs[0], pcs[1], pcs[2]);
                    pr.setCode(code);
                } else if ("priority".equals(type)) {
                    pr.setPriority(ProcedureRequestPriority.valueOf(value.toUpperCase()));
                } else if ("notes".equals(type)) {
                    Annotation annotation = new Annotation().setText(value);
                    pr.addNotes(annotation);
                }
            }

        });

        scenario.createOrUpdateResource(pr);
    }

}
