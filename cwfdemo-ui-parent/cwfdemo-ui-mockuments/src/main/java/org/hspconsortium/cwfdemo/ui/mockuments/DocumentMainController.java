/*
 * #%L
 * cwf-ui-mockuments
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
package org.hspconsortium.cwfdemo.ui.mockuments;

import org.carewebframework.shell.plugins.PluginController;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Include;

/**
 * This is the main controller for the clinical document display component. It doesn't do much other
 * than to control which of the two views (document list vs document display) is visible.
 */
public class DocumentMainController extends PluginController {
    
    
    private static final long serialVersionUID = 1L;
    
    private DocumentListController listController;
    
    private DocumentDisplayController displayController;
    
    private Include documentList;
    
    private Include documentDisplay;
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        listController.setDisplayController(displayController);
        displayController.setListController(listController);
        attachController(documentList, listController);
        attachController(documentDisplay, displayController);
    }
    
    public DocumentDisplayController getDisplayController() {
        return displayController;
    }
    
    public void setDisplayController(DocumentDisplayController displayController) {
        this.displayController = displayController;
    }
    
    public DocumentListController getListController() {
        return listController;
    }
    
    public void setListController(DocumentListController listController) {
        this.listController = listController;
    }
    
}
