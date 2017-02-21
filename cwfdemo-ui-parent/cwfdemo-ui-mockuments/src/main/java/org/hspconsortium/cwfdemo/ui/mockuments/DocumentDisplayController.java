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

import org.carewebframework.ui.FrameworkController;
import org.carewebframework.ui.dialog.PromptDialog;
import org.carewebframework.web.ancillary.MimeContent;
import org.carewebframework.web.annotation.WiredComponent;
import org.carewebframework.web.component.BaseComponent;
import org.carewebframework.web.component.Button;
import org.carewebframework.web.component.Div;
import org.carewebframework.web.component.Html;
import org.carewebframework.web.component.Iframe;
import org.carewebframework.web.component.Import;
import org.carewebframework.web.component.Label;
import org.hspconsortium.cwf.fhir.document.Document;
import org.hspconsortium.cwf.fhir.document.DocumentContent;
import org.hspconsortium.cwf.ui.reporting.Util;

/**
 * Controller for displaying the contents of selected documents.
 */
public class DocumentDisplayController extends FrameworkController {
    
    public interface IDocumentOperation {
        
        boolean hasChanged();
        
        void saveChanges();
        
        void cancelChanges();
    }
    
    public enum DocumentAction {
        SAVE("Save Changes"), DISCARD("Discard Changes"), CANCEL("Keep Editing"), DELETED("?");

        private final String displayValue;

        DocumentAction(String displayValue) {
            this.displayValue = displayValue;
        }

        @Override
        public String toString() {
            return displayValue;
        }
    }
    
    public static final String QUESTIONNAIRE_CONTENT_TYPE = "application/xcwf-";
    
    private static final DocumentAction[] ACTION_OPTIONS = { DocumentAction.SAVE, DocumentAction.DISCARD,
            DocumentAction.CANCEL };
    
    @WiredComponent
    private BaseComponent toolbar;
    
    @WiredComponent
    private Button btnPrint;
    
    @WiredComponent
    private Div printRoot;
    
    private Document document;
    
    private IDocumentOperation documentOperation;
    
    private DocumentListController listController;
    
    /**
     * Sets the document to be displayed.
     *
     * @param document The document to be displayed.
     * @param action The default action to take if document has been modified (if null, prompt for
     *            action).
     */
    protected void setDocument(Document document, DocumentAction action) {
        if (documentOperation != null && documentOperation.hasChanged()) {
            if (action != null) {
                if (doAction(action)) {
                    updateDocument(document);
                }

                return;
            }
            
            PromptDialog.show("What would you like to do?", "Pending Changes", null, ACTION_OPTIONS, null, null, null,
                (response) -> {
                    if (doAction(response.getResponse())) {
                        updateDocument(document);
                    }
                });
        } else {
            updateDocument(document);
        }
    }
    
    private boolean doAction(DocumentAction action) {
        switch (action) {
            case SAVE: // Save
                documentOperation.saveChanges();
                break;
            
            case DISCARD: // Discard
                documentOperation.cancelChanges();
                break;
            
            case CANCEL: // Cancel
                return false;
            
            case DELETED:
                refreshListController();
                break;
        }
        
        return true;
    }
    
    private void updateDocument(Document document) {
        documentOperation = null;
        this.document = document;
        
        if (printRoot != null) {
            printRoot.destroyChildren();
            toolbar.destroyChildren();
            btnPrint.setDisabled(document == null);
        }
        
        if (document != null) {
            for (DocumentContent content : document.getContent()) {
                String ctype = content.getContentType();
                
                if (ctype.startsWith(QUESTIONNAIRE_CONTENT_TYPE)) {
                    String cwf = "web/org/hspconsortium/cwfdemo/ui/mockuments/"
                            + ctype.substring(QUESTIONNAIRE_CONTENT_TYPE.length()) + ".cwf"; // Hack - need to register these somehow
                    Import include = new Import();
                    include.setAttribute("document", document);
                    include.setAttribute("displayController", this);
                    include.setSrc(cwf);
                    printRoot.addChild(include);
                } else if (ctype.equals("text/html")) {
                    Html html = new Html();
                    html.setContent(content.toString());
                    printRoot.addChild(html);
                } else if (ctype.equals("text/plain")) {
                    Label lbl = new Label(content.toString());
                    printRoot.addChild(lbl);
                } else {
                    MimeContent mc = new MimeContent(content.getContentType(), content.getData());
                    Iframe frame = new Iframe();
                    frame.setContent(mc);
                    printRoot.addChild(frame);
                }
            }
            
            if (printRoot.getFirstChild() == null) {
                printRoot.addChild(new Label("Document has no content."));
            }
        }
    }
    
    public void setListController(DocumentListController listController) {
        this.listController = listController;
    }
    
    public void onClick$btnPrint() {
        Util.print(printRoot, document.getTitle(), "patient", null, false);
    }
    
    protected void addToToolbar(BaseComponent source) {
        moveComponents(source, toolbar);
    }
    
    protected void removeFromToolbar(BaseComponent target) {
        moveComponents(toolbar, target);
    }
    
    protected void setDocumentOperation(IDocumentOperation documentOperation) {
        this.documentOperation = documentOperation;
    }
    
    protected void refreshListController() {
        listController.refresh();
    }
    
    private void moveComponents(BaseComponent source, BaseComponent target) {
        BaseComponent child;
        
        while ((child = source.getFirstChild()) != null) {
            child.setParent(target);
        }
    }
}
