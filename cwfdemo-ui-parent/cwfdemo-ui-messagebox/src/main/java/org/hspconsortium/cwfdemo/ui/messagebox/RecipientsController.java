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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.carewebframework.ui.FrameworkController;
import org.carewebframework.ui.zk.AbstractListitemRenderer;
import org.carewebframework.ui.zk.PopupDialog;
import org.carewebframework.ui.zk.ZKUtil;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.A;
import org.zkoss.zul.ListModelSet;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Textbox;

import org.hspconsortium.cwf.api.ucs.MessageService;
import org.socraticgrid.hl7.services.uc.model.UserContactInfo;

/**
 * Controller for adding recipients.
 */
public class RecipientsController extends FrameworkController {
    
    
    private static final long serialVersionUID = 1L;
    
    private static final String DIALOG = ZKUtil.getResourcePath(RecipientsController.class) + "recipients.zul";
    
    /**
     * Renderer for each recipient list box. A double click target must be specified to which all
     * list item double click events will be forwarded as single click events.
     */
    private class ItemRenderer extends AbstractListitemRenderer<UserContactInfo, Object> {
        
        
        private final Component doubleClickTarget;
        
        ItemRenderer(Component doubleClickTarget) {
            this.doubleClickTarget = doubleClickTarget;
        }
        
        @Override
        protected void renderItem(Listitem item, UserContactInfo recipient) {
            item.addForward(Events.ON_DOUBLE_CLICK, doubleClickTarget, Events.ON_CLICK);
            item.setLabel(recipient.getName());
        }
    };
    
    private Listbox lstRecipients;
    
    private Listbox lstUsers;
    
    private Listbox lstGroups;
    
    private Textbox txtComment;
    
    private A btnAdd;
    
    private A btnRemove;
    
    private A btnRemoveAll;
    
    private MessageService service;
    
    private final ListModelSet<UserContactInfo> modelRecipients = new ListModelSet<>();
    
    private final ListModelSet<UserContactInfo> modelUsers = new ListModelSet<>();
    
    private final ListModelSet<UserContactInfo> modelGroups = new ListModelSet<>();
    
    private Listbox lstActive;
    
    private Collection<UserContactInfo> recipients;
    
    /**
     * Display the dialog modally, hiding the comment input element.
     * 
     * @param recipients Recipient list to update.
     * @return True if the recipient list was updated.
     */
    protected static boolean show(Collection<UserContactInfo> recipients) {
        return show(recipients, false) != null;
    }
    
    /**
     * Display the dialog modally, showing the comment input element.
     * 
     * @param recipients Recipient list to update.
     * @return The comment text, or null if the dialog was cancelled.
     */
    protected static String showWithComment(Collection<UserContactInfo> recipients) {
        return (String) show(recipients, true);
    }
    
    /**
     * Display the dialog modally.
     * 
     * @param recipients Recipient list to update.
     * @param showComment If true, display the comment input element. If false, hide it.
     * @return The value returned by the dialog or null if the dialog was cancelled.
     */
    private static Object show(Collection<UserContactInfo> recipients, boolean showComment) {
        Map<Object, Object> args = new HashMap<>();
        args.put("recipients", recipients);
        args.put("showComment", showComment);
        return PopupDialog.popup(DIALOG, args, false, false, true).getAttribute("ok");
    }
    
    /**
     * Retrieve passed arguments. Initialize listbox renderers and models.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        recipients = (Collection<UserContactInfo>) arg.get("recipients");
        ZKUtil.updateStyle(txtComment, "visibility", (Boolean) arg.get("showComment") ? null : "hidden");
        lstRecipients.setItemRenderer(new ItemRenderer(btnRemove));
        lstRecipients.setModel(modelRecipients);
        lstGroups.setItemRenderer(new ItemRenderer(btnAdd));
        lstGroups.setModel(modelGroups);
        lstUsers.setItemRenderer(new ItemRenderer(btnAdd));
        lstUsers.setModel(modelUsers);
        modelRecipients.addAll(recipients);
    }
    
    /**
     * Allows IOC container to inject notification service.
     * 
     * @param service Message service.
     */
    public void setMessageService(MessageService service) {
        this.service = service;
    }
    
    /**
     * Update controllers when list box selection changes.
     */
    public void onSelect$lstUsers() {
        updateControls();
    }
    
    /**
     * Update controllers when list box selection changes.
     */
    public void onSelect$lstGroups() {
        updateControls();
    }
    
    /**
     * Update controllers when list box selection changes.
     */
    public void onSelect$lstRecipients() {
        updateControls();
    }
    
    /**
     * Set the active candidate list as the input focus changes.
     */
    public void onFocus$lstUsers() {
        setActiveList(lstUsers);
    }
    
    /**
     * Set the active candidate list as the input focus changes.
     */
    public void onFocus$lstGroups() {
        setActiveList(lstGroups);
    }
    
    /**
     * Set the active candidate list as the input focus changes.
     */
    public void onFocus$txtUsers() {
        setActiveList(lstUsers);
    }
    
    /**
     * Set the active candidate list as the input focus changes.
     */
    public void onFocus$txtGroups() {
        setActiveList(lstGroups);
    }
    
    /**
     * Perform user search based in text input.
     * 
     * @param event The onChanging event.
     */
    public void onChanging$txtUsers(InputEvent event) {
        String text = event.getValue().trim().toUpperCase();
        modelUsers.clear();
        
        if (text.length() >= 3) {
            modelUsers.addAll(service.queryUsers(text));
        }
    }
    
    /**
     * Perform mail group search based in text input.
     * 
     * @param event The onChanging event.
     */
    public void onChanging$txtGroups(InputEvent event) {
        String text = event.getValue().trim().toUpperCase();
        modelGroups.clear();
        
        if (text.length() >= 3) {
            modelGroups.addAll(service.queryUsers(text));
        }
    }
    
    /**
     * Sets the active candidate list and updates the controls.
     * 
     * @param lst A list box.
     */
    private void setActiveList(Listbox lst) {
        lstActive = lst;
        updateControls();
    }
    
    /**
     * Returns the recipient selected in the specified list, or null if no selection.
     * 
     * @param lst A list box.
     * @return The selected recipient, or null if no selection.
     */
    private UserContactInfo getSelected(Listbox lst) {
        Listitem selItem = lst == null ? null : lst.getSelectedItem();
        return selItem == null ? null : (UserContactInfo) selItem.getValue();
    }
    
    /**
     * Update controls to reflect the current selection state.
     */
    private void updateControls() {
        btnAdd.setDisabled(getSelected(lstActive) == null);
        btnRemove.setDisabled(getSelected(lstRecipients) == null);
        btnRemoveAll.setDisabled(modelRecipients.isEmpty());
    }
    
    /**
     * Add a recipient from the active candidate list.
     */
    public void onClick$btnAdd() {
        modelRecipients.add(getSelected(lstActive));
    }
    
    /**
     * Remove a recipient from the current recipient list.
     */
    public void onClick$btnRemove() {
        modelRecipients.remove(getSelected(lstRecipients));
    }
    
    /**
     * Remove all recipients from the current recipient list.
     */
    public void onClick$btnRemoveAll() {
        modelRecipients.clear();
    }
    
    /**
     * Update the original recipient list, set the response attribute, and close the dialog
     */
    public void onClick$btnOK() {
        recipients.clear();
        recipients.addAll(modelRecipients);
        root.setAttribute("ok", txtComment.isVisible() ? txtComment.getText() : true);
        root.detach();
    }
}
