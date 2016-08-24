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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carewebframework.common.DateUtil;
import org.carewebframework.common.MiscUtil;
import org.hl7.fhir.dstu3.model.BaseDateTimeType;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hspconsortium.cwf.fhir.common.BaseService;
import org.hspconsortium.cwf.fhir.common.FhirUtil;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;

import ca.uhn.fhir.parser.IParser;

public class Scenario {
    
    private static final Log log = LogFactory.getLog(Scenario.class);
    
    private final Map<String, Map<String, String>> scenarioConfig;
    
    private final String scenarioName;
    
    private final Resource scenarioBase;
    
    private final IBaseCoding scenarioTag;
    
    private final Map<String, IBaseResource> scenarioResources = new HashMap<>();
    
    private final BaseService fhirService;
    
    private boolean isLoaded;
    
    @SuppressWarnings("unchecked")
    public Scenario(Resource scenarioYaml, BaseService fhirService) {
        this.scenarioName = FilenameUtils.getBaseName(scenarioYaml.getFilename());
        this.scenarioBase = scenarioYaml;
        this.fhirService = fhirService;
        this.scenarioTag = ScenarioUtil.createScenarioTag(scenarioName);
        
        try (InputStream in = scenarioYaml.getInputStream()) {
            scenarioConfig = (Map<String, Map<String, String>>) new Yaml().load(in);
        } catch (Exception e) {
            log.error("Failed to load scenario: " + scenarioName, e);
            throw MiscUtil.toUnchecked(e);
        }
        
        log.info("Loaded demo scenario: " + scenarioName);
    }
    
    /**
     * Return the name of this scenario.
     * 
     * @return The scenario name.
     */
    public String getName() {
        return scenarioName;
    }
    
    /**
     * Return the tag used to mark a resource as belonging to this scenario.
     * 
     * @return The scenario tag.
     */
    public IBaseCoding getTag() {
        return scenarioTag;
    }
    
    /**
     * Adds the general demo tag and a scenario tag to the resource.
     * 
     * @param resource Resource to be tagged.
     */
    public void addTags(IBaseResource resource) {
        ScenarioUtil.addDemoTag(resource);
        FhirUtil.addTag(scenarioTag, resource);
    }
    
    /**
     * Returns a read-only list of loaded resources.
     * 
     * @return List of loaded resources.
     */
    public Collection<IBaseResource> getResources() {
        return Collections.unmodifiableCollection(scenarioResources.values());
    }
    
    /**
     * Returns a count of loaded resources.
     * 
     * @return Count of loaded resources.
     */
    public int getResourceCount() {
        return scenarioResources.size();
    }
    
    public boolean isLoaded() {
        return isLoaded;
    }
    
    /**
     * Initialize the scenario. Any existing resources belonging to the scenario will first be
     * deleted. Then creates all resources as defined in the scenario configuration.
     * 
     * @return Count of resources in scenario.
     */
    public synchronized int initialize() {
        destroy();
        IParser jsonParser = fhirService.getClient().getFhirContext().newJsonParser();
        Map<String, IBaseResource> resourceMap = new HashMap<>();
        
        for (String name : scenarioConfig.keySet()) {
            Map<String, String> map = scenarioConfig.get(name);
            String source = map.get("source");
            
            if (source == null) {
                throw new RuntimeException("No source specified in scenario.");
            }
            
            IBaseResource resource = parseResource(source, map, jsonParser, resourceMap);
            resource = createOrUpdateResource(resource);
            resourceMap.put(name, resource);
            logAction(resource, "Created");
        }
        
        return scenarioResources.size();
    }
    
    /**
     * Load all resources for this scenario.
     * 
     * @return Count of resources loaded for this scenario.
     */
    @SuppressWarnings("unchecked")
    public synchronized int load() {
        isLoaded = true;
        scenarioResources.clear();
        
        for (Class<? extends IBaseResource> clazz : ScenarioUtil.getResourceClasses()) {
            for (IBaseResource resource : fhirService.searchResourcesByTag(scenarioTag, (Class<IBaseResource>) clazz)) {
                addResource(resource);
                logAction(resource, "Retrieved");
            }
        }
        
        return scenarioResources.size();
    }
    
    /**
     * Destroy all resources belonging to this scenario.
     * 
     * @return The number of resources successfully deleted.
     */
    public synchronized int destroy() {
        load();
        int count = 0;
        boolean deleted = true;
        
        while (deleted) {
            deleted = false;
            Iterator<IBaseResource> iterator = scenarioResources.values().iterator();
            
            while (iterator.hasNext()) {
                IBaseResource resource = iterator.next();
                
                try {
                    fhirService.deleteResource(resource);
                    iterator.remove();
                    deleted = true;
                    count++;
                    logAction(resource, "Deleted");
                } catch (Exception e) {}
            }
        }
        
        for (IBaseResource resource : scenarioResources.values()) {
            logAction(resource, "Failed to delete");
        }
        
        return count;
    }
    
