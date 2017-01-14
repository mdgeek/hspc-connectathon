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
/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 * 
 * This Source Code Form is also subject to the terms of the Health-Related Additional
 * Disclaimer of Warranty and Limitation of Liability available at
 * http://www.carewebframework.org/licensing/disclaimer.
 */
package org.hspconsortium.cwfdemo.ui.mar.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carewebframework.shell.plugins.PluginContainer;
import org.carewebframework.shell.plugins.PluginController;
import org.carewebframework.ui.zk.PromptDialog;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DosageInstruction;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.MedicationAdministration;
import org.hl7.fhir.dstu3.model.MedicationAdministration.MedicationAdministrationDosageComponent;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Timing;
import org.hl7.fhir.dstu3.model.Timing.TimingRepeatComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.hspconsortium.cwf.api.patient.PatientContext;
import org.hspconsortium.cwf.fhir.common.FhirUtil;
import org.hspconsortium.cwf.fhir.medication.MedicationService;
import org.hspconsortium.cwfdemo.api.eps.EPSService;
import org.hspconsortium.cwfdemo.ui.mar.MedicationActionUtil;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Textbox;

/**
 * 
 */
public class SimpleMedicationAdministrationController extends PluginController {
    
    private static final long serialVersionUID = 1908890098540845685L;
    
    private static final Log log = LogFactory.getLog(SimpleMedicationAdministrationController.class);
    
    /**
     * Service used to create, update, and/or delete medication resources from a FHIR server to
     * support MAR operations.
     */
    private final MedicationService medicationService;
    
    private final EPSService epsService;
    
    /**
     * Textbox for the doseQuantity
     */
    private Decimalbox doseQuantity;
    
    /**
     * Decimal box holding duration for administration
     */
    private Decimalbox duration;
    
    /**
     * Checkbox for whether administration should be PRN
     */
    private Checkbox isPRN;
    
    /**
     * Combo box for the selection of allowable patient medication. At this time, it is hard-coded
     * for the HIMSS demo. In the future, it will be patient-based and dynamic.
     */
    private Combobox medSelector;
    
    /**
     * Combo box for the selection of allowable dose units for the selected medication. At this
     * time, the codeset is hard-coded for the HIMSS demo. It will be dynamic in the future based on
     * the medication type (provided an ontology is available).
     */
    private Combobox doseUnitSelector;
    
    /**
     * Combo box for the selection of allowable routes of administration. At this time, the codeset
     * is hard-coded for the HIMSS demo. It will be dynamic in the future based on the medication
     * type (provided an ontology is available).
     */
    private Combobox routeOfAdminSelector;
    
    /**
     * Combo box for the selection of allowable frequencies. At this time, the codeset is hard-coded
     * for the HIMSS demo. It will be dynamic in the future based on the medication type (provided
     * an ontology is available).
     */
    private Combobox frequencySelector;
    
    /**
     * Combo box for the selection of allowable units of time for medication administration
     * durations. At this time, the codeset is hard-coded for the HIMSS demo. It will be dynamic in
     * the future based on the medication type (provided an ontology is available).
     */
    private Combobox unitOfTimeSelector;
    
    /**
     * Combo box for the selection of PRN Reasons for medication administration durations. At this
     * time, the codeset is hard-coded for the HIMSS demo. It will be dynamic in the future based on
     * the medication type (provided an ontology is available).
     */
    private Combobox prnReasonSelector;
    
    /**
     * Reason for the order or administration
     */
    private Textbox txReason;
    
    /**
     * Convenience identifier to group medication administrations created using the MAR. This
     * identifier can be useful for bulk updates and deletes.
     */
    private final Identifier generatedMedAdminsIdentifier = new Identifier()
            .setSystem("urn:cogmedsys:hsp:model:medicationadministration").setValue("gen");
    
    /**
     * Convenience identifier to group medication administrations created using the MAR. This
     * identifier can be useful for bulk updates and deletes.
     */
    private final Identifier generatedMedOrderIdentifier = new Identifier()
            .setSystem("urn:cogmedsys:hsp:model:medicationorder").setValue("gen");
    
    /**
     * No-arg constructor
     * 
     * @param epsService
     * @param medicationService
     */
    public SimpleMedicationAdministrationController(EPSService epsService, MedicationService medicationService) {
        this.epsService = epsService;
        this.medicationService = medicationService;
    }
    
