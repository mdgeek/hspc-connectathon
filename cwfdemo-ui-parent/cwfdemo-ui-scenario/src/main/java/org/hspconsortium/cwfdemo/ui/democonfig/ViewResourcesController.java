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
package org.hspconsortium.cwfdemo.ui.democonfig;

import java.util.HashMap;
import java.util.Map;

import org.carewebframework.ui.FrameworkController;
import org.carewebframework.ui.zk.PopupDialog;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hspconsortium.cwf.fhir.common.BaseService;
import org.hspconsortium.cwf.fhir.common.FhirUtil;
import org.hspconsortium.cwfdemo.api.democonfig.Scenario2;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class ViewResourcesController extends FrameworkController {
    
    private static final long serialVersionUID = 1;
    
    private static ListitemRenderer<IBaseResource> resourceRenderer = new ListitemRenderer<IBaseResource>() {
        
        @Override
        public void render(Listitem item, IBaseResource resource, int index) throws Exception {
            item.setLabel(FhirUtil.getResourceIdPath(resource));
            item.setValue(resource);
        }
        
    };
    
    private Listbox lboxResources;
    
    private Textbox txtResource;
    
    private Radiogroup rgrpFormat;
    
    private Scenario2 scenario;
    
    private final BaseService fhirService;
    
    /**
     * Display view resources dialog.
     * 
     * @param scenario Scenario2 whose resources are to be viewed.
     */
    public static void show(Scenario2 scenario) {
        Map<Object, Object> args = new HashMap<>();
        args.put("scenario", scenario);
        PopupDialog.popup("~./org/hspconsortium/cwfdemo/ui/democonfig/viewResources.zul", args, true, true, true);
    }
    
    public ViewResourcesController(BaseService fhirService) {
        super();
        this.fhirService = fhirService;
    }
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        scenario = (Scenario2) arg.get("scenario");
        ((Window) comp).getCaption().setLabel(scenario.getName());
        ListModelList<IBaseResource> model = new ListModelList<>();
        model.addAll(scenario.getResources());
        lboxResources.setItemRenderer(resourceRenderer);
        lboxResources.setModel(model);
    }
    
    public void onSelect$lboxResources() {
        displayResource();
    }
    
    public void onCheck$rgrpFormat() {
        displayResource();
    }
    
    private void displayResource() {
        Listitem item = lboxResources.getSelectedItem();
        
        if (item == null) {
            txtResource.setValue(null);
        } else {
            IBaseResource resource = item.getValue();
            FhirContext ctx = fhirService.getClient().getFhirContext();
            IParser parser = rgrpFormat.getSelectedIndex() == 0 ? ctx.newJsonParser() : ctx.newXmlParser();
            parser.setPrettyPrint(true);
            txtResource.setValue(parser.encodeResourceToString(resource));
            txtResource.setSelectionRange(0, 0);
        }
    }
}
