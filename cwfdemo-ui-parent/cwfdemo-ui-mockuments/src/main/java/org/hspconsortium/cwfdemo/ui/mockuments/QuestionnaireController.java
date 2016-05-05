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

import org.carewebframework.common.MiscUtil;
import org.carewebframework.common.XMLUtil;
import org.carewebframework.ui.FrameworkController;
import org.carewebframework.ui.zk.ListUtil;
import org.carewebframework.ui.zk.ZKUtil;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Textbox;

import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestPriority;
import org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestStatus;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hspconsortium.cwf.fhir.common.FhirTerminology;
import org.hspconsortium.cwf.fhir.common.FhirUtil;
import org.hspconsortium.cwf.fhir.document.Document;
import org.hspconsortium.cwf.fhir.document.DocumentContent;
import org.hspconsortium.cwf.fhir.document.DocumentService;
import org.hspconsortium.cwfdemo.ui.mockuments.DocumentDisplayController.DocumentAction;
import org.hspconsortium.cwfdemo.ui.mockuments.DocumentDisplayController.IDocumentOperation;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Controller for questionnaires.
 */
public class QuestionnaireController extends FrameworkController implements IDocumentOperation {
    
    
    private static final long serialVersionUID = 1L;
    
    private interface IResponseProcessor {
        
        
        void process(String value, Component target);
    }
    
    //@formatter:off
    private enum GeneratedResource {
        QUESTIONNAIRERESPONSE,
        PROCEDUREREQUEST
    };
    //@formatter:on
    
    private Label lblPatientName;
    
    private Component toolbar;
    
    private final DocumentService service;
    
    private Document document;
    
    private GeneratedResource generatedResource;
    
    private DocumentDisplayController controller;
    
    private boolean modified;
    
    public QuestionnaireController(DocumentService service) {
        this.service = service;
    }
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        
        if (!"draft".equalsIgnoreCase(document.getStatus())) {
            disableAll();
            generatedResource = null;
        } else {
            String gr = (String) comp.getAttribute("generated_resource");
            generatedResource = gr == null ? null : GeneratedResource.valueOf(gr.toUpperCase());
            ZKUtil.wireChangeEvents(comp, comp, "onChanged");
            controller.setDocumentOperation(this);
            
            if (toolbar != null) {
                controller.addToToolbar(toolbar);
            }
        }
        