    /*********************************************************************************************
     * Accessor Methods
     *********************************************************************************************/
    
    /**
     * @return
     */
    public Decimalbox getDoseQuantity() {
        return doseQuantity;
    }
    
    public void setDoseQuantity(Decimalbox doseQuantity) {
        this.doseQuantity = doseQuantity;
    }
    
    public Decimalbox getDuration() {
        return duration;
    }
    
    public void setDuration(Decimalbox duration) {
        this.duration = duration;
    }
    
    public Checkbox getIsPRN() {
        return isPRN;
    }
    
    public void setIsPRN(Checkbox isPRN) {
        this.isPRN = isPRN;
    }
    
    public Combobox getMedSelector() {
        return medSelector;
    }
    
    public void setMedSelector(Combobox medSelector) {
        this.medSelector = medSelector;
    }
    
    public Combobox getDoseUnitSelector() {
        return doseUnitSelector;
    }
    
    public void setDoseUnitSelector(Combobox doseUnitSelector) {
        this.doseUnitSelector = doseUnitSelector;
    }
    
    public Combobox getRouteOfAdminSelector() {
        return routeOfAdminSelector;
    }
    
    public void setRouteOfAdminSelector(Combobox routeOfAdminSelector) {
        this.routeOfAdminSelector = routeOfAdminSelector;
    }
    
    public Combobox getFrequencySelector() {
        return frequencySelector;
    }
    
    public void setFrequencySelector(Combobox frequencySelector) {
        this.frequencySelector = frequencySelector;
    }
    
    public Combobox getUnitOfTimeSelector() {
        return unitOfTimeSelector;
    }
    
    public void setUnitOfTimeSelector(Combobox unitOfTimeSelector) {
        this.unitOfTimeSelector = unitOfTimeSelector;
    }
    
    public Combobox getPrnReasonSelector() {
        return prnReasonSelector;
    }
    
    public void setPrnReasonSelector(Combobox prnReasonSelector) {
        this.prnReasonSelector = prnReasonSelector;
    }
    
    public Textbox getTxReason() {
        return txReason;
    }
    
    public void setTxReason(Textbox txReason) {
        this.txReason = txReason;
    }
    
    /**
     * Method populates the administrable medications for the patient
     * 
     * @param order
     */
    public void populateMedSelector(MedicationRequest order) {//TODO For demo only. In future, reference a repository
        Map<String, Comboitem> meds = new HashMap<String, Comboitem>();
        medSelector.getItems().clear();
        Comboitem metoprolol = new Comboitem();
        metoprolol.setValue("372891");
        metoprolol.setLabel("metoprolol tartrate 25 MG Oral Tablet");
        meds.put("372891", metoprolol);
        Comboitem atenolol = new Comboitem();
        atenolol.setValue("197379");
        atenolol.setLabel("Atenolol 100 MG Oral Tablet");
        meds.put("197379", atenolol);
        Comboitem bisoprolol = new Comboitem();
        bisoprolol.setValue("854901");
        bisoprolol.setLabel("Bisoprolol Fumarate 10 MG Oral Tablet");
        meds.put("854901", bisoprolol);
        Comboitem clopidogrel = new Comboitem();
        clopidogrel.setValue("309362");
        clopidogrel.setLabel("clopidogrel 75 MG Oral Tablet");
        meds.put("309362", clopidogrel);
        Comboitem amlodipine = new Comboitem();
        amlodipine.setValue("597967");
        amlodipine.setLabel("Amlodipine 10 MG / atorvastatin 20 MG Oral Tablet");
        meds.put("597967", amlodipine);
        Comboitem acetaminophen = new Comboitem();
        acetaminophen.setValue("665056");
        acetaminophen.setLabel("Acetaminophen 500 MG Chewable Tablet");
        meds.put("665056", acetaminophen);
        Comboitem aspirin = new Comboitem();
        aspirin.setValue("198466");
        aspirin.setLabel("Aspirin 325 MG Oral Capsule");
        meds.put("198466", aspirin);
        Comboitem hydrochlorothiazide = new Comboitem();
        hydrochlorothiazide.setValue("310798");
        hydrochlorothiazide.setLabel("Hydrochlorothiazide 25 MG Oral Tablet");
        meds.put("310798", hydrochlorothiazide);
        Comboitem bisacodyl = new Comboitem();
        bisacodyl.setValue("1550933");
        bisacodyl.setLabel("Bisacodyl 5 MG Oral Tablet");
        meds.put("1550933", bisacodyl);
        Comboitem acetazolamide = new Comboitem();
        acetazolamide.setValue("197304");
        acetazolamide.setLabel("Acetazolamide 250 MG Oral Tablet");
        meds.put("197304", acetazolamide);
        
        medSelector.appendChild(metoprolol);
        medSelector.appendChild(atenolol);
        medSelector.appendChild(bisoprolol);
        medSelector.appendChild(clopidogrel);
        medSelector.appendChild(amlodipine);
        medSelector.appendChild(acetaminophen);
        medSelector.appendChild(aspirin);
        medSelector.appendChild(hydrochlorothiazide);
        medSelector.appendChild(bisacodyl);
        medSelector.appendChild(acetazolamide);
        
        Coding selectedMed = null;
        if (order != null) {
            try {
                selectedMed = FhirUtil.getFirst(order.getMedicationCodeableConcept().getCoding());
                Comboitem item = meds.get(selectedMed.getCode());
                if (item != null) {
                    medSelector.setSelectedItem(item);
                }
            } catch (FHIRException e) {}
        }
    }
    
