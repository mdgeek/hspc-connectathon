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
import org.hspconsortium.cwfdemo.api.democonfig.Scenario;
import org.hspconsortium.cwfdemo.api.democonfig.ScenarioDefinition;
import org.hspconsortium.cwfdemo.api.democonfig.ScenarioRegistry;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.ComboitemRenderer;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;

/**
 * This controller is only intended to be used for demo purposes in order to stage and unstage data.
 * At this time, it is fairly simple in its function. At a later time, it can be enhanced as needed
 * for demo or connectathon use cases.
 */
public class DemoConfigController extends PluginController {
    
    private static final long serialVersionUID = 1L;
    
    private static final ComboitemRenderer<Scenario> scenarioRenderer = new ComboitemRenderer<Scenario>() {
        
        @Override
        public void render(Comboitem item, Scenario scenario, int index) throws Exception {
            item.setLabel(scenario.getName());
            item.setValue(scenario);
        }
        
    };
    
    private Combobox cboScenarios;
    
    private Button btnDelete;
    
    private Button btnInit;
    
    private Label lblMessage;
    
    private final ScenarioRegistry scenarioRegistry;
    
    private final ListModelList<Scenario> model = new ListModelList<>();
    
    /**
     * Demonstration Configuration Helper Class.
     */
    public static void show() {
        PopupDialog.popup("~./org/hspconsortium/cwfdemo/ui/democonfig/demoConfigWin.zul", true, true, true);
    }
    
    public DemoConfigController(ScenarioRegistry scenarioRegistry) {
        super();
        this.scenarioRegistry = scenarioRegistry;
    }
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        cboScenarios.setItemRenderer(scenarioRenderer);
        refreshScenarios();
    }
    
    private void refreshScenarios() {
        cboScenarios.setModel(null);
        model.clear();
        
        for (ScenarioDefinition def : scenarioRegistry.getAll()) {
            model.add(def.createScenario());
        }
        
        cboScenarios.setModel(model);
    }
    
    public void onSelect$cboScenarios() {
        boolean disabled = getSelectedScenario() == null;
        btnDelete.setDisabled(disabled);
        btnInit.setDisabled(disabled);
        
        if (disabled) {
            setMessage(null);
        } else {
            loadScenario();
        }
    }
    
    private void loadScenario() {
        Scenario scenario = getSelectedScenario();
        
        if (scenario != null) {
            try {
                scenario.load();
                setMessage(
                    "Scenario " + scenario.getName() + " contains " + scenario.getResources().size() + " resource(s)");
            } catch (Exception e) {
                setMessage(ZKUtil.formatExceptionForDisplay(e));
            }
        }
    }
    
    public void onClick$btnInit() {
        Scenario scenario = getSelectedScenario();
        
        if (scenario != null) {
            try {
                scenario.init();
                setMessage("Created " + scenario.getResources().size() + " resources");
            } catch (Exception e) {
                setMessage(ZKUtil.formatExceptionForDisplay(e));
            }
        }
    }
    
    public void onClick$btnDelete() {
        Scenario scenario = getSelectedScenario();
        
        if (scenario != null) {
            int count = scenario.destroy();
            setMessage("Deleted " + count + " resources");
        }
    }
    
    private Scenario getSelectedScenario() {
        Comboitem item = cboScenarios.getSelectedItem();
        Scenario scenario = null;
        
        if (item != null) {
            scenario = item.getValue();
            scenario.load();
        }
        
        return scenario;
    }
    
    private void setMessage(String msg) {
        lblMessage.setValue(msg);
    }
}
