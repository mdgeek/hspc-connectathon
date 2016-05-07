/*
 * #%L
 * Demo Configuration Plugin
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
package org.hspconsortium.cwfdemo.api.democonfig;

import static org.hspconsortium.cwf.fhir.common.FhirTerminology.IDENT_MRN;
import static org.hspconsortium.cwf.fhir.common.FhirTerminology.SYS_COGMED;
import static org.hspconsortium.cwf.fhir.common.FhirTerminology.SYS_RXNORM;
import static org.hspconsortium.cwf.fhir.common.FhirTerminology.SYS_SNOMED;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.DocumentReference.DocumentReferenceContentComponent;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.MedicationAdministration;
import org.hl7.fhir.dstu3.model.MedicationAdministration.MedicationAdministrationDosageComponent;
import org.hl7.fhir.dstu3.model.MedicationOrder;
import org.hl7.fhir.dstu3.model.MedicationOrder.MedicationOrderDosageInstructionComponent;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hspconsortium.cwf.fhir.common.BaseService;
import org.hspconsortium.cwf.fhir.common.FhirUtil;

/**
 * Currently hard coded but in later iterations, bootstrapper should be configured based on a
 * configuration file to support greater flexibility during demos or connectathons.
 */
public class Bootstrapper {
    
    
    private static final String IMAGE_PATH = "org/hspconsortium/cwfdemo/ui/democonfig/images/";
    
    private static final String NOTE_PATH = "org/hspconsortium/cwfdemo/ui/democonfig/notes/";
    
    private static final String[] STREETS = { "123 Yellowbrick Road", "325 Emory Lane", "1201 Regenstrief Blvd",
            "353 Intermountain Street" };
    
    private static final String[] CITIES = { "Los Angeles,CA,90001", "Indianapolis,IN,46202", "New York,NY,10010",
            "Sanibel,FL,33957" };
    
    private static final Class<?>[] DEMO_RESOURCE_TYPES = { Condition.class, MedicationOrder.class,
            MedicationAdministration.class, DocumentReference.class, Patient.class, Practitioner.class, Location.class };
    
    private static final Log log = LogFactory.getLog(Bootstrapper.class);
    
    /**
     * FHIR service for managing resources.
     */
    BaseService fhirService;
    
    /**
     * Medication index
     */
    private final Map<String, CodeableConcept> medicationList = new HashMap<>();
    
    private final Map<String, CodeableConcept> conditionList = new HashMap<>();
    
    private final Map<Class<? extends DomainResource>, List<? extends DomainResource>> resourceCache = new HashMap<>();
    
    /**
     * Initialize with FHIR service and populate demo codes.
     * 
     * @param fhirService The FHIR service.
     */
    public Bootstrapper(BaseService fhirService) {
        this.fhirService = fhirService;
        populateMedicationCodes();
        populateConditionCodes();
        fetchAll();
    }
    
    // ------------- Internal operations -------------
    
    /**
     * Fetches all demo data from FHIR server.
     * 
     * @return Number of resources fetched.
     */
    @SuppressWarnings("unchecked")
    public int fetchAll() {
        int count = 0;
        
        for (Class<?> clazz : DEMO_RESOURCE_TYPES) {
            count += fetchByType((Class<? extends DomainResource>) clazz).size();
        }
        
        return count;
    }
    
    /**
     * Fetches demo data of specified type into cache.
     * 
     * @param clazz Type of resource.
     * @return The list of fetched resources.
     */
    private <D extends DomainResource> List<D> fetchByType(Class<D> clazz) {
        List<D> list = fhirService.searchResourcesByTag(DemoUtils.DEMO_GROUP_TAG, clazz);
        resourceCache.put(clazz, list);
        return list;
    }
    
    @SuppressWarnings("unchecked")
    private <D extends DomainResource> List<D> getCachedResources(Class<D> clazz) {
        List<D> list = (List<D>) resourceCache.get(clazz);
        
        if (list == null) {
            resourceCache.put(clazz, list = new ArrayList<>());
        }
        
        return list;
    }
    
