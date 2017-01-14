/*
 * #%L
 * Medication Administration Record
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
/*
 * Copyright 2015 Cognitive Medical Systems, Inc (http://www.cognitivemedicine.com).
 *
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
 */
package org.hspconsortium.cwfdemo.ui.mar.model;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.dstu3.model.MedicationAdministration;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.exceptions.FHIRException;
import org.hspconsortium.cwf.fhir.common.FhirUtil;
import org.hspconsortium.cwfdemo.ui.mar.render.MarRenderer;
import org.zkoss.zul.ListModelList;

/**
 * Simple model for a dynamic grid that can expand its columns and rows in order to represent a
 * patient's medication administration record (MAR) ordered based on time of administration.
 * <p>
 * The headers of the MAR represent the administration time of a patient medication. It could be a
 * discrete time or a range. The rows of the MAR indicate the medication administered and a
 * checkmark beneath the column that represents its administration time (or the appropriate time
 * range). The MAR provides a quick visual summary of which medication was administered and when it
 * was administered using an administration timeline visual paradigm.
 * 
 * @author Claude Nanjo
 */
public class MarModel {
    
    private static final Log log = LogFactory.getLog(MarModel.class);
    
    /**
     * The column headers for the grid
     */
    private ListModelList<String> headers;
    
    /**
     * The individual medication administration timings
     */
    private ListModelList<List<Object>> rows;
    
    /**
     * The time format used for the column headers
     */
    //	private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d hh:mm");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd h a");//TODO Talk to Emory or Doug to figure out best format
    
    /**
     * Medication index for collapsing administrations for the same medicament
     */
    private Map<String, List<Object>> medicationRowIndex;
    
    /**
     * Map of Orders by ID
     */
    private Map<String, MedicationRequest> orderIndex;
    
    public static final String checkboxPlaceholder = "x";
    
    /**
     * No-arg constructor. If this constructor is called, be sure to also call the init() method.
     */
    public MarModel() {
    }
    
    /**
     * Constructor that initializes this model. Precondition: medAdmins should either be an empty
     * list or a list of medication administrations. It should not be null.
     * 
     * @param medAdmins
     */
    public MarModel(List<MedicationRequest> medOrders, List<MedicationAdministration> medAdmins) {
        this();
        init(medOrders, medAdmins);
        System.out.println(medAdmins);
    }
    
    /**
     * Returns the headers for the medication administration record model.
     * 
     * @return
     */
    public List<String> getHeaders() {
        return headers;
    }
    
    /**
     * Sets the headers for this medication administration record model.
     * 
     * @param headers
     */
    public void setHeaders(ListModelList<String> headers) {
        this.headers = headers;
    }
    
    /**
     * Returns the medication administration records for this MAR.
     * 
     * @return
     */
    public ListModelList<List<Object>> getRows() {
        return rows;
    }
    
    /**
     * Sets the medication administration records for this MAR.
     * 
     * @param rows
     */
    public void setRows(ListModelList<List<Object>> rows) {
        this.rows = rows;
    }
    
    /**
     * Initializes the MAR by transforming each FHIR medication administration into a corresponding
     * row in the MAR.
     * 
     * @param medAdmins
     */
    private void init(List<MedicationRequest> medOrders, List<MedicationAdministration> medAdmins) {
        Map<String, Integer> headerIndex = new HashMap<String, Integer>();
        headers = new ListModelList<String>();
        rows = new ListModelList<List<Object>>();
        medicationRowIndex = new HashMap<String, List<Object>>();
        orderIndex = new HashMap<String, MedicationRequest>();
        
        headers.add("Medication");
        int index = 0;
        for (MedicationAdministration medAdmin : medAdmins) {
            String timeHeader;
            try {
                timeHeader = dateFormat.format(medAdmin.getEffectiveDateTimeType());
            } catch (FHIRException e) {
                timeHeader = "";
            }
            if (!headerIndex.containsKey(timeHeader)) {
                headerIndex.put(timeHeader, index++);
                headers.add(timeHeader);
            }
        }
        
        // Index the orders by ID for easy retrieval
        // TODO May wish to filter somehow in future
        for (MedicationRequest order : medOrders) {
            if (order.getId() != null || order.getIdElement().getValue() == null) {
                orderIndex.put(order.getIdElement().getIdPart(), order);
                String sentence = MarRenderer.generateMedicationOrderSentence(order);
                List<Object> row = medicationRowIndex.get(sentence);
                if (row == null) {
                    row = new ListModelList<Object>(Collections.nCopies(headers.size() + 1, ""));
                    medicationRowIndex.put(sentence, row);
                    row.set(headers.size(), order);
                    row.set(0, sentence);
                    rows.add(row);
                }
            } else {
                log.error("Skipping Order. Medication order does not have an ID");
            }
        }
        
        for (MedicationAdministration medAdmin : medAdmins) {
            try {
                String medicationName = FhirUtil.getDisplayValueForType(medAdmin.getMedicationCodeableConcept());
            } catch (FHIRException e) {
                
            }
            MedicationRequest associatedPrescription = orderIndex
                    .get(medAdmin.getPrescription().getReferenceElement().getIdPart());// TODO Surface reference in generated code
            String sentence = MarRenderer.generateMedicationOrderSentence(associatedPrescription);
            List<Object> row = medicationRowIndex.get(sentence);
            String timeHeader;
            try {
                timeHeader = dateFormat.format(medAdmin.getEffectiveDateTimeType());
            } catch (FHIRException e) {
                timeHeader = "";
            }
            int ind = headerIndex.get(timeHeader);
            // row.set(ind + 1, checkboxPlaceholder);
            //			row.set(ind + 1, "eafry: " + medAdmin.getDosage().getQuantity().getValue() + " "
            //					+ medAdmin.getDosage().getQuantity().getUnit());
            MarRenderer.recordAdministrationNotes(row, ind + 1, medAdmin, associatedPrescription, "eafry");
        }
    }
    
}
