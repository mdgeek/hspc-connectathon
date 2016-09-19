/*-
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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.carewebframework.ui.FrameworkController;
import org.carewebframework.ui.zk.PopupDialog;
import org.carewebframework.ui.zk.PromptDialog;
import org.carewebframework.ui.zk.ZKUtil;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hspconsortium.cwf.fhir.common.BaseService;
import org.hspconsortium.cwf.fhir.common.FhirUtil;
import org.hspconsortium.cwfdemo.api.democonfig.Scenario;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Button;
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
    
    private static class ResourceComparator implements Comparator<IBaseResource> {
        
        private final boolean ascending;
        
        public ResourceComparator(boolean ascending) {
            this.ascending = ascending;
        }
        
        @Override
        public int compare(IBaseResource r1, IBaseResource r2) {
            int cmp = r1.getIdElement().getValue().compareToIgnoreCase(r2.getIdElement().getValue());
            return ascending ? cmp : -cmp;
        }
        
    }
    
    private Listbox lboxResources;
    
    private Textbox txtResource;
    
    private Radiogroup rgrpFormat;
    
    private Button btnDelete;
    
    private Scenario scenario;
    
    private final BaseService fhirService;
    
    private final ListModelList<IBaseResource> model = new ListModelList<>();
    
    private final ResourceComparator ascendingComparator = new ResourceComparator(true);
    
    private final ResourceComparator descendingComparator = new ResourceComparator(false);
    
    /**
     * Display view resources dialog.
     * 
     * @param scenario Scenario whose resources are to be viewed.
     * @return True if the scenario was modified.
     */
    public static boolean show(Scenario scenario) {
        Map<Object, Object> args = new HashMap<>();
        args.put("scenario", scenario);
        Window dlg = PopupDialog.popup("~./org/hspconsortium/cwfdemo/ui/democonfig/viewResources.zul", args, true, true,
            true);
        return dlg.hasAttribute("modified");
    }
    
    public ViewResourcesController(BaseService fhirService) {
        super();
        this.fhirService = fhirService;
    }
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        scenario = (Scenario) arg.get("scenario");
        model.addAll(scenario.getResources());
        model.sort(ascendingComparator, true);
        lboxResources.setItemRenderer(resourceRenderer);
        lboxResources.setModel(model);
        updateCaption();
    }
    
    public void onSelect$lboxResources() {
        displayResource();
    }
    
    public void onCheck$rgrpFormat() {
        displayResource();
    }
    
    public void onClick$btnDelete() {
        IBaseResource resource = getSelectedResource();
        
        if (PromptDialog.confirm("Delete " + FhirUtil.getResourceIdPath(resource, true) + "?", "Delete Resource")) {
            try {
                fhirService.deleteResource(resource);
                model.remove(resource);
                root.setAttribute("modified", true);
                updateCaption();
                displayResource();
            } catch (Exception e) {
                PromptDialog.showError("Error deleting resource:\n\n" + ZKUtil.formatExceptionForDisplay(e));
            }
        }
    }
    
    private void updateCaption() {
        ((Window) root).getCaption()
                .setLabel(scenario.getName() + " (" + model.size() + " resource" + (model.size() == 1 ? ")" : "s)"));
    }
    
    private IBaseResource getSelectedResource() {
        Listitem item = lboxResources.getSelectedItem();
        return item == null ? null : (IBaseResource) item.getValue();
    }
    
    private void displayResource() {
        IBaseResource resource = getSelectedResource();
        
        if (resource == null) {
            txtResource.setValue(null);
            btnDelete.setDisabled(true);
        } else {
            FhirContext ctx = fhirService.getClient().getFhirContext();
            IParser parser = rgrpFormat.getSelectedIndex() == 0 ? ctx.newJsonParser() : ctx.newXmlParser();
            parser.setPrettyPrint(true);
            txtResource.setValue(parser.encodeResourceToString(resource));
            txtResource.setSelectionRange(0, 0);
            btnDelete.setDisabled(false);
        }
    }
    
    public Comparator<IBaseResource> getAscendingComparator() {
        return ascendingComparator;
    }
    
    public Comparator<IBaseResource> getDescendingComparator() {
        return descendingComparator;
    }
}