    /**
     * Populates Dose Unit code set
     * 
     * @param order
     */
    public void populateDoseUnitSelector(MedicationRequest order) {//TODO For demo only. In future, reference a knowledge base
        doseUnitSelector.getItems().clear();
        Comboitem tablet = new Comboitem();
        tablet.setValue("{tbl}");
        tablet.setLabel("Tablet");
        Comboitem mg = new Comboitem();
        mg.setValue("mg");
        mg.setLabel("Milligram");
        doseUnitSelector.appendChild(tablet);
        doseUnitSelector.appendChild(mg);
        
        if (order != null) {
            Quantity qty = (Quantity) order.getDosageInstruction().get(0).getDose();//TODO Fix in code generator
            for (Comboitem item : doseUnitSelector.getItems()) {
                if (item.getValue().equals(qty.getUnit())) {
                    doseUnitSelector.setSelectedItem(item);
                }
                doseQuantity.setValue(qty.getValue());
            }
        }
    }
    
    /**
     * Populates frequency code set
     * 
     * @param order
     */
    public void populateFrequencySelector(MedicationRequest order) {//TODO For demo only. In future, reference a knowledge base
        if (frequencySelector != null) {//frequencySelector will be null for medication administrations
            frequencySelector.getItems().clear();
            Comboitem qd = new Comboitem();
            qd.setValue("1");
            qd.setLabel("QD");
            Comboitem q8h = new Comboitem();
            q8h.setValue("2");
            q8h.setLabel("Q8H");
            frequencySelector.appendChild(qd);
            frequencySelector.appendChild(q8h);
        }
    }
    
    /**
     * Populates route of administration code set
     * 
     * @param order
     */
    public void populateRouteOfAdminSelector(MedicationRequest order) {//TODO For demo only. In future, reference a knowledge base
        routeOfAdminSelector.getItems().clear();
        Comboitem oral = new Comboitem();
        oral.setValue("26643006");
        oral.setLabel("Oral route");
        Comboitem topical = new Comboitem();
        topical.setValue("6064005");
        topical.setLabel("Topical route");
        routeOfAdminSelector.appendChild(oral);
        routeOfAdminSelector.appendChild(topical);
        
        if (order != null) {
            Coding route = FhirUtil.getFirst(order.getDosageInstruction().get(0).getRoute().getCoding());//TODO Fix in code generator
            for (Comboitem item : routeOfAdminSelector.getItems()) {
                if (item.getValue().equals(route.getCode())) {
                    routeOfAdminSelector.setSelectedItem(item);
                }
            }
        }
    }
    