    @SuppressWarnings("unchecked")
    private <D extends DomainResource> void addToCache(D resource) {
        if (resource != null) {
            getCachedResources((Class<D>) resource.getClass()).add(resource);
        }
    }
    
    @SuppressWarnings("unchecked")
    private <D extends DomainResource> D getOrCreate(D resource) {
        Identifier identifier = DemoUtils.getMainIdentifier(resource);
        List<D> list = (List<D>) getCachedResources(resource.getClass());
        
        for (D cachedResource : list) {
            Identifier identifier2 = DemoUtils.getMainIdentifier(cachedResource);
            
            if (identifier.equalsShallow(identifier2)) {
                return cachedResource;
            }
        }
        
        List<? extends DomainResource> results = fhirService.searchResourcesByIdentifier(identifier,
            (Class<? extends DomainResource>) resource.getClass());
        
        if (!results.isEmpty()) {
            resource = (D) results.get(0);
        } else {
            resource = fhirService.createResource(resource);
        }
        
        addToCache(resource);
        return resource;
    }
    
    /**
     * Deletes demo data of specified type from server and clears the cache.
     * 
     * @param clazz Type of resource
     * @return Count of resources deleted.
     */
    private <D extends DomainResource> int deleteByType(Class<D> clazz) {
        getCachedResources(clazz).clear();
        return fhirService.deleteResourcesByTag(DemoUtils.DEMO_GROUP_TAG, clazz);
    }
    
    // ------------- General operations -------------
    
    public int addAll(Patient patient) {
        // @formatter:off
        return addConditions(patient).size()
                + addDocuments(patient).size()
                + addMedicationOrders(patient).size()
                + addMedicationAdministrations(patient).size();
        // @formatter:on
    }
    
    public int deleteAll(Patient patient) {
        List<DomainResource> list = new ArrayList<>();
        
        list.addAll(fhirService.searchResourcesForPatient(patient, Condition.class));
        list.addAll(fhirService.searchResourcesForPatient(patient, MedicationAdministration.class));
        list.addAll(fhirService.searchResourcesForPatient(patient, MedicationOrder.class));
        list.addAll(fhirService.searchResourcesForPatient(patient, DocumentReference.class));
        fhirService.deleteResources(list);
        return list.size();
    }
    
    /**
     * Deletes all demo data.
     * 
     * @return Number of resources deleted.
     */
    @SuppressWarnings("unchecked")
    public int deleteAll() {
        int count = 0;
        
        for (Class<?> clazz : DEMO_RESOURCE_TYPES) {
            count += deleteByType((Class<? extends DomainResource>) clazz);
        }
        
        return count;
    }
    
    // ------------- Patient-related operations -------------
    
    /**
     * Adds a demo patient. TODO Read from configuration file.
     * 
     * @return
     */
    public List<Patient> addPatients() {
        int idnum = 0;
        getOrCreate(buildPatient(++idnum, "LeMalade,Jacques", 365 * 56, AdministrativeGender.MALE, "male_adult.jpeg"));
        getOrCreate(buildPatient(++idnum, "Intermountain,Jane", 365 * 26, AdministrativeGender.FEMALE, "female_adult.jpeg"));
        getOrCreate(buildPatient(++idnum, "Intermountain,Jose", 1, AdministrativeGender.MALE, "male_newborn.jpeg"));
        return getCachedResources(Patient.class);
    }
    
    /**
     * Deletes all patient sharing the PATIENT_GROUP_IDENTIFIER
     * 
     * @return Count of deleted resources.
     */
    public int deletePatients() {
        return deleteByType(Patient.class);
    }
    
    /**
     * Builds a patient instance but does NOT persist it.
     * 
     * @param idnum
     * @param name
     * @param dobOffset
     * @param gender
     * @param photo
     * @return
     */
    private Patient buildPatient(int idnum, String name, int dobOffset, AdministrativeGender gender, String photo) {
        Patient patient = new Patient();
        patient.addIdentifier(DemoUtils.createIdentifier("patient", idnum).setType(IDENT_MRN));
        DemoUtils.addDemoTag(patient);
        patient.addName(FhirUtil.parseName(name));
        patient.setGender(gender);
        patient.setBirthDate(DemoUtils.createDateWithDayOffset(dobOffset));
        setPatientPhoto(patient, photo);
        setPatientAddress(patient);
        return patient;
    }
    
