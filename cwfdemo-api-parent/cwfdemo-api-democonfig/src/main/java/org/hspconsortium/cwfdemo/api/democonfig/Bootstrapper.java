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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.carewebframework.api.spring.SpringUtil;
import org.carewebframework.common.MiscUtil;

import org.springframework.core.io.Resource;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hspconsortium.cwf.fhir.common.BaseService;

/**
 * Currently hard coded but in later iterations, bootstrapper should be configured based on a
 * configuration file to support greater flexibility during demos or connectathons.
 */
public class Bootstrapper {
    
    
    private static final Log log = LogFactory.getLog(Bootstrapper.class);
    
    protected static final String CONFIG_PATH = "org/hspconsortium/cwfdemo/api/democonfig/";
    
    /**
     * FHIR service for managing resources.
     */
    private final BaseService fhirService;
    
    private final Map<String, Scenario> scenarios = new TreeMap<>();
    
    /**
     * Initialize with FHIR service and populate demo codes.
     * 
     * @param fhirService The FHIR service.
     */
    public Bootstrapper(BaseService fhirService) {
        this.fhirService = fhirService;
        
        try {
            Resource[] resources = SpringUtil.getAppContext().getResources("classpath:" + CONFIG_PATH + "scenario/*.yaml");
            
            for (Resource scenario : resources) {
                String fn = scenario.getFilename();
                scenarios.put(fn.substring(0, fn.length() - 5), null);
            }
        } catch (Exception e) {
            throw MiscUtil.toUnchecked(e);
        }
    }
    
    public Collection<String> getScenarios() {
        return scenarios.keySet();
    }
    
    public Scenario loadScenario(String key) {
        Scenario scenario = scenarios.get(key);
        
        if (scenario == null) {
            scenarios.put(key, scenario = new Scenario(key, fhirService));
        }
        
        return scenario;
    }
    
    public void deleteScenario(String name) {
        deleteScenario(loadScenario(name));
    }
    
    public void deleteScenario(Scenario scenario) {
        List<IBaseResource> resources = scenario.getResources();
        Collections.reverse(resources);
        
        for (IBaseResource resource : resources) {
            fhirService.deleteResource(resource);
        }
        
        scenarios.put(scenario.getName(), null);
    }
}
