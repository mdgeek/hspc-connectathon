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

import ca.uhn.fhir.parser.IParser;

public class Scenario {
    
    
    private static final String CONFIG_PATH = Bootstrapper.CONFIG_PATH;
    
    private final Map<String, IBaseResource> resources = new LinkedHashMap<>();
    
    private final Map<String, Map<String, String>> config;
    
    private final IParser jsonParser;
    
    private final String name;
    
    @SuppressWarnings("unchecked")
    public Scenario(String name, BaseService fhirService) {
        this.name = name;
        this.jsonParser = fhirService.getClient().getFhirContext().newJsonParser();
        
        config = (Map<String, Map<String, String>>) new Yaml().load(getResourceAsStream("scenario/" + name + ".yaml"));
        
        for (String key : config.keySet()) {
            Map<String, String> map = config.get(key);
            String source = map.get("source");
            
            if (source == null) {
                throw new RuntimeException("No source specified in scenario.");
            }
            
            IBaseResource resource = parseResource(source, map);
            resource = fhirService.createOrUpdateResource(resource);
            resources.put(key, resource);
        }
    }
    
    public String getName() {
        return name;
    }
    
    public List<IBaseResource> getResources() {
        return new ArrayList<>(resources.values());
    }
    
    private InputStream getResourceAsStream(String path) {
        return Scenario.class.getClassLoader().getResourceAsStream(CONFIG_PATH + path);
    }
    
    private IBaseResource parseResource(String source, Map<String, String> map) {
        source = source.contains(".") ? source : source + ".json";
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
        return resource;
    }
    
    private String eval(String exp) {
        int i = exp.indexOf('/');
        
        if (i == -1) {
            IBaseResource resource = resources.get(exp);
            
            if (resource == null) {
                throw new RuntimeException("Resource not defined: " + exp);
            }
            
            return resource.getIdElement().getValue();
        }
        
        String type = exp.substring(0, i);
        String value = exp.substring(i + 1);
        
        if ("date".equals(type)) {
            return doDate(value);
        }
        
        if ("image".equals(type)) {
            return doBinary(exp);
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
    
    private String doDate(String value) {
        String s = value.toLowerCase().trim();
        char first = s.charAt(0);
        
        if (first == 't' || first == 'n') {
            Date date = DateUtil.parseDate(s);
            
            if (date != null) {
                BaseDateTimeType dtt = first == 't' ? new DateTimeType(date) : new DateType(date);
                value = dtt.getValueAsString();
            }
        }
        
        return value;
    }
}
