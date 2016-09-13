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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.carewebframework.api.query.AbstractQueryFilter;
import org.carewebframework.api.query.DateQueryFilter.DateType;
import org.carewebframework.api.query.IQueryContext;
import org.carewebframework.ui.zk.ListUtil;
import org.carewebframework.ui.zk.PromptDialog;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hspconsortium.cwf.api.patient.PatientContext;
import org.hspconsortium.cwf.fhir.common.FhirTerminology;
import org.hspconsortium.cwf.fhir.common.FhirUtil;
import org.hspconsortium.cwf.fhir.document.Document;
import org.hspconsortium.cwf.fhir.document.DocumentContent;
import org.hspconsortium.cwf.fhir.document.DocumentListDataService;
import org.hspconsortium.cwf.fhir.document.DocumentService;
import org.hspconsortium.cwf.ui.reporting.controller.AbstractListController;
import org.hspconsortium.cwfdemo.api.democonfig.ScenarioUtil;
import org.hspconsortium.cwfdemo.ui.mockuments.DocumentDisplayController.DocumentAction;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Listitem;

/**
 * Controller for the list-based display of clinical documents.
 */
public class DocumentListController extends AbstractListController<Document, Document> {
    
    /**
     * Handles filtering by document type.
     */
    private class DocumentTypeFilter extends AbstractQueryFilter<Document> {
        
        @Override
        public boolean include(Document document) {
            String filter = getCurrentFilter();
            return filter == null || document.hasType(filter);
        }
        
        @Override
        public boolean updateContext(IQueryContext context) {
            context.setParam("type", getCurrentFilter());
            return true;
        }
        
    }
    
    private static final long serialVersionUID = 1L;
    
    private Combobox cboFilter;
    
    private Comboitem cbiSeparator;
    
    private Label lblFilter;
    
    private Label lblInfo;
    
    private String fixedFilter;
    
    private DocumentDisplayController displayController;
    
    private Document selectedDocument;
    
    private final Collection<String> allTypes;
    
    private final DocumentService documentService;
    
    public DocumentListController(DocumentService service) {
        super(new DocumentListDataService(service), "cwfdocuments", "DOCUMENT", "documentsPrint.css");
        this.documentService = service;
        setPaging(false);
        registerQueryFilter(new DocumentTypeFilter());
        allTypes = service.getTypes();
    }
    
    @Override
    public void initializeController() {
        super.initializeController();
        getContainer().registerProperties(this, "fixedFilter");
        addFilters(allTypes, null, null);
    }
    
    /**
     * This is a good place to update the filter list.
     */
    @Override
    protected List<Document> toModel(List<Document> queryResult) {
        if (queryResult != null) {
            updateListFilter(queryResult);
        }
        
        return queryResult;
    }
    
    protected void setDisplayController(DocumentDisplayController displayController) {
        this.displayController = displayController;
    }
    
    /**
     * Presents a quick pick list limited to types present in the unfiltered document list.
     *
     * @param documents The unfiltered document list.
     */
    private void updateListFilter(List<Document> documents) {
        if (fixedFilter != null) {
            return;
        }
        
        List<Comboitem> items = cboFilter.getItems();
        Set<String> types = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        String currentFilter = getCurrentFilter();
        
        while (items.get(1) != cbiSeparator) {
            items.remove(1);
        }
        
        cboFilter.setSelectedIndex(0);
        
        if (documents != null) {
            for (Document doc : documents) {
                types.addAll(doc.getTypes());
            }
        }
        
        addFilters(types, cbiSeparator, currentFilter);
        
        if (currentFilter != null && cboFilter.getSelectedIndex() < 1) {
            ListUtil.selectComboboxItem(cboFilter, currentFilter);
        }
    }
    
    private void addFilters(Collection<String> types, Component ref, String selected) {
        for (String type : types) {
            Comboitem item = new Comboitem(type);
            item.setValue(type);
            
            cboFilter.insertBefore(item, ref);
            
            if (type.equals(selected)) {
                cboFilter.setSelectedItem(item);
            }
        }
    }
    
    /**
     * Returns the currently active type filter.
     *
     * @return The currently active type filter.
     */
    private String getCurrentFilter() {
        return fixedFilter != null ? fixedFilter
                : cboFilter.getSelectedIndex() > 0 ? (String) cboFilter.getSelectedItem().getValue() : null;
    }
    
