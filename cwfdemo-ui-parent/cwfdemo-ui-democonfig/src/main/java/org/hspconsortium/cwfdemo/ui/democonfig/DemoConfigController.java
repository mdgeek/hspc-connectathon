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
import org.carewebframework.ui.zk.ZKUtil;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;

import org.hspconsortium.cwfdemo.api.democonfig.Bootstrapper;
import org.hspconsortium.cwfdemo.api.democonfig.Scenario;

/**
 * This controller is only intended to be used for demo purposes in order to stage and unstage data.
 * At this time, it is fairly simple in its function. At a later time, it can be enhanced as needed
 * for demo or connectathon use cases.
 */
public class DemoConfigController extends PluginController {
    
    
    private static final long serialVersionUID = 1L;
    
    private Combobox cboScenarios;
    
    private Button btnDelete;
    
    private Button btnLoad;
    
    private Label lblMessage;
    
    private final Bootstrapper bootstrapper;
    
    /**
     * Demonstration Configuration Helper Class.
     */
    public static void show() {
        PopupDialog.popup("~./org/hspconsortium/cwfdemo/ui/democonfig/demoConfigWin.zul", true, true, true);
    }
    
    public DemoConfigController(Bootstrapper bootstrapper) {
        super();
        this.bootstrapper = bootstrapper;
    }
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        cboScenarios.setModel(new ListModelList<String>(bootstrapper.getScenarios()));
    }
    
    public void onSelect$cboScenarios() {
        boolean disabled = getSelectedScenario() == null;
        btnDelete.setDisabled(disabled);
        btnLoad.setDisabled(disabled);
        setMessage(null);
    }
    
    public void onClick$btnLoad() {
        Scenario scenario = loadSelectedScenario();
        
        if (scenario != null) {
            setMessage("Loaded " + scenario.getResources().size() + " resources");
        }
    }
    
    public void onClick$btnDelete() {
        Scenario scenario = loadSelectedScenario();
        
        if (scenario != null) {
            bootstrapper.deleteScenario(scenario);
            setMessage("Deleted " + scenario.getResources().size() + " resources");
        }
    }
    
    private Scenario loadSelectedScenario() {
        Scenario scenario = null;
        
        try {
            scenario = bootstrapper.loadScenario(getSelectedScenario());
        } catch (Exception e) {
            setMessage(ZKUtil.formatExceptionForDisplay(e));
        }
        
        return scenario;
    }
    
    private String getSelectedScenario() {
        Comboitem item = cboScenarios.getSelectedItem();
        return item == null ? null : item.getLabel();
    }
    
    private void setMessage(String msg) {
        lblMessage.setValue(msg);
    }
}