    /**
     * Creates or updates the specified resource, first tagging it as belonging to this scenario.
     * 
     * @param resource The resource to create or update.
     * @return The resource, possibly modified.
     */
    public IBaseResource createOrUpdateResource(IBaseResource resource) {
        addTags(resource);
        resource = fhirService.createOrUpdateResource(resource);
        addResource(resource);
        return resource;
    }
    
    /**
     * Adds a resource to the list of resources for this scenario.
     * 
     * @param resource Scenario to add.
     */
    public synchronized void addResource(IBaseResource resource) {
        scenarioResources.put(resource.getIdElement().getValue(), resource);
    }
    
    private InputStream getResourceAsStream(String name) {
        try {
            return scenarioBase.createRelative(name).getInputStream();
        } catch (Exception e) {
            throw MiscUtil.toUnchecked(e);
        }
    }
    
    private void logAction(IBaseResource resource, String operation) {
        FhirUtil.stripVersion(resource);
        log.info(operation + " resource: " + resource.getIdElement().getValue());
    }
    
    private IBaseResource parseResource(String source, Map<String, String> map, IParser jsonParser,
                                        Map<String, IBaseResource> resourceMap) {
        source = addExtension(source, "json");
        StringBuilder sb = new StringBuilder();
        
        try (InputStream is = getResourceAsStream(source);) {
            List<String> json = IOUtils.readLines(is, "UTF-8");
            
            for (String s : json) {
                int p1;
                
                while ((p1 = s.indexOf("${")) > -1) {
                    int p2 = s.indexOf("}", p1);
                    String key = s.substring(p1 + 2, p2);
                    String value = map.get(key);
                    
                    if (value == null) {
                        throw new RuntimeException("Reference not found: " + key);
                    }
                    
                    String r = eval(value, resourceMap);
                    s = s.substring(0, p1) + r + s.substring(p2 + 1);
                }
                
                sb.append(s).append('\n');
            }
        } catch (Exception e) {
            MiscUtil.toUnchecked(e);
        }
        
        return jsonParser.parseResource(sb.toString());
    }
    
    /**
     * Add default extension if one is not present.
     * 
     * @param source File resource path.
     * @param dflt The default extension.
     * @return File resource path with extension.
     */
    private String addExtension(String source, String dflt) {
        return source.contains(".") ? source : source + "." + dflt;
    }
    
    /**
     * Evaluate an expression.
     * 
     * @param exp The expression. The general format is
     *            <p>
     *            <code>type/value</code>
     *            </p>
     *            If <code>type</code> is omitted, it is assumed to be a placeholder for a resource
     *            previously defined. Possible values for <code>type</code> are:
     *            <ul>
     *            <li>value - A literal value; inserted as is</li>
     *            <li>date - A date value; can be a relative date (T+n, for example)</li>
     *            <li>image - A file containing an image</li>
     *            <li>snippet - A file containing a snippet to be inserted</li>
     *            </ul>
     * @param resourceMap Map of resolved resources.
     * @return The result of the evaluation.
     */
    private String eval(String exp, Map<String, IBaseResource> resourceMap) {
        int i = exp.indexOf('/');
        
        if (i == -1) {
            IBaseResource resource = resourceMap.get(exp);
            
            if (resource == null) {
                throw new RuntimeException("Resource not defined: " + exp);
            }
            
            return resource.getIdElement().getResourceType() + "/" + resource.getIdElement().getIdPart();
        }
        
        String type = exp.substring(0, i);
        String value = exp.substring(i + 1);
        
        if ("value".equals(type)) {
            return value;
        }
        
        if ("date".equals(type)) {
            return doDate(value);
        }
        
        if ("image".equals(type)) {
            return doBinary(exp);
        }
        
        if ("snippet".equals(type)) {
            return doSnippet(exp);
        }
        
        throw new RuntimeException("Unknown type: " + type);
    }
    
    private String doBinary(String value) {
        try (InputStream is = getResourceAsStream(value)) {
            return Base64.encodeBase64String(IOUtils.toByteArray(is));
        } catch (Exception e) {
            throw MiscUtil.toUnchecked(e);
        }
    }
    
    private String doSnippet(String value) {
        value = addExtension(value, "json");
        
        try (InputStream is = getResourceAsStream(value)) {
            return IOUtils.toString(is);
        } catch (Exception e) {
            throw MiscUtil.toUnchecked(e);
        }
    }
    
    private String doDate(String value) {
        boolean dateOnly = value.toLowerCase().trim().charAt(0) == 't';
        Date date = DateUtil.parseDate(value);
        
        if (date != null) {
            BaseDateTimeType dtt = dateOnly ? new DateType(date) : new DateTimeType(date);
            value = dtt.getValueAsString();
        } else {
            throw new RuntimeException("Bad date specification: " + value);
        }
        
        return value;
    }
    
}