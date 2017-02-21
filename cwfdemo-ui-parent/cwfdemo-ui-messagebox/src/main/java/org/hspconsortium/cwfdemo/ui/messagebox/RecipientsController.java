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
import org.carewebframework.ui.dialog.PopupDialog;
import org.carewebframework.ui.render.AbstractRenderer;
import org.carewebframework.ui.util.CWFUtil;
import org.carewebframework.web.ancillary.IResponseCallback;
import org.carewebframework.web.annotation.EventHandler;
import org.carewebframework.web.annotation.WiredComponent;
import org.carewebframework.web.component.BaseComponent;
import org.carewebframework.web.component.Hyperlink;
import org.carewebframework.web.component.Listbox;
import org.carewebframework.web.component.Listitem;
import org.carewebframework.web.component.Textbox;
import org.carewebframework.web.event.ChangeEvent;
import org.carewebframework.web.event.DblclickEvent;
import org.carewebframework.web.model.ListModel;
import org.hspconsortium.cwfdemo.api.ucs.MessageService;
import org.socraticgrid.hl7.services.uc.model.UserContactInfo;

/**
 * Controller for adding recipients.
 */
public class RecipientsController extends FrameworkController {

    private static final String DIALOG = CWFUtil.getResourcePath(RecipientsController.class) + "recipients.cwf";
    
    /**
     * Renderer for each recipient list box. A double click target must be specified to which all
     * list item double click events will be forwarded as single click events.
     */
    private class ItemRenderer extends AbstractRenderer<Listitem, UserContactInfo> {

        private final BaseComponent doubleClickTarget;
        
        ItemRenderer(BaseComponent doubleClickTarget) {
            this.doubleClickTarget = doubleClickTarget;
        }
        
        @Override
        public Listitem render(UserContactInfo recipient) {
            Listitem item = new Listitem();
            item.addEventForward(DblclickEvent.TYPE, doubleClickTarget, null);
            item.setLabel(recipient.getName());
            return item;
        }
    };
    
    @WiredComponent
    private Listbox lstRecipients;
    
    @WiredComponent
    private Listbox lstUsers;
    
    @WiredComponent
    private Listbox lstGroups;
    
    @WiredComponent
    private Textbox txtComment;
    
    @WiredComponent
    private Hyperlink btnAdd;
    
    @WiredComponent
    private Hyperlink btnRemove;
    
    @WiredComponent
    private Hyperlink btnRemoveAll;
    
    private MessageService service;
    
    private final ListModel<UserContactInfo> modelRecipients = new ListModel<>();
    
    private final ListModel<UserContactInfo> modelUsers = new ListModel<>();
    
    private final ListModel<UserContactInfo> modelGroups = new ListModel<>();
    
    private Listbox lstActive;
    
    private Collection<UserContactInfo> recipients;
    
    /**
     * Display the dialog modally, hiding the comment input element.
     *
     * @param recipients Recipient list to update.
     * @param callback Reports true if the recipient list was updated.
     */
    protected static void show(Collection<UserContactInfo> recipients, IResponseCallback<Boolean> callback) {
        show(recipients, false, (Object response) -> {
            if (callback != null) {
                callback.onComplete(response != null);
            }
        });
    }
    
    /**
     * Display the dialog modally, showing the comment input element.
     *
     * @param recipients Recipient list to update.
     * @param callback The callback to report the comment text, or null if the dialog was cancelled.
     */
    protected static void showWithComment(Collection<UserContactInfo> recipients, IResponseCallback<String> callback) {
        show(recipients, true, callback);
    }
    
    /**
     * Display the dialog modally.
     *
     * @param recipients Recipient list to update.
     * @param showComment If true, display the comment input element. If false, hide it.
     * @param callback The callback to report the value returned by the dialog or null if the dialog
     *            was cancelled.
     */
    @SuppressWarnings("unchecked")
    private static <T> void show(Collection<UserContactInfo> recipients, boolean showComment,
                                 IResponseCallback<T> callback) {
        Map<String, Object> args = new HashMap<>();
        args.put("recipients", recipients);
        args.put("showComment", showComment);
        PopupDialog.show(DIALOG, args, false, false, true, (event) -> {
            if (callback != null) {
                callback.onComplete((T) event.getTarget().getAttribute("ok"));
            }
        });
    }
    
    /**
     * Retrieve passed arguments. Initialize listbox renderers and models.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void afterInitialized(BaseComponent comp) {
        super.afterInitialized(comp);
        recipients = (Collection<UserContactInfo>) comp.getAttribute("recipients");
        txtComment.addStyle("visibility", (Boolean) comp.getAttribute("showComment") ? null : "hidden");
        lstRecipients.setRenderer(new ItemRenderer(btnRemove));
        lstRecipients.setModel(modelRecipients);
        lstGroups.setRenderer(new ItemRenderer(btnAdd));
        lstGroups.setModel(modelGroups);
        lstUsers.setRenderer(new ItemRenderer(btnAdd));
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
    @EventHandler(value = "change", target = { "@lstRecipients", "@lstGroups", "@lstUsers" })
    private void onChange$lst() {
        updateControls();
    }
    
    /**
     * Set the active candidate list as the input focus changes.
     */
    @EventHandler(value = "focus", target = { "@lstUsers", "txtUsers" })
    private void onFocus$txtUsers() {
        setActiveList(lstUsers);
    }
    
    /**
     * Set the active candidate list as the input focus changes.
     */
    @EventHandler(value = "focus", target = { "@lstGroups", "txtGroups" })
    private void onFocus$txtGroups() {
        setActiveList(lstGroups);
    }
    
    /**
     * Perform user search based in text input.
     *
     * @param event The onChanging event.
     */
    @EventHandler(value = "change", target = "txtUsers")
    private void onChange$txtUsers(ChangeEvent event) {
        String text = event.getValue().toString().trim().toUpperCase();
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
    @EventHandler(value = "change", target = "txtGroups")
    private void onChange$txtGroups(ChangeEvent event) {
        String text = event.getValue().toString().trim().toUpperCase();
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
        return selItem == null ? null : (UserContactInfo) selItem.getData();
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
    @EventHandler(value = "click", target = "@btnAdd")
    private void onClick$btnAdd() {
        modelRecipients.add(getSelected(lstActive));
    }
    
    /**
     * Remove a recipient from the current recipient list.
     */
    @EventHandler(value = "click", target = "@btnRemove")
    private void onClick$btnRemove() {
        modelRecipients.remove(getSelected(lstRecipients));
    }
    
    /**
     * Remove all recipients from the current recipient list.
     */
    @EventHandler(value = "click", target = "@btnRemoveAll")
    private void onClick$btnRemoveAll() {
        modelRecipients.clear();
    }
    
    /**
     * Update the original recipient list, set the response attribute, and close the dialog
     */
    @EventHandler(value = "click", target = "btnOk")
    private void onClick$btnOK() {
        recipients.clear();
        recipients.addAll(modelRecipients);
        root.setAttribute("ok", txtComment.isVisible() ? txtComment.getValue() : true);
        root.detach();
    }
}
