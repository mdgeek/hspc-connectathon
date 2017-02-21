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

import org.carewebframework.api.event.IGenericEvent;
import org.carewebframework.ui.FrameworkController;
import org.carewebframework.ui.dialog.DialogUtil;
import org.carewebframework.ui.render.AbstractRenderer;
import org.carewebframework.ui.util.CWFUtil;
import org.carewebframework.web.annotation.WiredComponent;
import org.carewebframework.web.component.BaseComponent;
import org.carewebframework.web.component.Button;
import org.carewebframework.web.component.Grid;
import org.carewebframework.web.component.Row;
import org.carewebframework.web.event.ClickEvent;
import org.carewebframework.web.event.DblclickEvent;
import org.carewebframework.web.model.ListModel;
import org.hspconsortium.cwfdemo.api.ucs.MessageService;
import org.hspconsortium.cwfdemo.api.ucs.ScheduledMessage;

/**
 * Controller for viewing scheduled messages.
 */
public class SchedulingController extends FrameworkController {

    private static final String DIALOG = CWFUtil.getResourcePath(SchedulingController.class) + "scheduling.cwf";
    
    /**
     * Renders the scheduled messages.
     */
    private final AbstractRenderer<Row, ScheduledMessage> renderer = new AbstractRenderer<Row, ScheduledMessage>() {

        @Override
        public Row render(ScheduledMessage message) {
            Row row = new Row();
            createImage(row, UrgencyRenderer.getIconPath(message.getUrgency()));
            createLabel(row, message.getDeliveryDate());
            createLabel(row, message.getPatientName());
            createLabel(row, message.getSubject());
            row.addEventForward(DblclickEvent.TYPE, btnModify, ClickEvent.TYPE);
            return row;
        }
    };
    
    /**
     * Listens to events related to scheduled messages.
     */
    private final IGenericEvent<String> alertEventListener = new IGenericEvent<String>() {

        @Override
        public void eventCallback(String eventName, String eventData) {
            refresh();
        }
        
    };
    
    @WiredComponent
    private Grid grdScheduled;
    
    @WiredComponent
    private Button btnModify;
    
    @WiredComponent
    private Button btnDelete;
    
    private MessageService service;
    
    private final ListModel<ScheduledMessage> model = new ListModel<>();
    
    /**
     * Displays the scheduling controller modally.
     */
    public static void show() {
        DialogUtil.popup(DIALOG, true, false);
    }
    
    /**
     * Update controls to reflect the current selection state.
     */
    private void updateControls() {
        btnModify.setDisabled(grdScheduled.getRows().getSelectedRow() == null);
        btnDelete.setDisabled(btnModify.isDisabled());
    }
    
    /**
     * Adds a new scheduled message.
     */
    public void onClick$btnAdd() {
        ScheduleController.show(null, null);
    }
    
    /**
     * Modifies an existing scheduled message.
     */
    public void onClick$btnModify() {
        ScheduleController.show(getSelected(), null);
    }
    
    /**
     * Refreshes the list.
     */
    public void onClick$btnRefresh() {
        refresh();
    }
    
    /**
     * Update controls when the selection changes.
     */
    public void onSelect$grdScheduled() {
        updateControls();
    }
    
    /**
     * Delete the selected scheduled message.
     */
    public void onClick$btnDelete() {
        DialogUtil.confirm("@cwfmessagebox.scheduling.delete.confirm.prompt", (confirm) -> {
            if (confirm) {
                service.deleteScheduledMessage(getSelected());
            }
        });
    }
    
    /**
     * Returns the currently selected message.
     *
     * @return The currently selected message.
     */
    private ScheduledMessage getSelected() {
        return (ScheduledMessage) grdScheduled.getRows().getSelectedRow().getData();
    }
    
    /**
     * Initialize the dialog.
     */
    @Override
    public void afterInitialized(BaseComponent comp) {
        super.afterInitialized(comp);
        comp.setAttribute("iconUrgency", Constants.ICON_URGENCY);
        grdScheduled.getRows().setRenderer(renderer);
        refresh();
        getEventManager().subscribe("MESSAGE.SCHEDULE", alertEventListener);
    }
    
    /**
     * Refresh the display.
     */
    @Override
    public void refresh() {
        grdScheduled.getRows().setModel(null);
        model.clear();
        model.addAll(service.getScheduledMessages());
        grdScheduled.getRows().setModel(model);
        updateControls();
    }
    
    /**
     * Unsubscribe on dialog closure.
     */
    public void onClose() {
        getEventManager().unsubscribe("MESSAGE.SCHEDULE", alertEventListener);
    }
    
    /**
     * Allows IOC container to inject message service.
     *
     * @param service Message service.
     */
    public void setMessageService(MessageService service) {
        this.service = service;
    }
    
}
