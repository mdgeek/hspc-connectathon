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

import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestPriority;
import org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestStatus;
import org.hspconsortium.cwf.fhir.common.FhirUtil;
import org.hspconsortium.cwf.fhir.document.Document;
import org.hspconsortium.cwf.fhir.document.DocumentService;
import org.hspconsortium.cwfdemo.api.democonfig.DemoUtils;

public class ProcedureRequestHandler extends BaseQuestionnaireHandler {
    
    
    private final DocumentService service;
    
    ProcedureRequestHandler(DocumentService service) {
        super("procedure-request");
        this.service = service;
    }
    
    @Override
    public void processResponses(Document document, final Component root, org.w3c.dom.Document responses) {
        final ProcedureRequest pr = new ProcedureRequest();
        DemoUtils.addDemoTag(pr);
        pr.setSubject(document.getReference().getSubject());
        pr.setStatus(ProcedureRequestStatus.REQUESTED);
        pr.setOrderedOn(new Date());
        processResponses(responses, new IResponseProcessor() {
            
            
            @Override
            public void processResponse(String value, String targetId) {
                Component target = root.getFellowIfAny(targetId);
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
        
        service.createResource(pr);
    }
    
}
