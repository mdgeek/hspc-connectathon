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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.carewebframework.common.MiscUtil;
import org.carewebframework.common.XMLUtil;
import org.carewebframework.ui.FrameworkController;
import org.carewebframework.ui.zk.ListUtil;
import org.carewebframework.ui.zk.ZKUtil;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Patient;
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
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Textbox;

/**
 * Controller for questionnaires.
 */
public class QuestionnaireController extends FrameworkController implements IDocumentOperation {
    
    private static final long serialVersionUID = 1L;
    
    private Label lblPatientName;
    
    private Combobox cboLocation;
    
    private Component toolbar;
    
    private final DocumentService service;
    
    private Document document;
    
    private final List<IQuestionnaireHandler> questionnaireHandlers = new ArrayList<>();
    
    private DocumentDisplayController controller;
    
    private boolean modified;
    
    final QuestionnaireHandlerRegistry registry;
    
    public QuestionnaireController(DocumentService service, QuestionnaireHandlerRegistry registry) {
        this.service = service;
        this.registry = registry;
    }
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        
        if (cboLocation != null) {
            Bundle locations = service.getClient().search().forResource(Location.class).returnBundle(Bundle.class).execute();
            
            for (Location location : FhirUtil.getEntries(locations, Location.class)) {
                Comboitem item = new Comboitem(location.getName());
                item.setValue(FhirUtil.getResourceIdPath(location));
                cboLocation.appendChild(item);
            }
            
        }
        
        if (!"draft".equalsIgnoreCase(document.getStatus())) {
            disableAll();
        } else {
            String questionnaireIds = (String) comp.getAttribute("handler_id");
            
            if (questionnaireIds != null) {
                for (String id : questionnaireIds.split("\\,")) {
                    IQuestionnaireHandler questionnaireHandler = registry.get(id);
                    
                    if (questionnaireHandler != null) {
                        questionnaireHandlers.add(questionnaireHandler);
                    }
                }
            }
            
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
    
    private void invokeHandlers(org.w3c.dom.Document responses) {
        for (IQuestionnaireHandler handler : questionnaireHandlers) {
            handler.processResponses(document, root, responses);
        }
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
        }
        
        controller.setDocument(null, DocumentAction.DELETED);
    }
    
    public void onClick$btnSign() {
        disableAll();
        document.getReference()
                .setDocStatus(FhirUtil.createCodeableConcept(FhirTerminology.SYS_COGMED, "status-signed", "Signed"));
        org.w3c.dom.Document responses = getResponses();
        saveResponses(responses);
        invokeHandlers(responses);
        controller.refreshListController();
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
