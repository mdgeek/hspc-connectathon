/*
 * #%L
 * Medication Administration Record
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
package org.hspconsortium.cwfdemo.ui.mar.model;

import java.util.ArrayList;
import java.util.List;

/**
 * This model is an MVVM model and is not used for now. Please do not yet remove.
 * 
 * @author cnanjo
 */
public class MarViewModel {
    
    
    private String title = "Medication AdministrationRecord";
    
    private List<String> headers;
    
    private List<List<String>> rows;
    
    public MarViewModel() {
        headers = new ArrayList<String>();
        rows = new ArrayList<List<String>>();
        //initializeMar();
    }
    
    public List<List<String>> getRows() {
        System.out.println("LOADED");
        return rows;
    }
    
    public void setRows(List<List<String>> rows) {
        this.rows = rows;
    }
    
    public List<String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
}
