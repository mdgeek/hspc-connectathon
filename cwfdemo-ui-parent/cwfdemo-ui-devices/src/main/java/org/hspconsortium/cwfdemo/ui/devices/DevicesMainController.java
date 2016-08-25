/*
 * #%L
 * Devices Plugin
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
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hspconsortium.cwfdemo.ui.devices;

import ca.uhn.fhir.context.FhirContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.carewebframework.ui.FrameworkController;
//import org.carewebframework.ui.highcharts.Chart;
//import org.carewebframework.ui.highcharts.Series;
import org.carewebframework.ui.thread.ZKThread;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Device;
import org.hl7.fhir.dstu3.model.DeviceComponent;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.InstantType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hspconsortium.cwf.fhir.common.BaseService;
import org.hspconsortium.cwfdemo.api.eps.EPSService;
import org.socraticgrid.hl7.services.eps.model.Message;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.GenericEventListener;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.ListModelList;

/**
 *
 * @author esteban
 */
public class DevicesMainController extends FrameworkController {
    
    private static final String THREAD_KEY_DEVICES = "devices";
    
    private static final String EPS_OBSERVATIONS_TOPIC = "Patient";
    
    private FhirContext fhirContext;
    private BaseService fhirService;
    private EPSService epsService;
    
    private Combobox cboDevice;
    private Combobox cboComponent;
    
//    private Chart chart;

    public DevicesMainController() {
//        chart = new Chart();
        System.out.println("\n\n\tCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC");
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp); 
        
        System.out.println("\n\n\tAAAAAAAAAAAAAAAAAAAAAAAAA");
        
        startBackgroundThread(new ZKThread.ZKRunnable() {
            @Override
            public void run(ZKThread thread) throws Exception {
                System.out.println("\n\n\tBBBBBBBBBBBBBBBBBBBBBBB");
                thread.setAttribute(THREAD_KEY_DEVICES, fetchDevices());
            }

            @Override
            public void abort() {
            }
        });
        
        startBackgroundThread(new ZKThread.ZKRunnable() {
            @Override
            public void run(ZKThread thread) throws Exception {
                epsService.subscribe(EPS_OBSERVATIONS_TOPIC, new EPSService.IEventCallback() {
                    @Override
                    public void onEvent(Message event) {
                        IBaseResource resource = fhirContext.newJsonParser().parseResource(event.getMessageBodies().get(0).getBody());
                        if(resource instanceof Observation){
                            plotObservation((Observation)resource);
                        }
                    }
                });
            }

            @Override
            public void abort() {
            }
        });
        
//        chart.getXAxis().gridLineWidth = 0;
//        chart.getYAxis().gridLineWidth = 0;
//        chartData();
        
    }
    
    public void onCreate$cboDevice(Event e){
        cboDevice.addEventListener("onInitRenderLater", new GenericEventListener<Event>() {
            @Override
            public void onEvent(Event evt) throws Exception {
                if(cboDevice.getItemCount() > 0){
                    cboDevice.setSelectedIndex(0);
                    //setSelectedIndex() doesn't fire the onSelect event, so
                    //we have to manually call the method bellow.
                    onSelect$cboDevice(new SelectEvent<Combobox, Device>("onSelect", cboDevice, null));
                }
            }
        });
    }
    
    public void onSelect$cboDevice(SelectEvent<Combobox, Device> e){
        List<DeviceComponent> components = fetchDeviceComponents((Device) ((Combobox)e.getTarget()).getSelectedItem().getValue());
        if (components != null){
            populateComponentsDropdown(components);
        }
    }
    
    public void onSelect$cboComponent(SelectEvent<Combobox, DeviceComponent> e){
    }

    @Override
    protected void threadFinished(ZKThread thread) {
        List<Device> devices = (List<Device>) thread.getAttribute(THREAD_KEY_DEVICES);
        if (devices != null){
            populateDevicesDropdown(devices);
        }
    }
    
    private void plotObservation(Observation o){
        
    }
    
//    private void chartData() {
//        chart.clear();
//        
//        Series series = chart.addSeries();
//        series.addDataPoint(10, 5);
//        series.addDataPoint(20, 10);
//        series.addDataPoint(30, 9);
//        
//        chart.run();
//    }
    
    /**
     * Fetch all the devices from the FHIR service
     * @return 
     */
    private List<Device> fetchDevices(){
        List<Device> devices = new ArrayList<>();
        
        //TODO: use fhirService to fetch Devices

        /////Mock
        Device d1 = new Device(new CodeableConcept().addCoding(new Coding("Some System", "Some Code", "Device 1")));
        d1.setId("Device 1");
        d1.setManufacturer("M1");
        d1.setVersion("V1");
        d1.setUserData("seed", 1);
        
        Device d2 = new Device(new CodeableConcept().addCoding(new Coding("Some System", "Some Code", "Device 2")));
        d2.setId("Device 2");
        d2.setManufacturer("M1");
        d2.setVersion("V2");
        d2.setUserData("seed", 2);
        
        devices.add(d1);
        devices.add(d2);
        ////
        
        return devices;
    }
    
    /**
     * Fetch all the DeviceComponents for a Device from the FHIR service
     * @return 
     */
    private List<DeviceComponent> fetchDeviceComponents(Device device){
        
        List<DeviceComponent> components = new ArrayList<>();
        
        //TODO: use fhirService to fetch DeviceComponents
        Random random = new Random((long)device.getUserInt("seed"));
        DeviceComponent c1 = new DeviceComponent(
            new CodeableConcept().addCoding(new Coding("Some System", "Some Code", "Component "+random.nextInt(99))), 
            new Identifier(),
            new InstantType(new Date())
        );
        
        DeviceComponent c2 = new DeviceComponent(
            new CodeableConcept().addCoding(new Coding("Some System", "Some Code", "Component "+random.nextInt(99))), 
            new Identifier(),
            new InstantType(new Date())
        );
        
        components.add(c1);
        components.add(c2);
        ///////////////////////
        
        
        return components;
    }
    
    private void populateDevicesDropdown(List<Device> devices){
        ListModelList<Device> model = new ListModelList<>(devices);
        if(!devices.isEmpty()){
            model.setSelection(Arrays.asList(devices.get(0)));
        }
        cboDevice.setModel(model);
    }
    
    private void populateComponentsDropdown(List<DeviceComponent> components){
        ListModelList<DeviceComponent> model = new ListModelList<>(components);
        if(!components.isEmpty()){
            model.setSelection(Arrays.asList(components.get(0)));
        }
        cboComponent.setModel(model);
    }

    public FhirContext getFhirContext() {
        return fhirContext;
    }

    public void setFhirContext(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    public EPSService getEpsService() {
        return epsService;
    }

    public void setEpsService(EPSService epsService) {
        this.epsService = epsService;
    }
    
    public BaseService getFhirService() {
        return fhirService;
    }
    
    public void setFhirService(BaseService fhirService) {
        this.fhirService = fhirService;
    }
    
}