    /**
     * Populates unit of time code set
     */
    public void populateUnitOfTimeSelector() {//TODO For demo only. In future, reference a knowledge base
        if (unitOfTimeSelector != null) {//unitOfTimeSelector will be null for medication administrations
            unitOfTimeSelector.getItems().clear();
            Comboitem min = new Comboitem();
            min.setValue("min");
            min.setLabel("Minute");
            Comboitem hour = new Comboitem();
            hour.setValue("h");
            hour.setLabel("Hour");
            Comboitem day = new Comboitem();
            day.setValue("d");
            day.setLabel("Day");
            Comboitem week = new Comboitem();
            week.setValue("wk");
            week.setLabel("Week");
            unitOfTimeSelector.appendChild(min);
            unitOfTimeSelector.appendChild(hour);
            unitOfTimeSelector.appendChild(day);
            unitOfTimeSelector.appendChild(week);
        }
    }
    
    /**
     * Populates PRN Reason Code Set
     */
    public void populatePrnReasonSelector() {//TODO For demo only. In future, reference a knowledge base
        if (prnReasonSelector != null) {//prnReasonSelector will be null for medication administrations
            prnReasonSelector.getItems().clear();
            Comboitem pain = new Comboitem();
            pain.setValue("1");
            pain.setLabel("Pain");
            Comboitem hypertension = new Comboitem();
            hypertension.setValue("2");
            hypertension.setLabel("Hypertention");
            Comboitem fever = new Comboitem();
            fever.setValue("3");
            fever.setLabel("Fever");
            Comboitem heartburn = new Comboitem();
            heartburn.setValue("4");
            heartburn.setLabel("Heartburn");
            Comboitem indigestion = new Comboitem();
            indigestion.setValue("5");
            indigestion.setLabel("Indigestion");
            Comboitem anxiety = new Comboitem();
            anxiety.setValue("6");
            anxiety.setLabel("Anxiety");
            prnReasonSelector.appendChild(pain);
            prnReasonSelector.appendChild(hypertension);
            prnReasonSelector.appendChild(fever);
            prnReasonSelector.appendChild(heartburn);
            prnReasonSelector.appendChild(indigestion);
            prnReasonSelector.appendChild(anxiety);
        }
    }
    
    /******************************************************************
     * Overridden Supertype Methods
     ******************************************************************/
    
