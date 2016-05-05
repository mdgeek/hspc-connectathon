/*
 * #%L
 * Message Viewer Plugin
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
package org.hspconsortium.cwfdemo.ui.messagebox;

import org.carewebframework.common.MiscUtil;
import org.carewebframework.common.StrUtil;
import org.carewebframework.ui.FrameworkController;
import org.carewebframework.ui.zk.PopupDialog;
import org.carewebframework.ui.zk.PromptDialog;
import org.carewebframework.ui.zk.ZKUtil;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import org.hspconsortium.cwf.api.ucs.MessageWrapper;

/**
 * Controller for individual message display.
 */
public class ViewerController extends FrameworkController {
    
    
    private static final long serialVersionUID = 1L;
    
    private static final String DIALOG = ZKUtil.getResourcePath(ViewerController.class) + "viewer.zul";
    
    protected static class ActionEvent extends Event {
        
        
        private static final long serialVersionUID = 1L;
        
        private final MessageWrapper message;
        
        public ActionEvent(MessageWrapper message, Action action) {
            super("onAction", null, action);
            this.message = message;
        }
        
        public MessageWrapper getMessage() {
            return message;
        }
        
        public Action getAction() {
            return (Action) getData();
        }
    }
    
    public enum Action {
        DELETE, DELETE_ALL, SKIP, SKIP_ALL, CANCEL, VIEW
    };
    
    private MessageWrapper message;
    
    private EventListener<ActionEvent> actionListener;
    
    private String defaultTitle;
    
    private Label lblHeader;
    
    private Button btnDelete;
    
    private Button btnDeleteAll;
    
    private Button btnSkipAll;
    
    private Button btnView;
    
    private Textbox txtMessage;
    
    private Caption caption;
    
    /**
     * Create an amodal instance of the viewer dialog.
     * 
     * @param actionListener Listener to respond to viewer action events.
     * @return The controller associated with the viewer dialog.
     */
    protected static ViewerController create(EventListener<ActionEvent> actionListener) {
        Window dlg = PopupDialog.popup(DIALOG, false, false, false);
        ViewerController infoOnlyController = (ViewerController) FrameworkController.getController(dlg);
        infoOnlyController.actionListener = actionListener;
        return infoOnlyController;
    }
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        defaultTitle = caption.getLabel();
    }
    
    /**
     * Process a single message.
     * 
     * @param message Message to display.
     * @param text Optional message text to display. If null, the message body is displayed.
     */
    public void process(MessageWrapper message, String text) {
        if (message != null) {
            this.message = message;
            lblHeader.setValue(message.getSubject());
            txtMessage.setText(text != null ? text : message.getBody());
            txtMessage.setVisible(!txtMessage.getText().isEmpty());
            btnDelete.setDisabled(!message.canDelete());
            btnDeleteAll.setDisabled(message.isActionable() || btnDelete.isDisabled());
            btnSkipAll.setDisabled(message.isActionable());
            btnView.setDisabled(!message.hasPatient());
            caption.setLabel(message.hasPatient() ? message.getPatientName() : defaultTitle);
            txtMessage.invalidate();
            root.setVisible(true);
        } else {
            onAction(null);
        }
    }
    
    /**
     * Delete the message.
     */
    public void onClick$btnDelete() {
        if (PromptDialog.confirm(StrUtil.formatMessage("@cwfmessagebox.viewer.delete.confirm.prompt", message.getSubject()))) {
            onAction(Action.DELETE);
        }
    }
    
    /**
     * Delete this and all remaining messages.
     */
    public void onClick$btnDeleteAll() {
        if (PromptDialog.confirm(StrUtil.formatMessage("@cwfmessagebox.viewer.delete.all.confirm.prompt"))) {
            onAction(Action.DELETE_ALL);
        }
    }
    
    /**
     * Skip this message.
     */
    public void onClick$btnSkip() {
        onAction(Action.SKIP);
    }
    
    /**
     * Skip this and all remaining messages.
     */
    public void onClick$btnSkipAll() {
        onAction(Action.SKIP_ALL);
    }
    
    /**
     * Cancel message processing.
     */
    public void onClick$btnCancel() {
        onAction(Action.CANCEL);
    }
    
    /**
     * Change context to patient associated with the message.
     */
    public void onClick$btnView() {
        onAction(Action.VIEW);
    }
    
    /**
     * Forward the action to the listener.
     * 
     * @param action Action to forward.
     */
    protected void onAction(Action action) {
        root.setVisible(action == Action.VIEW);
        
        if (action != null && actionListener != null) {
            try {
                actionListener.onEvent(new ActionEvent(message, action));
            } catch (Exception e) {
                throw MiscUtil.toUnchecked(e);
            }
        }
    }
}
