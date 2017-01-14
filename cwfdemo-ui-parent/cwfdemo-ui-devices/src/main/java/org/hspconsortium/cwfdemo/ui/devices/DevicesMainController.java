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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.carewebframework.shell.plugins.PluginContainer;
import org.carewebframework.shell.plugins.PluginController;
import org.carewebframework.ui.highcharts.Chart;
import org.carewebframework.ui.highcharts.Series;
import org.carewebframework.ui.thread.ZKThread;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Device;
import org.hl7.fhir.dstu3.model.DeviceComponent;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.InstantType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hspconsortium.cwf.api.security.BasicAuthConfigurator;
import org.hspconsortium.cwf.api.security.BasicAuthInterceptor;
import org.hspconsortium.cwf.fhir.client.FhirContext;
import org.hspconsortium.cwf.fhir.common.BaseService;
import org.hspconsortium.cwf.fhir.common.FhirConfigurator;
import org.hspconsortium.cwf.fhir.common.FhirUtil;
import org.hspconsortium.cwfdemo.api.eps.EPSService;
import org.socraticgrid.hl7.services.eps.model.Message;
import org.springframework.util.StringUtils;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.GenericEventListener;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;

import ca.uhn.fhir.context.FhirVersionEnum;

/**
 * @author esteban
 */
public class DevicesMainController extends PluginController {
    
    private static final String THREAD_KEY_DEVICES = "devices";
    
    private static final String EPS_OBSERVATIONS_TOPIC = "Patient";
    
    private int cfgPlotThreshold;
    
    private String cfgEPSEndpoint;
    
    private String cfgFHIREndpoint;
    
    private String cfgFHIRUsername;
    
    private String cfgFHIRPassword;
    
    private FhirConfigurator fhirConfig;
    
    private FhirContext fhirContext;
    
    private BaseService fhirService;
    
    private EPSService epsService;
    
    private Combobox cboDevice;
    
    private Combobox cboComponent;
    
    private Textbox newObservationCodeTxt;
    
    private Textbox newObservationTxt;
    
    private Datebox newObservationDate;
    
    private Timebox newObservationTime;
    
    private Chart chart;
    
    private Map<String, Series> series = new HashMap<>();
    