    /**
     * Convenience method to add a random address to a patient resource
     * 
     * @param patient
     */
    private void setPatientAddress(Patient patient) {
        Address address = new Address();
        address.addLine(DemoUtils.getRandom(STREETS));
        String[] csz = DemoUtils.getRandom(CITIES).split("\\,");
        address.setCity(csz[0]);
        address.setState(csz[1]);
        address.setPostalCode(csz[2]);
        patient.addAddress(address);
    }
    
    /**
     * Convenience method for creating and setting patient photo.
     * 
     * @param patient
     */
    private void setPatientPhoto(Patient patient, String file) {
        try {
            byte[] data = FhirUtil.getResourceAsByteArray(IMAGE_PATH + file);
            Attachment photo = patient.addPhoto().setData(data);
            photo.setContentType("image/jpeg");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // ------------- Medication Administration operations -------------
    
    /**
     * Method populates the patient record with some sample medication administrations.
     * 
     * @param patient
     * @return
     */
    public List<MedicationAdministration> addMedicationAdministrations(Patient patient) {
        addMedicationOrders(patient);
        int idnum = 0;
        List<MedicationAdministration> list = new ArrayList<>();
        
        list.add(getOrCreate(
            buildMedicationAdministration(++idnum, patient, "metoprolol", 1, DemoUtils.createDateWithMinuteOffset(45))));
        list.add(getOrCreate(
            buildMedicationAdministration(++idnum, patient, "clopidogrel", 1, DemoUtils.createDateWithMinuteOffset(35))));
        return list;
    }
    
    /**
     * Clears all demo medication administrations.
     * 
     * @return Count of deleted resources.
     */
    public int deleteMedicationAdministrations() {
        return deleteByType(MedicationAdministration.class);
    }
    
    /**
     * Method builds a medication administration instance.
     * 
     * @param identifier
     * @param medCode
     * @param dose
     * @param effectiveDate
     * @param prescription
     * @return
     */
    private MedicationAdministration buildMedicationAdministration(int idnum, Patient patient, String medCode, int tabCount,
                                                                   Date effectiveDate) {
        MedicationAdministration medAdmin = new MedicationAdministration();
        medAdmin.addIdentifier(DemoUtils.createIdentifier("medadmin", idnum, patient));
        DemoUtils.addDemoTag(medAdmin);
        medAdmin.setPatient(new Reference(patient));
        medAdmin.setMedication(medicationList.get(medCode));
        medAdmin.setEffectiveTime(new DateTimeType(effectiveDate));
        medAdmin.setDosage(createDosage(tabCount));
        medAdmin.setPrescription(new Reference(findMedicationOrder(patient, medCode)));
        return medAdmin;
    }
    
    /**
     * Convenience method for representing n tablets of medication X
     * 
     * @param numberOfTablets
     * @return
     */
    private MedicationAdministrationDosageComponent createDosage(int numberOfTablets) {
        SimpleQuantity simpleQuantity = new SimpleQuantity();
        simpleQuantity.setValue(numberOfTablets);
        simpleQuantity.setUnit("{tbl}");
        MedicationAdministrationDosageComponent dose = new MedicationAdministrationDosageComponent()
                .setQuantity(simpleQuantity);
        return dose;
    }
    
    /**
     * Populates a medication index. TODO Build off a configuration file
     */
    private void populateMedicationCodes() {
        medicationList.clear();
        medicationList.put("metoprolol",
            FhirUtil.createCodeableConcept(SYS_RXNORM, "372891", "Metoprolol Tartrate 25 MG Oral tablet"));
        medicationList.put("atenolol", FhirUtil.createCodeableConcept(SYS_RXNORM, "197379", "Atenolol 100 MG Oral Tablet"));
        medicationList.put("bisoprolol",
            FhirUtil.createCodeableConcept(SYS_RXNORM, "854901", "Bisoprolol Fumarate 10 MG Oral Tablet"));
        medicationList.put("clopidogrel",
            FhirUtil.createCodeableConcept(SYS_RXNORM, "309362", "Clopidogrel 75 MG Oral Tablet"));
        medicationList.put("atorvastatin",
            FhirUtil.createCodeableConcept(SYS_RXNORM, "597967", "Amlodipine 10 MG / Atorvastatin 20 MG Oral Tablet"));
        medicationList.put("acetaminophen",
            FhirUtil.createCodeableConcept(SYS_RXNORM, "665056", "Acetaminophen 500 MG Chewable Tablet"));
        medicationList.put("aspirin", FhirUtil.createCodeableConcept(SYS_RXNORM, "198466", "Aspirin 325 MG Oral Capsule"));
        medicationList.put("hydrochlorothiazide",
            FhirUtil.createCodeableConcept(SYS_RXNORM, "310798", "Hydrochlorothiazide 25 MG Oral Tablet"));
        medicationList.put("bisacodyl", FhirUtil.createCodeableConcept(SYS_RXNORM, "1550933", "Bisacodyl 5 MG Oral Tablet"));
        medicationList.put("acetazolamide",
            FhirUtil.createCodeableConcept(SYS_RXNORM, "197304", "Acetazolamide 250 MG Oral Tablet"));
    }
    
    // ------------- Medication Order operations -------------
    
    /**
     * Method populates the patient record with some sample medication orders.
     * 
     * @param medOrderSetIdentifier
     * @param patient
     * @return
     */
    public List<MedicationOrder> addMedicationOrders(Patient patient) {
        int idnum = 0;
        List<MedicationOrder> list = new ArrayList<>();
        
        list.add(getOrCreate(buildMedicationOrder(++idnum, patient, "metoprolol",
            createDosageInstructions(1, "PO", "QD", null), DemoUtils.createDateWithMinuteOffset(45))));
        list.add(getOrCreate(buildMedicationOrder(++idnum, patient, "clopidogrel",
            createDosageInstructions(1, "PO", "QD", null), DemoUtils.createDateWithMinuteOffset(35))));
        list.add(getOrCreate(buildMedicationOrder(++idnum, patient, "atorvastatin",
            createDosageInstructions(1, "PO", "QD", null), DemoUtils.createDateWithMinuteOffset(25))));
        list.add(getOrCreate(buildMedicationOrder(++idnum, patient, "acetaminophen",
            createDosageInstructions(1, "PO", "Q8H", "1"), DemoUtils.createDateWithMinuteOffset(15))));
        return list;
    }
    
    /**
     * Clears all medication orders that share the given identifier.
     * 
     * @return
     */
    public int deleteMedicationOrders() {
        return deleteByType(MedicationOrder.class);
    }
    
    private MedicationOrder findMedicationOrder(Patient patient, String med) {
        CodeableConcept medCode = medicationList.get(med);
        Reference ref = new Reference(patient);
        
        for (MedicationOrder order : getCachedResources(MedicationOrder.class)) {
            if (order.getPatient().equalsShallow(ref) && medCode.equalsShallow(order.getMedication())) {
                return order;
            }
        }
        
        return null;
    }
    
    /**
     * Method builds a medication order instance.
     * 
     * @param medCode The medication code.
     * @param dose The dosage instruction.
     * @param dateWritten When written.
     * @return The new medication order.
     */
    private MedicationOrder buildMedicationOrder(int idnum, Patient patient, String medCode,
                                                 MedicationOrderDosageInstructionComponent dose, Date dateWritten) {
        MedicationOrder medOrder = new MedicationOrder();
        medOrder.setPatient(new Reference(patient));
        medOrder.addIdentifier(DemoUtils.createIdentifier("medorder", idnum, patient));
        DemoUtils.addDemoTag(medOrder);
        medOrder.setMedication(medicationList.get(medCode));
        medOrder.setDateWritten(dateWritten);
        medOrder.addDosageInstruction(dose);
        return medOrder;
    }
    
    /**
     * Convenience method for representing n tablets of medication X
     * 
     * @param numberOfTablets
     * @param routeCode
     * @param freqCode
     * @param prnCode
     * @return
     */
    private MedicationOrderDosageInstructionComponent createDosageInstructions(int numberOfTablets, String routeCode,
                                                                               String freqCode, String prnCode) {
        SimpleQuantity simpleQuantity = new SimpleQuantity();
        simpleQuantity.setValue(numberOfTablets);
        simpleQuantity.setUnit("{tbl}");
        MedicationOrderDosageInstructionComponent dose = new MedicationOrderDosageInstructionComponent()
                .setDose(simpleQuantity);
        if (routeCode != null && routeCode.equalsIgnoreCase("PO")) {
            CodeableConcept route = FhirUtil.createCodeableConcept(SYS_SNOMED, "26643006", "Oral route");
            dose.setRoute(route);
        }
        if (freqCode != null && freqCode.equalsIgnoreCase("QD")) {
            dose.getTiming().setRepeat(FhirUtil.getRepeatFromFrequencyCode(freqCode));
        } else if (freqCode != null && freqCode.equalsIgnoreCase("Q8H")) {
            dose.getTiming().setRepeat(FhirUtil.getRepeatFromFrequencyCode(freqCode));
        } else {
            log.error("Unknown frequency code " + freqCode);
        }
        if (prnCode != null && prnCode.equalsIgnoreCase("1")) {
            CodeableConcept prnCodeableConcept = FhirUtil.createCodeableConcept(SYS_COGMED, "1", "As needed for pain");
            dose.setAsNeeded(prnCodeableConcept);
        } else if (prnCode != null && prnCode.equalsIgnoreCase("2")) {
            CodeableConcept prnCodeableConcept = FhirUtil.createCodeableConcept(SYS_COGMED, "1",
                "As needed to control hypertension");
            dose.setAsNeeded(prnCodeableConcept);
        }
        return dose;
    }
    
    // ------------- Condition-related operations -------------
    
    /**
     * Method populates the patient record with some sample conditions.
     * 
     * @param patient
     * @return
     */
    public List<Condition> addConditions(Patient patient) {
        int idnum = 0;
        List<Condition> list = new ArrayList<>();
        
        list.add(getOrCreate(buildCondition(++idnum, patient, "HTN", "ACTIVE", "Strong family history HTN.",
            DemoUtils.createDateWithYearOffset(1))));
        list.add(getOrCreate(buildCondition(++idnum, patient, "OSTEO", "ACTIVE", "Patient played linebacker in NFL.",
            DemoUtils.createDateWithYearOffset(3))));
        list.add(getOrCreate(buildCondition(++idnum, patient, "CONCUSSION", "ACTIVE", "Secondary to automobile accident.",
            DemoUtils.createDateWithDayOffset((3)))));
        return list;
    }
    
    /**
     * Deletes all conditions that share the given identifier.
     * 
     * @return
     */
    public int deleteConditions() {
        return deleteByType(Condition.class);
    }
    
    /**
     * Build a condition.
     * 
     * @param identifier
     * @param conditionCode
     * @param status
     * @param notes
     * @param dateRecorded
     * @return
     */
    private Condition buildCondition(int idnum, Patient patient, String conditionCode, String status, String notes,
                                     Date dateRecorded) {
        Condition condition = new Condition();
        condition.setPatient(new Reference(patient));
        condition.addIdentifier(DemoUtils.createIdentifier("condition", idnum, patient));
        DemoUtils.addDemoTag(condition);
        condition.setDateRecorded(dateRecorded);
        condition.setCode(conditionList.get(conditionCode));
        condition.setClinicalStatus(status);
        condition.addNote(new Annotation().setText(notes));
        return condition;
    }
    
    /**
     * Populates a condition index. TODO Build off a configuration file
     */
    private void populateConditionCodes() {
        conditionList.clear();
        conditionList.put("HTN", FhirUtil.createCodeableConcept(SYS_SNOMED, "5962100", "Essential Hypertension"));
        conditionList.put("OSTEO", FhirUtil.createCodeableConcept(SYS_SNOMED, "396275006", "Osteoarthritis"));
        conditionList.put("CONCUSSION", FhirUtil.createCodeableConcept(SYS_SNOMED, "110030002", "Concussive Brain Injury"));
    }
    
    // ------------- Document-related operations -------------
    
    public List<DocumentReference> addDocuments(Patient patient) {
        int idnum = 0;
        List<Practitioner> practitioners = addPractitioners();
        List<DocumentReference> list = new ArrayList<>();
        
        list.add(getOrCreate(buildDocument(++idnum, patient, practitioners.get(0), 234, "Discharge Summary",
            "Discharge Summary", "discharge_summary.txt")));
        list.add(getOrCreate(buildDocument(++idnum, patient, practitioners.get(1), 5, "Progress Report", "Progress Report",
            "progress_report.txt")));
        
        if (patient.getGender() == AdministrativeGender.FEMALE) {
            list.add(getOrCreate(buildDocument(++idnum, patient, practitioners.get(2), 1, "Lactation Assessment",
                "Lactation Assessment", "lactation_assessment.txt")));
        }
        return list;
    }
    
    /**
     * Deletes all demo documents.
     * 
     * @return Count of deleted resources.
     */
    public int deleteDocuments() {
        return deleteByType(DocumentReference.class);
    }
    
    private DocumentReference buildDocument(int idnum, Patient patient, Practitioner author, int createOffset, String type,
                                            String description, String body) {
        DocumentReference doc = new DocumentReference();
        doc.setType(FhirUtil.createCodeableConcept(SYS_COGMED, type, description));
        doc.setSubject(new Reference(patient));
        doc.addIdentifier(DemoUtils.createIdentifier("document", idnum, patient));
        DemoUtils.addDemoTag(doc);
        doc.setCreated(DemoUtils.createDateWithDayOffset(createOffset));
        doc.addAuthor(new Reference(author));
        DocumentReferenceContentComponent content = doc.addContent();
        Attachment attachment = new Attachment();
        attachment.setContentType("text/plain");
        attachment.setData(getNoteData(body));
        content.setAttachment(attachment);
        return doc;
    }
    
    private byte[] getNoteData(String note) {
        try {
            return FhirUtil.getResourceAsByteArray(NOTE_PATH + note);
        } catch (Exception e) {
            return null;
        }
    }
    
    // ------------- Practitioner-related operations -------------
    
    public List<Practitioner> addPractitioners() {
        int idnum = 0;
        List<Practitioner> list = new ArrayList<>();
        
        list.add(getOrCreate(buildPractitioner("Fry,Emory", ++idnum)));
        list.add(getOrCreate(buildPractitioner("Martin,Doug", ++idnum)));
        list.add(getOrCreate(buildPractitioner("Huff,Stan", ++idnum)));
        return list;
    }
    
    /**
     * Deletes all practitioners that share the given identifier.
     * 
     * @return
     */
    public int deletePractitioners() {
        return deleteByType(Practitioner.class);
    }
    
    private Practitioner buildPractitioner(String name, int idnum) {
        Practitioner p = new Practitioner();
        p.addName(FhirUtil.parseName(name));
        p.addIdentifier(DemoUtils.createIdentifier("practitioner", idnum));
        DemoUtils.addDemoTag(p);
        return p;
    }
    
    // ------------- Location-related operations -------------
    
    public List<Location> addLocations() {
        List<Location> list = new ArrayList<>();
        
        list.add(getOrCreate(buildLocation("Delivery Suite", "413964002")));
        list.add(getOrCreate(buildLocation("Recovery Room", "398161000")));
        list.add(getOrCreate(buildLocation("Post Recovery Room", "420280003")));
        return list;
    }
    
    /**
     * Deletes all locations that share the given identifier.
     * 
     * @return
     */
    public int deleteLocations() {
        return deleteByType(Location.class);
    }
    
    private Location buildLocation(String name, String code) {
        Location loc = new Location();
        loc.setName(name);
        Identifier identifier = FhirUtil.createIdentifier("http://snomed.info/sct", code);
        loc.addIdentifier(identifier);
        DemoUtils.addDemoTag(loc);
        return loc;
    }
    
}