    /**
     * Handle change in type filter selection.
     */
    public void onSelect$cboFilter() {
        applyFilters();
    }
    
    /**
     * Selecting document displays view.
     */
    public void onSelect$listBox() {
        Listitem item = listBox.getSelectedItem();
        setSelectedDocument(item == null ? null : (Document) item.getValue());
    }
    
    public void onClick$btnNew() {
        if (!allowChange()) {
            return;
        }
        
        List<String> itemNames = new ArrayList<>();
        List<Object> items = new ArrayList<>();
        itemNames.add("Lactation Assessment");
        items.add("lactation_assessment");
        itemNames.add("Newborn Admission");
        items.add("newborn-admission");
        itemNames.add("Procedure Request");
        items.add("procedure-request");
        String item = (String) PromptDialog.input("Select document type to create.", "New Document", null, itemNames, items);
        
        if (item == null) {
            return;
        }
        
        String displayName = itemNames.get(items.indexOf(item));
        DocumentReference ref = new DocumentReference();
        Patient patient = PatientContext.getActivePatient();
        ScenarioUtil.copyDemoTags(patient, ref);
        String id = FhirUtil.getResourceIdPath(patient);
        ref.setSubject(new Reference(id));
        ref.setCreated(new Date());
        ref.setType(FhirUtil.createCodeableConcept(FhirTerminology.SYS_COGMED, item, displayName));
        ref.setDocStatus(FhirUtil.createCodeableConcept(FhirTerminology.SYS_COGMED, "status-draft", "Draft"));
        DocumentContent content = new DocumentContent("<responses/>".getBytes(),
                DocumentDisplayController.QUESTIONNAIRE_CONTENT_TYPE + item);
        Document newDocument = new Document(ref, content);
        documentService.updateDocument(newDocument);
        refresh();
        setSelectedDocument(newDocument);
    }
    
    private void setSelectedDocument(Document document) {
        if (document == selectedDocument || !allowChange()) {
            return;
        }
        
        selectedDocument = document;
        highlightSelectedDocument();
        displayController.setDocument(document, DocumentAction.DISCARD);
    }
    
    private void highlightSelectedDocument() {
        if (selectedDocument == null) {
            listBox.clearSelection();
            return;
        }
        
        Listitem item = listBox.getSelectedItem();
        
        if (item != null && item.getValue() == selectedDocument) {
            return;
        }
        
        DocumentReference reference = selectedDocument.getReference();
        
        for (Document doc : getFilteredModel()) {
            if (FhirUtil.areEqual(reference, doc.getReference(), true)) {
                ListUtil.selectListboxData(listBox, doc);
                selectedDocument = doc;
                return;
            }
        }
        
        getFilteredModel().add(selectedDocument);
        highlightSelectedDocument();
    }
    
    @Override
    protected void afterModelChanged() {
        highlightSelectedDocument();
    }
    
    @Override
    protected void onPatientChanged(Patient patient) {
        setSelectedDocument(null);
        super.onPatientChanged(patient);
    }
    
    @Override
    protected String onPatientChanging(boolean silent) {
        return displayController.setDocument(null, silent ? DocumentAction.SAVE : null) ? null : "Edit in progress.";
    }
    
    private boolean allowChange() {
        return displayController.setDocument(null, null);
    }
    
    /**
     * Returns the fixed filter, if any.
     *
     * @return The fixed filter.
     */
    public String getFixedFilter() {
        return fixedFilter;
    }
    
    /**
     * Sets the fixed filter.
     *
     * @param name The fixed filter.
     */
    public void setFixedFilter(String name) {
        fixedFilter = name;
        cboFilter.setVisible(fixedFilter == null);
        lblFilter.setVisible(fixedFilter != null);
        lblFilter.setValue(fixedFilter);
        refresh();
    }
    
    @Override
    protected void setListModel(ListModel<Document> model) {
        super.setListModel(model);
        int docCount = model == null ? 0 : model.getSize();
        lblInfo.setValue(docCount + " document(s)");
    }
    
    @Override
    public Date getDateByType(Document result, DateType dateMode) {
        return result.getDateTime();
    }
    
}
