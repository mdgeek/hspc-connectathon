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
/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 * 
 * This Source Code Form is also subject to the terms of the Health-Related Additional
 * Disclaimer of Warranty and Limitation of Liability available at
 * http://www.carewebframework.org/licensing/disclaimer.
 */
package org.hspconsortium.cwfdemo.ui.democonfig;

import org.carewebframework.shell.plugins.PluginController;
import org.carewebframework.ui.zk.PopupDialog;

import org.zkoss.zul.Label;

import org.hl7.fhir.dstu3.model.Patient;
import org.hspconsortium.cwfdemo.api.democonfig.Bootstrapper;

/**
 * This controller is only intended to be used for demo purposes in order to stage and unstage data.
 * At this time, it is fairly simple in its function. At a later time, it can be enhanced as needed
 * for demo or connectathon use cases.
 */
public class ConnectathonController extends PluginController {
    
    
    private static final long serialVersionUID = 1L;
    
    private static final String ADDED = "%s resources: %d";
    
    private static final String DELETED = "%s deleted: %d";
    
    private static final String NOPATIENT = "A patient must be selected to perform that operation.";
    
    private static final String ADDALL = "Demo resources: %2$d";
    
    private static final String DELETEALL = "Resources deleted: %2$d";
    
    private final Bootstrapper bootstrapper;
    
    private Patient mother;
    
    private Patient baby;
    
    private Label lblInfo;
    
    /**
     * Demonstration Configuration Helper Class.
     */
    public static void show() {
        PopupDialog.popup("~./org/hspconsortium/cwfdemo/ui/democonfig/connectathon.zul", true, true, true);
    }
    
    public ConnectathonController(Bootstrapper bootstrapper) {
        super();
        this.bootstrapper = bootstrapper;
    }
    
    private void showMessage(String msg) {
        lblInfo.setValue(msg);
    }
    
    /*************************************************************************
     * Event Listeners
     *************************************************************************/
    
    public void onClick$btnCreateMother() {
        if (mother == null) {
            
        }
    }
    
    public void onClick$btnDeleteMother() {
        if (mother != null) {
            bootstrapper.deleteAll(mother);
            mother = null;
        }
    }
    
    public void onClick$btnCreateBaby() {
        if (baby == null) {
            
        }
    }
    
    public void onClick$btnDeleteBaby() {
        if (baby != null) {
            bootstrapper.deleteAll(baby);
            baby = null;
        }
    }
    
    public void onClick$btnDeleteAll() {
        onClick$btnDeleteMother();
        onClick$btnDeleteBaby();
    }
    
}
