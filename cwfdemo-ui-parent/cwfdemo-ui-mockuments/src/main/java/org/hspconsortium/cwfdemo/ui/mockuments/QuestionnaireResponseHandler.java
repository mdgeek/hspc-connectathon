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

import org.carewebframework.web.component.BaseComponent;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.hspconsortium.cwf.api.scenario.ScenarioUtil;
import org.hspconsortium.cwf.fhir.common.FhirUtil;
import org.hspconsortium.cwf.fhir.document.Document;
import org.hspconsortium.cwf.fhir.document.DocumentService;

public class QuestionnaireResponseHandler extends BaseQuestionnaireHandler {

    private final DocumentService service;

    QuestionnaireResponseHandler(DocumentService service) {
        super("questionnaire-response");
        this.service = service;
    }

    @Override
    public void processResponses(Document document, final BaseComponent root, org.w3c.dom.Document responses) {
        final QuestionnaireResponse qr = new QuestionnaireResponse();
        ScenarioUtil.copyDemoTags(document.getReference(), qr);
        //String ref = (String) root.getAttribute("questionnaire_reference");
        //qr.setQuestionnaire(ref == null ? null : new Reference(ref));
        qr.setSubject(document.getReference().getSubject());
        qr.setAuthor(FhirUtil.getFirst(document.getReference().getAuthor()));
        qr.setStatus(QuestionnaireResponseStatus.COMPLETED);
        qr.setAuthored(new Date());

        processResponses(responses, new BaseQuestionnaireHandler.IResponseProcessor() {

            @Override
            public void processResponse(String value, String targetId) {
                BaseComponent target = root.findByName(targetId);

                if (target.hasAttribute("linkId")) {
                    QuestionnaireResponseItemComponent item = new QuestionnaireResponseItemComponent();
                    qr.addItem(item);
                    item.setLinkId((String) target.getAttribute("linkId"));
                    item.setText((String) target.getAttribute("text"));
                    QuestionnaireResponseItemAnswerComponent answer = new QuestionnaireResponseItemAnswerComponent();
                    item.addAnswer(answer);
                    Coding coding = new Coding();
                    answer.setValue(coding);
                    coding.setSystem((String) target.getAttribute("system"));
                    coding.setDisplay((String) target.getAttribute("display"));
                    String code = (String) target.getAttribute("code");
                    coding.setCode(code == null ? value : code);
                }
            };
        });

        service.createResource(qr);
    }

}
