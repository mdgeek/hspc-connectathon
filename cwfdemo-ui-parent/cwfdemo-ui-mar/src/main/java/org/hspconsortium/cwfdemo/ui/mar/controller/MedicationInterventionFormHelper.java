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
package org.hspconsortium.cwfdemo.ui.mar.controller;

import java.math.BigDecimal;

import org.zkoss.zul.Combobox;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hl7.fhir.dstu3.model.Timing.TimingRepeatComponent;
import org.hspconsortium.cwf.api.patient.PatientContext;
import org.hspconsortium.cwf.fhir.common.FhirTerminology;
import org.hspconsortium.cwf.fhir.common.FhirUtil;

public class MedicationInterventionFormHelper {
    
    
    private final SimpleMedicationAdministrationController controller;
    
    private CodeableConcept selectedMedication;
    
    private CodeableConcept selectedUnits;
    
    private CodeableConcept selectedFrequency;
    
    private CodeableConcept selectedRoute;
    
    private CodeableConcept selectedTimeUnit;
    
    private CodeableConcept selectedPrnReason;
    
    private BigDecimal doseAmount;
    
    private BigDecimal durationTime;
    
    private Boolean isPRNMed;
    
    public MedicationInterventionFormHelper(SimpleMedicationAdministrationController controller) {
        this.controller = controller;
    }
    
    public void initialize() {
        
        selectedMedication = getSelectedCode(controller.getMedSelector());
        selectedUnits = getSelectedCode(controller.getDoseUnitSelector());
        selectedRoute = getSelectedCode(controller.getRouteOfAdminSelector());
        selectedFrequency = getSelectedCode(controller.getFrequencySelector());
        selectedTimeUnit = getSelectedCode(controller.getUnitOfTimeSelector());
        selectedPrnReason = getSelectedCode(controller.getPrnReasonSelector());
        
        doseAmount = getDoseQuantityValue();
        durationTime = getDurationTime();
        
        isPRNMed = getIsPrnValue();
    }
    
    /**
     * Returns true if the necessary form state meets the minimum requirements for placing an
     * medication order.
     * 
     * @return
     */
    public boolean meetsOrderRequirements() {
        Patient patient = PatientContext.getActivePatient();
        return (patient != null && selectedMedication != null && selectedUnits != null && doseAmount != null);
    }
    
    /**
     * Returns true if the necessary form state meets the minimum requirements for administering a
     * medication.
     * 
     * @return
     */
    public boolean meetsMedicationAdministrationRequirements() {
        Patient patient = PatientContext.getActivePatient();
        return (patient != null && selectedMedication != null && selectedUnits != null && doseAmount != null);
    }
    
    public CodeableConcept getSelectedMedication() {
        return selectedMedication;
    }
    
    public void setSelectedMedication(CodeableConcept selectedMedication) {
        this.selectedMedication = selectedMedication;
    }
    
    public CodeableConcept getSelectedUnits() {
        return selectedUnits;
    }
    
    public void setSelectedUnits(CodeableConcept selectedUnits) {
        this.selectedUnits = selectedUnits;
    }
    
    public CodeableConcept getSelectedFrequency() {
        return selectedFrequency;
    }
    
    public void setSelectedFrequency(CodeableConcept selectedFrequency) {
        this.selectedFrequency = selectedFrequency;
    }
    
    public CodeableConcept getSelectedRoute() {
        return selectedRoute;
    }
    
    public void setSelectedRoute(CodeableConcept selectedRoute) {
        this.selectedRoute = selectedRoute;
    }
    
    public CodeableConcept getSelectedTimeUnit() {
        return selectedTimeUnit;
    }
    
    public void setSelectedTimeUnit(CodeableConcept selectedTimeUnit) {
        this.selectedTimeUnit = selectedTimeUnit;
    }
    
    public CodeableConcept getSelectedPrnReason() {
        return selectedPrnReason;
    }
    
    public void setSelectedPrnReason(CodeableConcept selectedPrnReason) {
        this.selectedPrnReason = selectedPrnReason;
    }
    
    public BigDecimal getDoseAmount() {
        return doseAmount;
    }
    
    public void setDoseAmount(BigDecimal doseAmount) {
        this.doseAmount = doseAmount;
    }
    
    public BigDecimal getDurationTime() {
        return durationTime;
    }
    
    public void setDurationTime(BigDecimal durationTime) {
        this.durationTime = durationTime;
    }
    
    public Boolean getIsPRNMed() {
        return isPRNMed;
    }
    
    public void setIsPRNMed(Boolean isPRNMed) {
        this.isPRNMed = isPRNMed;
    }
    
    public BigDecimal getDoseQuantityValue() {
        BigDecimal doseQuantity = null;
        if (controller.getDoseQuantity() != null) {
            doseQuantity = controller.getDoseQuantity().getValue();
        }
        return doseQuantity;
    }
    
    public BigDecimal getDurationTimeValue() {
        BigDecimal durationTime = null;
        if (controller.getDuration() != null) {
            durationTime = controller.getDuration().getValue();
        }
        return durationTime;
    }
    
    public Boolean getIsPrnValue() {
        Boolean isPRN = null;
        if (controller.getIsPRN() != null) {
            isPRN = controller.getIsPRN().isChecked();
        }
        return isPRN;
    }
    
    public SimpleQuantity getDoseQuantity() {
        SimpleQuantity simpleQuantity = null;
        if (doseAmount != null && selectedUnits != null) {
            simpleQuantity = new SimpleQuantity();// TODO Support specifying
                                                  // in UI? Leave blank? Ask
                                                  // Emory.
            simpleQuantity.setValue(doseAmount);
            simpleQuantity.setUnit(FhirUtil.getFirst(selectedUnits.getCoding()).getCode());
        }
        return simpleQuantity;
    }
    
    public TimingRepeatComponent getTimingRepeat() {
        TimingRepeatComponent repeat = null;
        if (selectedFrequency != null && selectedFrequency.hasCoding()) {
            repeat = FhirUtil.getRepeatFromFrequencyCode(FhirUtil.getFirst(selectedFrequency.getCoding()).getDisplay());
            if (durationTime != null) {
                repeat.setDuration(durationTime);
                repeat.setDurationUnit(
                    FhirUtil.convertTimeUnitToEnum(FhirUtil.getFirst(selectedTimeUnit.getCoding()).getCode()));
            }
        }
        return repeat;
    }
    
    public CodeableConcept getSelectedCode(Combobox dropdown) {
        CodeableConcept selection = null;
        if (dropdown != null && dropdown.getSelectedItem() != null) {
            String label = dropdown.getSelectedItem().getLabel();
            String value = dropdown.getSelectedItem().getValue();
            selection = FhirUtil.createCodeableConcept(FhirTerminology.SYS_RXNORM, value, label);
        }
        return selection;
    }
    
    // public Repeat convertToTiming(CodeableConcept frequency) {
    // Repeat repeat = new Repeat();
    // if(frequency.getCodingFirstRep().getCode().equals("1")) {
    // repeat.setFrequency(1);
    // repeat.setPeriod(24);
    // repeat.setPeriodUnits(UnitsOfTimeEnum.H);
    // } else if(frequency.getCodingFirstRep().getCode().equals("2")) {
    // repeat.setFrequency(1);
    // repeat.setPeriod(8);
    // repeat.setPeriodUnits(UnitsOfTimeEnum.H);
    // } else {
    // throw new RuntimeException("Unknown timing " +
    // frequency.getCodingFirstRep().getDisplay());
    // }
    // return repeat;
    // }
}
