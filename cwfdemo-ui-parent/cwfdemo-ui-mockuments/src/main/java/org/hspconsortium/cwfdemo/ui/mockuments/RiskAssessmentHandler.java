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

import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.RiskAssessment;
import org.hspconsortium.cwf.fhir.common.FhirUtil;
import org.hspconsortium.cwf.fhir.document.Document;
import org.hspconsortium.cwf.fhir.document.DocumentService;
import org.hspconsortium.cwfdemo.api.democonfig.ScenarioUtil;
import org.zkoss.zk.ui.Component;

public class RiskAssessmentHandler extends BaseQuestionnaireHandler {
    
    private final DocumentService service;
    
    RiskAssessmentHandler(DocumentService service) {
        super("risk-assessment");
        this.service = service;
    }
    
    @Override
    public void processResponses(Document document, final Component root, org.w3c.dom.Document responses) {
        final RiskAssessment ra = new RiskAssessment();
        ScenarioUtil.copyDemoTags(document.getReference(), ra);
        ra.setSubject(document.getReference().getSubject());
        ra.setPerformer(FhirUtil.getFirst(document.getReference().getAuthor()));
        ra.setOccurrence(new DateTimeType(new Date()));
        processResponses(responses, new BaseQuestionnaireHandler.IResponseProcessor() {
            
            @Override
            public void processResponse(String value, String targetId) {
                Component target = root.getFellowIfAny(targetId);
                
                if (target != null && "169641007".equals(target.getAttribute("code"))) {
                    ra.setMethod(FhirUtil.createCodeableConcept("Feeding Intention-Not-To-Breastfeed",
                        "Feeding Intention-Not-To-Breastfeed", "Feeding Intention-Not-To-Breastfeed"));
                }
            };
        });
        
        if (ra.hasMethod()) {
            service.createResource(ra);
        }
    }
    
}
