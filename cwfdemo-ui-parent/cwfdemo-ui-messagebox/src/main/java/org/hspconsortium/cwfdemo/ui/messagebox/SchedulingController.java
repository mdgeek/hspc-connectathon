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
import org.carewebframework.ui.zk.AbstractListitemRenderer;
import org.carewebframework.ui.zk.PopupDialog;
import org.carewebframework.ui.zk.PromptDialog;
import org.carewebframework.ui.zk.ZKUtil;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;

import org.hspconsortium.cwfdemo.api.ucs.MessageService;
import org.hspconsortium.cwfdemo.api.ucs.ScheduledMessage;

/**
 * Controller for viewing scheduled messages.
 */
public class SchedulingController extends FrameworkController {
    
    
    private static final long serialVersionUID = 1L;
    
    private static final String DIALOG = ZKUtil.getResourcePath(SchedulingController.class) + "scheduling.zul";
    
    /**
     * Renders the scheduled messages.
     */
    private final AbstractListitemRenderer<ScheduledMessage, Object> renderer = new AbstractListitemRenderer<ScheduledMessage, Object>() {
        
        
        @Override
        protected void renderItem(Listitem item, ScheduledMessage message) {
            createCell(item, null).setImage(UrgencyRenderer.getIconPath(message.getUrgency()));
            createCell(item, message.getDeliveryDate());
            createCell(item, message.getPatientName());
            createCell(item, message.getSubject());
            item.addForward(Events.ON_DOUBLE_CLICK, btnModify, Events.ON_CLICK);
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
    
    private Listbox lstScheduled;
    
    private Button btnModify;
    
    private Button btnDelete;
    
    private MessageService service;
    
    private final ListModelList<ScheduledMessage> model = new ListModelList<>();
    
    /**
     * Displays the scheduling controller modally.
     */
    public static void show() {
        PopupDialog.popup(DIALOG, true, false);
    }
    
    /**
     * Update controls to reflect the current selection state.
     */
    private void updateControls() {
        btnModify.setDisabled(lstScheduled.getSelectedItem() == null);
        btnDelete.setDisabled(btnModify.isDisabled());
    }
    
    /**
     * Adds a new scheduled message.
     */
    public void onClick$btnAdd() {
        ScheduleController.show(null);
    }
    
    /**
     * Modifies an existing scheduled message.
     */
    public void onClick$btnModify() {
        ScheduleController.show(getSelected());
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
    public void onSelect$lstScheduled() {
        updateControls();
    }
    
    /**
     * Delete the selected scheduled message.
     */
    public void onClick$btnDelete() {
        if (PromptDialog.confirm("@cwfmessagebox.scheduling.delete.confirm.prompt")) {
            service.deleteScheduledMessage(getSelected());
        }
    }
    
    /**
     * Returns the currently selected message.
     * 
     * @return The currently selected message.
     */
    private ScheduledMessage getSelected() {
        return (ScheduledMessage) lstScheduled.getSelectedItem().getValue();
    }
    
    @Override
    public void doBeforeComposeChildren(Component comp) throws Exception {
        super.doBeforeComposeChildren(comp);
        comp.setAttribute("iconUrgency", Constants.ICON_URGENCY);
    }
    
    /**
     * Initialize the dialog.
     */
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        lstScheduled.setItemRenderer(renderer);
        refresh();
        getEventManager().subscribe("MESSAGE.SCHEDULE", alertEventListener);
    }
    
    /**
     * Refresh the display.
     */
    @Override
    public void refresh() {
        lstScheduled.setModel((ListModel<?>) null);
        model.clear();
        model.addAll(service.getScheduledMessages());
        lstScheduled.setModel(model);
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
