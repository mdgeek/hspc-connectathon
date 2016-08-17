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
import java.util.Map;
import java.util.TreeMap;

import org.carewebframework.api.spring.SpringUtil;
import org.carewebframework.common.MiscUtil;
import org.hspconsortium.cwf.fhir.common.BaseService;
import org.springframework.core.io.Resource;

/**
 * Currently hard coded but in later iterations, bootstrapper should be configured based on a
 * configuration file to support greater flexibility during demos or connectathons.
 */
public class Bootstrapper {
    
    /**
     * FHIR service for managing resources.
     */
    private final BaseService fhirService;
    
    private final Map<String, Resource> configs = new TreeMap<>();
    
    /**
     * Initialize with FHIR service and populate demo codes.
     * 
     * @param fhirService The FHIR service.
     * @param scenarioBase Base path for scenario files.
     */
    public Bootstrapper(BaseService fhirService, String scenarioBase) {
        this.fhirService = fhirService;
        
        try {
            Resource[] resources = SpringUtil.getAppContext().getResources(scenarioBase + "/*.yaml");
            
            for (Resource resource : resources) {
                String fn = resource.getFilename();
                configs.put(fn.substring(0, fn.length() - 5), resource);
            }
        } catch (Exception e) {
            throw MiscUtil.toUnchecked(e);
        }
    }
    
    public Collection<String> getScenarios() {
        return configs.keySet();
    }
    
    public Scenario loadScenario(String name) {
        Resource config = configs.get(name);
        
        if (config == null) {
            throw new RuntimeException("Scenario not found:" + name);
        }
        
        try {
            return new Scenario(name, config, fhirService);
        } catch (Exception e) {
            throw MiscUtil.toUnchecked(e);
        }
    }
    
    public int deleteScenario(Scenario scenario) {
        return scenario.destroy();
    }
}
