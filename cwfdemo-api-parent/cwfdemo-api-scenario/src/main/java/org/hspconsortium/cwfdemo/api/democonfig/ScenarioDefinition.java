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

import java.io.InputStream;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carewebframework.common.MiscUtil;
import org.hspconsortium.cwf.fhir.common.BaseService;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;

public class ScenarioDefinition {
    
    private static final Log log = LogFactory.getLog(ScenarioDefinition.class);
    
    private final Map<String, Map<String, String>> scenarioConfig;
    
    private final String scenarioName;
    
    private final Resource scenarioBase;
    
    private final BaseService fhirService;
    
    @SuppressWarnings("unchecked")
    public ScenarioDefinition(String scenarioName, Resource scenarioBase, BaseService fhirService) {
        this.scenarioName = scenarioName;
        this.scenarioBase = scenarioBase;
        this.fhirService = fhirService;
        
        try (InputStream in = scenarioBase.createRelative(scenarioName + ".yaml").getInputStream()) {
            scenarioConfig = (Map<String, Map<String, String>>) new Yaml().load(in);
        } catch (Exception e) {
            log.error("Failed to load scenario: " + scenarioName, e);
            throw MiscUtil.toUnchecked(e);
        }
        
        log.info("Loaded demo scenario: " + scenarioName);
    }
    
    public String getName() {
        return scenarioName;
    }
    
    public Resource getBase() {
        return scenarioBase;
    }
    
    public Map<String, Map<String, String>> getConfig() {
        return scenarioConfig;
    }
    
    public BaseService getFhirService() {
        return fhirService;
    }
    
    public Scenario createScenario() {
        return new Scenario(this);
    }
}