    /**
     * @see org.carewebframework.ui.FrameworkController#doAfterCompose(org.zkoss.zk.ui.Component)
     */
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        try {
            MedicationRequest order = (MedicationRequest) arg.get(MedicationActionUtil.MED_ORDER_KEY);
            populateMedSelector(order);
            populateDoseUnitSelector(order);
            populateFrequencySelector(order);
            populateRouteOfAdminSelector(order);
            populateUnitOfTimeSelector();
            populatePrnReasonSelector();
            log.trace("Controller composed");
        } catch (Exception e) {
            log.error("Error initializing controller", e);
            throw new RuntimeException("Error initializing SimpleMedicationAdministrationController", e);
        }
    }
    
    /**
     * @see org.carewebframework.shell.plugins.IPluginEvent#onLoad(org.carewebframework.shell.plugins.PluginContainer)
     */
    @Override
    public void onLoad(PluginContainer container) {
        super.onLoad(container);
        //        container.registerProperties(this, "banner");
    }
    
    /**
     * @see org.carewebframework.shell.plugins.IPluginEvent#onUnload()
     */
    @Override
    public void onUnload() {
        super.onUnload();
    }
    
    /**
     * @see org.carewebframework.shell.plugins.IPluginEvent#onActivate()
     */
    @Override
    public void onActivate() {
        super.onActivate();
    }
    
    /**
     * @see org.carewebframework.shell.plugins.IPluginEvent#onInactivate()
     */
    @Override
    public void onInactivate() {
        super.onInactivate();
    }
    
    /******************************************************************
     * Event Handling Methods
     ******************************************************************/
    /**
     * Creates a new medication administration entry if a selection has been made in the medication
     * combo box. If there is not medication selected or no active patient then the method will do
     * nothing.
     * 
     * @param event
     */
    public void onClick$btnMarAdminister(Event event) {
        
        Patient patient = PatientContext.getActivePatient();
        
        MedicationInterventionFormHelper formHelper = new MedicationInterventionFormHelper(this);
        formHelper.initialize();
        
        if (formHelper.meetsMedicationAdministrationRequirements()) {
            MedicationAdministration administration = buildMedicationAdministration(formHelper, patient);
            try {
                medicationService.createMedicationAdministration(administration);
                try {
                    publishJsonPayloadToEpsTopic(administration, MainController.MED_TOPIC);
                } catch (Exception e) {
                    log.error("Error Invoking EPS", e);
                    PromptDialog.showWarning("EPS not reachable", "Please ensure EPS service is up and reachable");
                }
                event.getTarget().getRoot().detach();
                //initializeMar();
            } catch (Exception e) {
                log.error("Error adding new medication administration", e);
            }
        } else {
            log.info("No patient context provided or medication selected");//TODO Button must be disabled if no patient has been selected.
            PromptDialog.showWarning("You must first select a patient and/or a medication to administer",
                "Please first select a patient and medication");
        }
    }
    
    /**
     * Creates a new medication administration entry if a selection has been made in the medication
     * combo box. If there is not medication selected or no active patient then the method will do
     * nothing.
     * 
     * @param event
     */
    public void onClick$btnOrderMedication(Event event) {
        
        Patient patient = PatientContext.getActivePatient();
        
        MedicationInterventionFormHelper formHelper = new MedicationInterventionFormHelper(this);
        formHelper.initialize();
        
        if (formHelper.meetsOrderRequirements()) {
            MedicationRequest order = buildMedicationOrder(formHelper, patient);
            try {
                medicationService.createMedicationOrder(order);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            log.info("No patient context provided or medication selected");//TODO Button must be disabled if no patient has been selected.
            PromptDialog.showWarning("You must first select a patient and/or a medication to administer",
                "Please first select a patient and medication");
        }
        event.getTarget().getRoot().detach();
    }
    
    /**
     * Method builds and persists a medication administration entered into a form.
     * 
     * @param formHelper
     * @param patient
     * @return
     */
    public MedicationAdministration buildMedicationAdministration(MedicationInterventionFormHelper formHelper,
                                                                  Patient patient) {
        MedicationRequest order = (MedicationRequest) arg.get(MedicationActionUtil.MED_ORDER_KEY);
        MedicationAdministration administration = new MedicationAdministration();
        administration.setPrescriptionTarget(order);
        administration.setMedication(formHelper.getSelectedMedication());
        DateTimeType administrationTime = new DateTimeType();
        administration.setEffective(administrationTime);
        MedicationAdministrationDosageComponent dose = new MedicationAdministrationDosageComponent();
        administration.setDosage(dose);
        dose.setDose(formHelper.getDoseQuantity());
        administration.addIdentifier(generatedMedAdminsIdentifier);
        dose.setRoute(formHelper.getSelectedRoute());
        administration.setPatientTarget(patient);
        return administration;
    }
    
    /**
     * Method builds and persists a medication order entered into a form.
     * 
     * @param formHelper
     * @param patient
     * @return
     */
    public MedicationRequest buildMedicationOrder(MedicationInterventionFormHelper formHelper, Patient patient) {
        MedicationRequest order = new MedicationRequest();
        order.addIdentifier(generatedMedOrderIdentifier);
        order.setMedication(formHelper.getSelectedMedication());
        Date administrationTime = new Date();
        order.setDateWritten(administrationTime);
        DosageInstruction dosageInstructions = new DosageInstruction();
        order.addDosageInstruction(dosageInstructions);
        dosageInstructions.setDose(formHelper.getDoseQuantity());//TODO Fix getAdaptee indirection in code generator
        //dosageInstructions.setAsNeeded(formHelper.getIsPRNMed());
        dosageInstructions.setAsNeeded(formHelper.getSelectedPrnReason());
        dosageInstructions.setRoute(formHelper.getSelectedRoute());
        
        if (formHelper.getSelectedFrequency() != null) {
            Timing timing = new Timing();
            TimingRepeatComponent repeat = formHelper.getTimingRepeat();
            timing.setRepeat(repeat);
            dosageInstructions.setTiming(timing);
        }
        
        order.setPatientTarget(patient);
        
        return order;
    }
    
    /**
     * Method marshals a Medication Administration resource to JSON and publishes it against an EPS
     * topic.
     * 
     * @param medAdmin
     * @param epsTopic
     */
    public void publishJsonPayloadToEpsTopic(MedicationAdministration medAdmin, String epsTopic) {
        epsService.publishResourceToTopic(epsTopic, medAdmin, "New Medication Administration",
            "New Medication Administration");
    }
}
