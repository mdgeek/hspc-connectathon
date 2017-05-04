/*
 * Copyright 2017 Cognitive Medical Systems, Inc (http://www.cognitivemedicine.com).
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Jeff Chung
 */

package com.cogmedicine.flowsheet.bean;

import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;

import java.util.*;

public class IOGridModel {
    private List<Date> headerNames;
    private List<String> rowNames;
    private List<List<String>> rows;

    private Date userStartTime;
    private Date userEndTime;

    private Bundle bundle;
    private Map<MedicationRequest, List<MedicationAdministration>> medicationRequestMap;

    public IOGridModel(Bundle bundle, Date userStartTime, Date userEndTime){
        headerNames = new LinkedList<Date>();
        rowNames = new LinkedList<String>();
        rows = new ArrayList<List<String>>();
        medicationRequestMap = new HashMap<MedicationRequest, List<MedicationAdministration>>();

        this.bundle = bundle;
        this.userStartTime = userStartTime;
        this.userEndTime = userEndTime;

        processBundle();
    }

    private void processBundle(){
        createMedicationRequestMap();

        Medication medication;
        MedicationRequest medicationRequest;
        List<MedicationAdministration> medicationAdministrations;
        for(Map.Entry<MedicationRequest, List<MedicationAdministration>> entry : medicationRequestMap.entrySet()){
            medicationRequest = entry.getKey();
            medicationAdministrations = entry.getValue();
            medication = getMedication(medicationRequest);

            Timing timing = medicationRequest.getDosageInstruction().get(0).getTiming();
            Date medicationStartDate = getStartDate(timing);
            Date medicationEndDate = getEndDate(timing);
            long frequency = getFrequency(timing);
            long tolerance = getTolerance(medicationRequest);

            //create the medication request row
            rowNames.add(getMedicationNameWithDosage(medication, medicationRequest));
            List<String> row = addRow();

            Set<Date> tempHeaders = getTempHeaders(medicationAdministrations, medicationStartDate, medicationEndDate, frequency);
            createGrid(medicationAdministrations, tempHeaders, row, tolerance);
        }
    }

    private Date getStartDate(Timing timing){
        try {
            return timing.getRepeat().getBoundsPeriod().getStart();
        }catch(FHIRException e){
            throw new IllegalArgumentException(e);
        }
    }

