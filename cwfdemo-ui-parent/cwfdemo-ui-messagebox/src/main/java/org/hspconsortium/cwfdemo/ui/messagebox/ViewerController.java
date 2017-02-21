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
import org.carewebframework.ui.dialog.DialogUtil;
import org.carewebframework.ui.util.CWFUtil;
import org.carewebframework.web.annotation.EventHandler;
import org.carewebframework.web.annotation.WiredComponent;
import org.carewebframework.web.component.BaseComponent;
import org.carewebframework.web.component.Button;
import org.carewebframework.web.component.Label;
import org.carewebframework.web.component.Memobox;
import org.carewebframework.web.component.Window;
import org.carewebframework.web.event.Event;
import org.carewebframework.web.event.IEventListener;
import org.hspconsortium.cwfdemo.api.ucs.MessageWrapper;

/**
 * Controller for individual message display.
 */
public class ViewerController extends FrameworkController {
    
    private static final long serialVersionUID = 1L;

    private static final String DIALOG = CWFUtil.getResourcePath(ViewerController.class) + "viewer.cwf";

    protected static class ActionEvent extends Event {

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

    private IEventListener actionListener;

    private String defaultTitle;

    private Window window;

    @WiredComponent
    private Label lblHeader;

    @WiredComponent
    private Button btnDelete;

    @WiredComponent
    private Button btnDeleteAll;

    @WiredComponent
    private Button btnSkipAll;

    @WiredComponent
    private Button btnView;

    @WiredComponent
    private Memobox txtMessage;

    /**
     * Create an amodal instance of the viewer dialog.
     *
     * @param actionListener Listener to respond to viewer action events.
     * @return The controller associated with the viewer dialog.
     */
    protected static ViewerController create(IEventListener actionListener) {
        Window dlg = DialogUtil.popup(DIALOG, false, false, false);
        ViewerController infoOnlyController = (ViewerController) FrameworkController.getController(dlg);
        infoOnlyController.actionListener = actionListener;
        return infoOnlyController;
    }

    @Override
    public void afterInitialized(BaseComponent comp) {
        super.afterInitialized(comp);
        Window window = (Window) comp;
        defaultTitle = window.getTitle();
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
            lblHeader.setLabel(message.getSubject());
            txtMessage.setValue(text != null ? text : message.getBody());
            txtMessage.setVisible(!txtMessage.getValue().isEmpty());
            btnDelete.setDisabled(!message.canDelete());
            btnDeleteAll.setDisabled(message.isActionable() || btnDelete.isDisabled());
            btnSkipAll.setDisabled(message.isActionable());
            btnView.setDisabled(!message.hasPatient());
            window.setTitle(message.hasPatient() ? message.getPatientName() : defaultTitle);
            root.setVisible(true);
        } else {
            onAction(null);
        }
    }

    /**
     * Delete the message.
     */
    @EventHandler(value = "click", target = "@btnDelete")
    private void onClick$btnDelete() {
        DialogUtil.confirm(StrUtil.formatMessage("@cwfmessagebox.viewer.delete.confirm.prompt"), (confirm) -> {
            if (confirm) {
                onAction(Action.DELETE);
            }
        });
    }

    /**
     * Delete this and all remaining messages.
     */
    @EventHandler(value = "click", target = "@btnDeleteAll")
    private void onClick$btnDeleteAll() {
        DialogUtil.confirm(StrUtil.formatMessage("@cwfmessagebox.viewer.delete.all.confirm.prompt"), (confirm) -> {
            if (confirm) {
                onAction(Action.DELETE_ALL);
            }
        });
    }

    /**
     * Skip this message.
     */
    @EventHandler(value = "click", target = "btnSkip")
    private void onClick$btnSkip() {
        onAction(Action.SKIP);
    }

    /**
     * Skip this and all remaining messages.
     */
    @EventHandler(value = "click", target = "@btnSkipAll")
    private void onClick$btnSkipAll() {
        onAction(Action.SKIP_ALL);
    }

    /**
     * Cancel message processing.
     */
    @EventHandler(value = "click", target = "btnCancel")
    private void onClick$btnCancel() {
        onAction(Action.CANCEL);
    }

    /**
     * Change context to patient associated with the message.
     */
    @EventHandler(value = "click", target = "@btnView")
    private void onClick$btnView() {
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
