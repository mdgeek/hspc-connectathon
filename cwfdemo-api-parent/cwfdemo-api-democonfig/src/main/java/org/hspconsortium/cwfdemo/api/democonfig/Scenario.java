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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import org.carewebframework.common.DateUtil;
import org.carewebframework.common.MiscUtil;

import org.hl7.fhir.dstu3.model.BaseDateTimeType;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hspconsortium.cwf.fhir.common.BaseService;
import org.yaml.snakeyaml.Yaml;

import ca.uhn.fhir.model.api.Tag;
import ca.uhn.fhir.parser.IParser;

public class Scenario {
    
    
    private static final String CONFIG_PATH = Bootstrapper.CONFIG_PATH;
    
    private final Map<String, IBaseResource> resources = new LinkedHashMap<>();
    
    private final Map<String, Map<String, String>> config;
    
    private final String name;
    
    private final BaseService fhirService;
    
    private boolean initialized;
    
    @SuppressWarnings("unchecked")
    public Scenario(String name, BaseService fhirService) {
        this.name = name;
        this.fhirService = fhirService;
        config = (Map<String, Map<String, String>>) new Yaml().load(getResourceAsStream("scenario/" + name + ".yaml"));
    }
    
    public String getName() {
        return name;
    }
    
    public List<IBaseResource> getResources() {
        return new ArrayList<>(resources.values());
    }
    
    public Scenario init() {
        if (initialized) {
            destroy();
        }
        
        IParser jsonParser = fhirService.getClient().getFhirContext().newJsonParser();
        
        for (String name : config.keySet()) {
            Map<String, String> map = config.get(name);
            String source = map.get("source");
            
            if (source == null) {
                throw new RuntimeException("No source specified in scenario.");
            }
            
            IBaseResource resource = parseResource(source, map, jsonParser);
            resource = fhirService.createOrUpdateResource(resource);
            resources.put(name, resource);
        }
        
        initialized = true;
        return this;
    }
    
    public Scenario load() {
        if (initialized) {
            return this;
        }
        
        List<IBaseResource> existing = fhirService.searchResourcesByTag(DemoUtils.createScenarioTag(name, null));
        
        for (IBaseResource resource : existing) {
            Tag tag = DemoUtils.getScenarioTag(resource, name);
            
            if (tag != null && tag.getLabel() != null) {
                resources.put(tag.getLabel(), resource);
            }
        }
        
        initialized = true;
        return this;
    }
    
    public int destroy() {
        Tag tag = DemoUtils.createScenarioTag(name, null);
        int count = fhirService.deleteResourcesByTag(tag);
        resources.clear();
        initialized = false;
        return count;
    }
    
    private InputStream getResourceAsStream(String path) {
        return Scenario.class.getClassLoader().getResourceAsStream(CONFIG_PATH + path);
    }
    
    private IBaseResource parseResource(String source, Map<String, String> map, IParser jsonParser) {
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
                    
                    String r = eval(value);
                    s = s.substring(0, p1) + r + s.substring(p2 + 1);
                }
                
                sb.append(s).append('\n');
            }
        } catch (Exception e) {
            MiscUtil.toUnchecked(e);
        }
        
        IBaseResource resource = jsonParser.parseResource(sb.toString());
        DemoUtils.addDemoTag(resource);
        DemoUtils.addDemoTag(resource, name, source);
        return resource;
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
     * @return The result of the evaluation.
     */
    private String eval(String exp) {
        int i = exp.indexOf('/');
        
        if (i == -1) {
            return doReference(exp);
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
    
    private String doReference(String value) {
        IBaseResource resource = resources.get(value);
        
        if (resource == null) {
            throw new RuntimeException("Resource not defined: " + value);
        }
        
        return resource.getIdElement().getValue();
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