    public DevicesMainController() {
    }
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        
        initChart();
    }
    
    @Override
    public void onActivate() {
        super.onActivate();
        
        startBackgroundThread(new ZKThread.ZKRunnable() {
            
            @Override
            public void run(ZKThread thread) throws Exception {
                thread.setAttribute(THREAD_KEY_DEVICES, fetchDevices());
            }
            
            @Override
            public void abort() {
            }
        });
        
        startBackgroundThread(new ZKThread.ZKRunnable() {
            
            @Override
            public void run(ZKThread thread) throws Exception {
                if (epsService != null) {
                    epsService.subscribe(EPS_OBSERVATIONS_TOPIC, new EPSService.IEventCallback() {
                        
                        @Override
                        public void onEvent(Message event) {
                            IBaseResource resource = fhirContext.newJsonParser()
                                    .parseResource(event.getMessageBodies().get(0).getBody());
                            
                            //We are only interested in resource of type Observation
                            if (resource instanceof Observation) {
                                final Observation observation = (Observation) resource;
                                //And not any Observation, only those belonging
                                //to the Device that is currently selected.
                                if (cboDevice.getSelectedItem() == null) {
                                    return;
                                }
                                Device device = (Device) cboDevice.getSelectedItem().getValue();
                                
                                String selectedDeviceId = FhirUtil.getIdAsString(device, true);
                                String observationDeviceId = observation.getDevice().getReference();
                                
                                if (selectedDeviceId.endsWith(observationDeviceId)
                                        || observationDeviceId.endsWith(selectedDeviceId)) {
                                    Executions.schedule(desktop, new EventListener() {
                                        
                                        @Override
                                        public void onEvent(Event event) throws Exception {
                                            plotObservation(observation);
                                        }
                                    }, null);
                                }
                            }
                        }
                    });
                }
            }
            
            @Override
            public void abort() {
            }
        });
    }
    
    @Override
    public void onLoad(PluginContainer container) {
        super.onLoad(container);
        container.registerProperties(this, "cfgPlotThreshold", "cfgEPSEndpoint", "cfgFHIREndpoint", "cfgFHIRUsername",
            "cfgFHIRPassword");
        
        configureServices();
    }
    
    public void onCreate$cboDevice(Event e) {
        cboDevice.addEventListener("onInitRenderLater", new GenericEventListener<Event>() {
            
            @Override
            public void onEvent(Event evt) throws Exception {
                if (cboDevice.getItemCount() > 0) {
                    cboDevice.setSelectedIndex(0);
                    //setSelectedIndex() doesn't fire the onSelect event, so
                    //we have to manually call the method bellow.
                    onSelect$cboDevice(new SelectEvent<Combobox, Device>("onSelect", cboDevice, null));
                }
            }
        });
    }
    
    public void onSelect$cboDevice(SelectEvent<Combobox, Device> e) {
        List<DeviceComponent> components = fetchDeviceComponents(
            (Device) ((Combobox) e.getTarget()).getSelectedItem().getValue());
        if (components != null) {
            populateComponentsDropdown(components);
        }
        
        //Clear Chart
        initChart();
    }
    
    public void onSelect$cboComponent(SelectEvent<Combobox, DeviceComponent> e) {
        chart.setTitle(((DeviceComponent) ((Combobox) e.getTarget()).getSelectedItem().getValue()).getType()
                .getCodingFirstRep().getDisplay());
    }
    
    public void onClick$btnSendObservation(Event e) {
        Calendar c1 = new GregorianCalendar();
        c1.setTime(newObservationDate.getValue());
        Calendar c2 = new GregorianCalendar();
        c2.setTime(newObservationTime.getValue());
        
        c1.set(Calendar.HOUR_OF_DAY, c2.get(Calendar.HOUR_OF_DAY));
        c1.set(Calendar.MINUTE, c2.get(Calendar.MINUTE));
        c1.set(Calendar.SECOND, c2.get(Calendar.SECOND));
        
        Observation o = new Observation();
        o.setEffective(new DateTimeType(c1.getTime()));
        o.setValue(new Quantity(Double.parseDouble(newObservationTxt.getValue())).setUnit("bpm"));
        
        Device device = (Device) cboDevice.getSelectedItem().getValue();
        o.setDevice(new Reference(device));
        
        o.setCode(new CodeableConcept().addCoding(
            new Coding("urn:iso:std:iso:11073:10101", newObservationCodeTxt.getValue(), newObservationCodeTxt.getValue())));
        
        //Publishes Observation to EPS to test the whole pub/sub workflow.
        epsService.publishResourceToTopic(EPS_OBSERVATIONS_TOPIC, o);
    }
    
    @Override
    protected void threadFinished(ZKThread thread) {
        List<Device> devices = (List<Device>) thread.getAttribute(THREAD_KEY_DEVICES);
        if (devices != null) {
            populateDevicesDropdown(devices);
        }
    }
    
    /**
     * Overrides the default services (fhir/eps/etc.) if the required configuration options are
     * provided.
     */
    private void configureServices() {
        
        //If a fhir endpoint is specified, then fhirContext, fhirService and fhirConfig
        //properties get overriten.
        if (!StringUtils.isEmpty(cfgFHIREndpoint)) {
            
            if (!StringUtils.isEmpty(cfgFHIRUsername) && !StringUtils.isEmpty(cfgFHIRPassword)) {
                org.hspconsortium.cwf.fhir.client.FhirContext.registerAuthInterceptor("basic",
                    new BasicAuthInterceptor("basic", new BasicAuthConfigurator() {
                        
                        @Override
                        public String getUsername() {
                            return cfgFHIRUsername;
                        }
                        
                        @Override
                        public String getPassword() {
                            return cfgFHIRPassword;
                        }
                        
                    }));
                
                fhirConfig = new FhirConfigurator() {
                    
                    @Override
                    public String getAuthenticationType() {
                        return "basic";
                    }
                    
                    @Override
                    public FhirVersionEnum getVersion() {
                        return FhirVersionEnum.DSTU3;
                    }
                    
                    @Override
                    public String getRootUrl() {
                        return cfgFHIREndpoint;
                    }
                    
                };
                
                fhirContext = new FhirContext(fhirConfig);
            }
            
            fhirService = new BaseService(fhirContext.newRestfulGenericClient(fhirConfig));
        }
        
        //if cfgEPSEndpoint is specified, epsService is overriten
        if (!StringUtils.isEmpty(cfgEPSEndpoint)) {
            epsService = new EPSService(fhirContext, cfgEPSEndpoint);
            epsService.init();
        }
    }
    
    private void plotObservation(Observation o) {
        try {
            
            String code = o.getCode().getCodingFirstRep().getCode();
            String display = o.getCode().getCodingFirstRep().getDisplay();
            
            Series s = getSeriesOrCreate(code, display == null || display.isEmpty() ? code : display);
            
            //Remove any necessary point in order to keep the max threshold
            if (s.data.size() >= cfgPlotThreshold) {
                int pointsToRemove = cfgPlotThreshold - s.data.size();
                for (int i = 0; i <= pointsToRemove; i++) {
                    s.data.remove(0);
                }
            }
            
            s.addDataPoint(o.getEffectiveDateTimeType().getValue().getTime(), o.getValueQuantity().getValue().doubleValue());
            chart.run();
        } catch (FHIRException e) {
            throw new RuntimeException(e);
        }
    }
    
    private Series getSeriesOrCreate(String name, String label) {
        Series s = this.series.get(name);
        if (s == null) {
            s = chart.addSeries();
            s.name = label;
            s.plotOptions.animation = false;
            this.series.put(name, s);
        }
        
        return s;
    }
    
    private void initChart() {
        chart.clear();
        
        chart.getXAxis().gridLineWidth = 1;
        chart.getXAxis().title.text = "Time";
        chart.getXAxis().type = "datetime";
        
        chart.getYAxis().gridLineWidth = 1;
        chart.getYAxis().title.text = "Value";
        
        series = new HashMap<>();
        
        chart.run();
    }
    
    /**
     * Fetch all the devices from the FHIR service
     *
     * @return
     */
    private List<Device> fetchDevices() {
        return fhirService.searchResourcesByType(Device.class);
    }
    
    /**
     * Fetch all the DeviceComponents for a Device from the FHIR service
     *
     * @return
     */
    private List<DeviceComponent> fetchDeviceComponents(Device device) {
        
        List<DeviceComponent> components = new ArrayList<>();
        
        //TODO: use fhirService to fetch DeviceComponents
        Random random = new Random(device.getUserInt("seed"));
        DeviceComponent c1 = new DeviceComponent(
                new CodeableConcept().addCoding(new Coding("Some System", "Some Code", "Component " + random.nextInt(99))),
                new Identifier(), new InstantType(new Date()));
        
        DeviceComponent c2 = new DeviceComponent(
                new CodeableConcept().addCoding(new Coding("Some System", "Some Code", "Component " + random.nextInt(99))),
                new Identifier(), new InstantType(new Date()));
        
        components.add(c1);
        components.add(c2);
        ///////////////////////
        
        return components;
    }
    
    private void populateDevicesDropdown(List<Device> devices) {
        ListModelList<Device> model = new ListModelList<>(devices);
        if (!devices.isEmpty()) {
            model.setSelection(Arrays.asList(devices.get(0)));
        }
        cboDevice.setModel(model);
    }
    
    private void populateComponentsDropdown(List<DeviceComponent> components) {
        ListModelList<DeviceComponent> model = new ListModelList<>(components);
        if (!components.isEmpty()) {
            model.setSelection(Arrays.asList(components.get(0)));
        }
        cboComponent.setModel(model);
    }
    
    public FhirConfigurator getFhirConfig() {
        return fhirConfig;
    }
    
    public void setFhirConfig(FhirConfigurator fhirConfig) {
        this.fhirConfig = fhirConfig;
    }
    
    public ca.uhn.fhir.context.FhirContext getFhirContext() {
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
    
    public int getCfgPlotThreshold() {
        return cfgPlotThreshold;
    }
    
    public void setCfgPlotThreshold(int cfgPlotThreshold) {
        this.cfgPlotThreshold = cfgPlotThreshold;
    }
    
    public String getCfgEPSEndpoint() {
        return cfgEPSEndpoint;
    }
    
    public void setCfgEPSEndpoint(String cfgEPSEndpoint) {
        this.cfgEPSEndpoint = cfgEPSEndpoint;
    }
    
    public String getCfgFHIREndpoint() {
        return cfgFHIREndpoint;
    }
    
    public void setCfgFHIREndpoint(String cfgFHIREndpoint) {
        this.cfgFHIREndpoint = cfgFHIREndpoint;
    }
    
    public String getCfgFHIRUsername() {
        return cfgFHIRUsername;
    }
    
    public void setCfgFHIRUsername(String cfgFHIRUsername) {
        this.cfgFHIRUsername = cfgFHIRUsername;
    }
    
    public String getCfgFHIRPassword() {
        return cfgFHIRPassword;
    }
    
    public void setCfgFHIRPassword(String cfgFHIRPassword) {
        this.cfgFHIRPassword = cfgFHIRPassword;
    }
    
}