    private Date getEndDate(Timing timing){
        try {
            return timing.getRepeat().getBoundsPeriod().getEnd();
        }catch(FHIRException e){
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Create the data rows for each medication request based on the medication administrations
     * @param medicationAdministrations
     * @param createdHeaders
     * @param row
     * @param tolerance
     */
    private void createGrid(List<MedicationAdministration> medicationAdministrations, Set<Date> tempHeaders, List<String> row, long tolerance){
        List<Date> dates = getDates(medicationAdministrations);
        Collections.sort(dates);
        long lastMedAdminTime = dates.get(dates.size() - 1).getTime();

        for(Date tempHeader : tempHeaders){
            MedicationAdministration medicationAdministration = contains(medicationAdministrations, tempHeader, tolerance);
            if(medicationAdministration != null){
                Date header = getMedicationAdministrationDate(medicationAdministration);
                createHeader(header);

                int index = headerNames.indexOf(header);

                SimpleQuantity simpleQuantity = medicationAdministration.getDosage().getDose();
                row.set(index, simpleQuantity.getValue() + " " + simpleQuantity.getUnit());
            }else{
                createHeader(tempHeader);
                int index = headerNames.indexOf(tempHeader);

                if(tempHeader.getTime() > lastMedAdminTime){
                    row.set(index, "DUE");
                }else{
                    row.set(index, "OVERDUE");
                }
            }
        }
    }

    /**
     * Get the medication administration that matches the date and tolerance
     * @param medicationAdministrations
     * @param createdHeader
     * @param tolerance
     * @return
     */
    private MedicationAdministration contains(List<MedicationAdministration> medicationAdministrations, Date createdHeader, long tolerance){
        long headerTime = createdHeader.getTime();
        long leftTime = headerTime - tolerance;
        long rightTime = headerTime + tolerance;

        Date medAdminDate;
        for(MedicationAdministration medicationAdministration : medicationAdministrations){
            medAdminDate = getMedicationAdministrationDate(medicationAdministration);

            if(leftTime <= medAdminDate.getTime() && medAdminDate.getTime() <= rightTime){
                return medicationAdministration;
            }
        }

        return null;
    }

    private Date getMedicationAdministrationDate(MedicationAdministration medicationAdministration){
        return ((DateTimeType)medicationAdministration.getEffective()).getValue();
    }

    /**
     * Create the column headers for a particular medication request
     * @param startDate
     * @param frequency
     * @return
     */
    private Set<Date> getTempHeaders(List<MedicationAdministration> medicationAdministrations, Date startDate, Date endDate, long frequency){
        Set<Date> setDates = new HashSet<Date>();
        List<Date> dates = getDates(medicationAdministrations);
        Collections.sort(dates);
        Date leftDate = dates.get(0);

        int i = -1;
        long startTime = startDate.getTime();
        while(true){
            i++;
            long time = startTime + (frequency * i);

            if(time > userEndTime.getTime()){
                break;
            }
            if(endDate != null && time > endDate.getTime()){
                break;
            }
            if(userStartTime.getTime() <= time && leftDate.getTime() <= time){
                Date date = new Date(time);
                //createHeader(date);
                setDates.add(date);
            }
        }

        return setDates;
    }

    /**
     * Adds a header to the headerNames List
     * @param date
     */
    private void createHeader(Date date){
        if(!headerNames.contains(date)){
            long startTime = date.getTime();
            int index = -1;

            for(int i = 0; i < headerNames.size(); i++){
                long time = headerNames.get(i).getTime();
                if(startTime < time){
                    index = i;
                    break;
                }
            }

            if(index == -1){
                appendColumn(date);
            }else{
                insertColumn(date, index);
            }
        }
    }

    /**
     * Get all the medication administrations dates for a particular medication request
     * @param medicationAdministrations
     * @param startTime
     * @return
     */
    private List<Date> getDates(List<MedicationAdministration> medicationAdministrations){
        List<Date> dates = new ArrayList<Date>();

        for(MedicationAdministration medicationAdministration : medicationAdministrations){
            Date date = ((DateTimeType)medicationAdministration.getEffective()).getValue();
            if(!dates.contains(date)){
                dates.add(date);
            }
        }

        return dates;
    }

    /**
     * Append a column header
     * @param startDate
     */
    private void appendColumn(Date startDate){
        headerNames.add(startDate);
        for(List<String> row : rows){
            row.add(null);
        }
    }

    /**
     * Insert a column header
     * @param startDate
     * @param index
     */
    private void insertColumn(Date startDate, int index){
        headerNames.add(index, startDate);
        for(List<String> row : rows){
            row.add(index, null);
        }
    }

    /**
     * Makes a call to determine the tolerance of dosages such as must be within 15 minutes of the schedule time
     * Currently hard coded to 15 minutes.  Plan to make a call to another service
     * @param medicationRequest
     * @return
     */
    private long getTolerance(MedicationRequest medicationRequest){
        return 1000 * 60 * 15; // 15 minutes
    }

    /**
     * Returns the time in milliseconds between dosages
     * Currently only implemented for times per day
     * @param timing
     * @return
     */
    private long getFrequency(Timing timing){
        Timing.TimingRepeatComponent repeat = timing.getRepeat();
        Timing.UnitsOfTime unitOfTime = repeat.getPeriodUnit();

        switch(unitOfTime){
            case D: return 1000 * 60 * 60 * (24 / repeat.getFrequency());
            default: throw new RuntimeException("Frequency of " + unitOfTime + " not implemented yet");
        }
    }

    /**
     * Gets the medication name with the dosage instructions
     * @param medication
     * @param medicationRequest
     * @return
     */
    private String getMedicationNameWithDosage(Medication medication, MedicationRequest medicationRequest){
        SimpleQuantity quantityObj = (SimpleQuantity)medicationRequest.getDosageInstruction().get(0).getDose();
        String display = medication.getCode().getCoding().get(0).getDisplay();
        String quantity = quantityObj.getValue() + " " + quantityObj.getUnit();

        return display + " (" + quantity + ")";
    }

    private Medication getMedication(MedicationRequest medicationRequest){
        try{
            return (Medication)medicationRequest.getMedicationReference().getResource();
        }catch(FHIRException e){
            throw new RuntimeException("miss Medication");
        }
    }

    private List<String> addRow(){
        List<String> columns = new LinkedList<String>();
        for(int i = 0; i < headerNames.size(); i++){
            columns.add(null);
        }

        rows.add(columns);

        return columns;
    }

    /**
     * Adds all MedicationAdministrations to a map of MedicationRequests
     */
    private void createMedicationRequestMap(){
        Resource resource;
        for(Bundle.BundleEntryComponent entry : bundle.getEntry()){
            resource = entry.getResource();
            if(resource instanceof MedicationAdministration){
                MedicationAdministration medicationAdministration = (MedicationAdministration)resource;
                addToMedicationRequestMap(medicationAdministration);
            }
        }
    }

    /**
     * Addes MedicationAdministrations to a keyed MedicationRequest
     * @param medicationAdministration
     */
    private void addToMedicationRequestMap(MedicationAdministration medicationAdministration){
        MedicationRequest medicationRequest = (MedicationRequest)medicationAdministration.getPrescription().getResource();
        List<MedicationAdministration> medicationAdministrations;

        if(medicationRequestMap.containsKey(medicationRequest)){
            medicationAdministrations = medicationRequestMap.get(medicationRequest);
        }else{
            medicationAdministrations = new ArrayList<MedicationAdministration>();
            medicationRequestMap.put(medicationRequest, medicationAdministrations);
        }

        medicationAdministrations.add(medicationAdministration);
    }

    public List<Date> getHeaderNames() {
        return headerNames;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public List<String> getRowNames() {
        return rowNames;
    }
}