        loadResponses();
    }
    
    public void setDocument(Document document) {
        this.document = document;
    }
    
    private void disableAll() {
        ZKUtil.disableChildren(root, true);
        
        if (toolbar != null) {
            controller.removeFromToolbar(toolbar);
        }
    }
    
    public void setDisplayController(DocumentDisplayController controller) {
        this.controller = controller;
    }
    
    private void loadResponses() {
        setPatient((Patient) service.getResource(document.getReference().getSubject()));
        DocumentContent content = FhirUtil.getFirst(document.getContent());
        NodeList responses = null;
        
        try {
            responses = content == null ? null
                    : XMLUtil.parseXMLFromString(content.toString()).getElementsByTagName("response");
        } catch (Exception e) {}
        
        if (responses == null) {
            return;
        }
        
        for (int i = 0; i < responses.getLength(); i++) {
            Node response = responses.item(i);
            NamedNodeMap attr = response.getAttributes();
            String value = attr.getNamedItem("value").getNodeValue();
            String id = attr.getNamedItem("target").getNodeValue();
            Component target = root.getFellowIfAny(id);
            
            if (target instanceof Checkbox) {
                ((Checkbox) target).setChecked("true".equals(value));
            } else if (target instanceof Datebox) {
                ((Datebox) target).setValue(new Date(Long.parseLong(value)));
            } else if (target instanceof Combobox) {
                if (ListUtil.selectComboboxData((Combobox) target, value) == -1) {
                    ListUtil.selectComboboxItem((Combobox) target, value);
                }
            } else if (target instanceof Listbox) {
                if (ListUtil.selectListboxData((Listbox) target, value) == -1) {
                    ListUtil.selectListboxItem((Listbox) target, value);
                }
            } else if (target instanceof Textbox) {
                ((Textbox) target).setValue(value);
            }
        }
        
        modified = false;
    }
    
    private void saveResponses(org.w3c.dom.Document responses) {
        DocumentContent content = new DocumentContent(XMLUtil.toString(responses).getBytes(), document.getContentType());
        document.getContent().clear();
        document.getContent().add(content);
        service.updateDocument(document);
        modified = false;
    }
    
    private org.w3c.dom.Document getResponses() {
        try {
            org.w3c.dom.Document responses = XMLUtil.parseXMLFromString("<responses/>");
            getResponses(root, responses.getElementsByTagName("responses").item(0));
            return responses;
        } catch (Exception e) {
            throw MiscUtil.toUnchecked(e);
        }
    }
    
    private void getResponses(Component comp, Node responses) {
        for (Component child : comp.getChildren()) {
            String id = child.getId();
            
            if (id != null && !id.isEmpty()) {
                String value = null;
                
                if (child instanceof Checkbox) {
                    value = ((Checkbox) child).isChecked() ? "true" : null;
                } else if (child instanceof Datebox) {
                    Date date = ((Datebox) child).getValue();
                    value = date == null ? null : Long.toString(date.getTime());
                } else if (child instanceof Combobox) {
                    Comboitem item = ((Combobox) child).getSelectedItem();
                    
                    if (item != null) {
                        value = item.getValue();
                        value = value == null ? item.getLabel() : value;
                    }
                } else if (child instanceof Listbox) {
                    Listitem item = ((Listbox) child).getSelectedItem();
                    
                    if (item != null) {
                        value = item.getValue();
                        value = value == null ? item.getLabel() : value;
                    }
                } else if (child instanceof Textbox) {
                    value = ((Textbox) child).getText();
                }
                
                if (value != null && !value.isEmpty()) {
                    Element node = responses.getOwnerDocument().createElement("response");
                    node.setAttribute("target", id);
                    node.setAttribute("value", value);
                    responses.appendChild(node);
                }
            }
            
            getResponses(child, responses);
        }
    }
    
    private void createResource(org.w3c.dom.Document responses) {
        if (generatedResource == null) {
            return;
        }
        
        IBaseResource resource = null;
        
        switch (generatedResource) {
            case QUESTIONNAIRERESPONSE:
                resource = buildQuestionnaireResponse(responses);
                break;
            
            case PROCEDUREREQUEST:
                resource = buildProcedureRequest(responses);
                break;
            
        }
        
        if (resource != null) {
            service.createResource(resource);
        }
    }
    
    private void processResponses(org.w3c.dom.Document responses, IResponseProcessor processor) {
        NodeList nodeList = responses.getElementsByTagName("response");
        
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node response = nodeList.item(i);
            NamedNodeMap attr = response.getAttributes();
            String value = attr.getNamedItem("value").getNodeValue();
            String id = attr.getNamedItem("target").getNodeValue();
            Component target = root.getFellowIfAny(id);
            processor.process(value, target);
        }
    }
    
    private QuestionnaireResponse buildQuestionnaireResponse(org.w3c.dom.Document responses) {
        final QuestionnaireResponse qr = new QuestionnaireResponse();
        //String ref = (String) root.getAttribute("questionnaire_reference");
        //qr.setQuestionnaire(ref == null ? null : new Reference(ref));
        qr.setSubject(document.getReference().getSubject());
        qr.setAuthor(FhirUtil.getFirst(document.getReference().getAuthor()));
        qr.setStatus(QuestionnaireResponseStatus.COMPLETED);
        qr.setAuthored(new Date());
        
        processResponses(responses, new IResponseProcessor() {
            
            
            @Override
            public void process(String value, Component target) {
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
                    coding.setCode(value);
                }
            };
        });
        
        return qr;
    }
    
    private ProcedureRequest buildProcedureRequest(org.w3c.dom.Document responses) {
        final ProcedureRequest pr = new ProcedureRequest();
        pr.setSubject(document.getReference().getSubject());
        pr.setStatus(ProcedureRequestStatus.REQUESTED);
        pr.setOrderedOn(new Date());
        processResponses(responses, new IResponseProcessor() {
            
            
            @Override
            public void process(String value, Component target) {
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
        
        return pr;
    }
    
    private void setPatient(Patient patient) {
        if (lblPatientName != null) {
            lblPatientName.setValue("Patient: " + FhirUtil.formatName(patient.getName()));
        }
    }
    
    public void onClick$btnSave() {
        saveChanges();
    }
    
    public void onClick$btnDelete() {
        if (document.getReference().hasId()) {
            service.deleteResource(document.getReference());
            controller.setDocument(null, DocumentAction.DISCARD);
        }
        
        root.detach();
    }
    
    public void onClick$btnSign() {
        disableAll();
        document.getReference()
                .setDocStatus(FhirUtil.createCodeableConcept(FhirTerminology.SYS_COGMED, "status-signed", "Signed"));
        org.w3c.dom.Document responses = getResponses();
        saveResponses(responses);
        createResource(responses);
    }
    
    public void onChanged() {
        modified = true;
    }
    
    @Override
    public boolean hasChanged() {
        return modified;
    }
    
    @Override
    public void saveChanges() {
        saveResponses(getResponses());
    }
    
    @Override
    public void cancelChanges() {
    }
}
