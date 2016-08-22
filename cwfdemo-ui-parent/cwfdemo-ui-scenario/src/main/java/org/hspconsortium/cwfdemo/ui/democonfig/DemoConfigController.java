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

import java.util.Collections;
import java.util.Comparator;

import org.carewebframework.shell.plugins.PluginController;
import org.carewebframework.ui.zk.PopupDialog;
import org.carewebframework.ui.zk.PromptDialog;
import org.carewebframework.ui.zk.ZKUtil;
import org.hspconsortium.cwfdemo.api.democonfig.Scenario;
import org.hspconsortium.cwfdemo.api.democonfig.ScenarioRegistry;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.ComboitemRenderer;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;

/**
 * This controller is only intended to be used for demo purposes in order to stage and unstage data.
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
    
    private static final Comparator<Scenario> scenarioComparator = new Comparator<Scenario>() {
        
        @Override
        public int compare(Scenario s1, Scenario s2) {
            return s1.getName().compareToIgnoreCase(s2.getName());
        }
        
    };
    
    private enum Action {
        LOAD("Loading scenario"), RELOAD("Reloading scenario"), RESET("Resetting scenario"), DELETE(
                "Deleting scenario"), DELETEALL("Deleting resources across all scenarios");
        
        private String label;
        
        Action(String label) {
            this.label = label;
        }
        
        @Override
        public String toString() {
            return label;
        }
    };
    
    private Combobox cboScenarios;
    
    private Button btnDelete;
    
    private Button btnReset;
    
    private Button btnReload;
    
    private Button btnView;
    
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
        cboScenarios.setSelectedItem(null);
        model.clear();
        model.addAll(scenarioRegistry.getAll());
        Collections.sort(model, scenarioComparator);
        cboScenarios.setModel(model);
    }
    
    public void onSelect$cboScenarios() {
        boolean disabled = getSelectedScenario() == null;
        btnDelete.setDisabled(disabled);
        btnReset.setDisabled(disabled);
        btnReload.setDisabled(disabled);
        btnView.setDisabled(disabled);
        
        if (disabled) {
            setMessage(null);
        } else {
            doAction(Action.LOAD);
        }
    }
    
    public void onClick$btnReset() {
        doAction(Action.RESET);
    }
    
    public void onClick$btnReload() {
        doAction(Action.RELOAD);
    }
    
    public void onClick$btnDelete() {
        doAction(Action.DELETE);
    }
    
    public void onClick$btnDeleteAll() {
        if (PromptDialog.confirm("Delete all demo scenario resources?", "Delete All")) {
            doAction(Action.DELETEALL);
        }
    }
    
    public void onClick$btnView() {
        ViewResourcesController.show(getSelectedScenario());
    }
    
    /**
     * Queues an action to be performed on the next execution.
     * 
     * @param action Action to be performed.
     */
    private void doAction(Action action) {
        Event event = new Event("onAction", root, action);
        setMessage(null);
        Clients.showBusy(root, action + "...");
        Events.echoEvent(event);
    }
    
    /**
     * Invokes the action specified in the event data.
     * 
     * @param event The event containing the action to invoke.
     */
    public void onAction(Event event) {
        Clients.clearBusy(root);
        Scenario scenario = getSelectedScenario();
        Action action = (Action) event.getData();
        String result = null;
        
        if (action == Action.DELETEALL || scenario != null) {
            try {
                
                switch (action) {
                    case LOAD:
                        if (scenario.isLoaded()) {
                            result = scenario.getResourceCount() + " resource(s) associated with scenario "
                                    + scenario.getName();
                            break;
                        }
                        
                        // Fall through intended here.
                        
                    case RELOAD:
                        result = scenario.load() + " resource(s) loaded for scenario " + scenario.getName();
                        break;
                    
                    case RESET:
                        result = "Created " + scenario.initialize() + " resource(s)";
                        break;
                    
                    case DELETE:
                        result = "Deleted " + scenario.destroy() + " resource(s)";
                        break;
                    
                    case DELETEALL:
                        int count = 0;
                        
                        for (Scenario ascenario : model) {
                            count += ascenario.destroy();
                        }
                        
                        result = "Deleted " + count + " resource(s) across " + model.size() + " scenario(s)";
                }
            } catch (Exception e) {
                result = ZKUtil.formatExceptionForDisplay(e);
            }
        }
        
        setMessage(result);
    }
    
    /**
     * Returns the currently selected scenario, or null if none.
     * 
     * @return The currently selected scenario.
     */
    private Scenario getSelectedScenario() {
        Comboitem item = cboScenarios.getSelectedItem();
        return item == null ? null : (Scenario) item.getValue();
    }
    
    /**
     * Displays the specified message;
     * 
     * @param msg Message to display.
     */
    private void setMessage(String msg) {
        lblMessage.setValue(msg);
    }
}
