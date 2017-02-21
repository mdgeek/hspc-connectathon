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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.carewebframework.api.context.ISurveyResponse;
import org.carewebframework.api.context.UserContext;
import org.carewebframework.api.event.IGenericEvent;
import org.carewebframework.common.NumUtil;
import org.carewebframework.common.StrUtil;
import org.carewebframework.shell.plugins.PluginController;
import org.carewebframework.ui.dialog.DialogControl.IPromptCallback;
import org.carewebframework.ui.dialog.PromptDialog;
import org.carewebframework.ui.util.CWFUtil;
import org.carewebframework.web.ancillary.Badge;
import org.carewebframework.web.annotation.EventHandler;
import org.carewebframework.web.annotation.WiredComponent;
import org.carewebframework.web.component.BaseComponent;
import org.carewebframework.web.component.Button;
import org.carewebframework.web.component.Column;
import org.carewebframework.web.component.Grid;
import org.carewebframework.web.component.Image;
import org.carewebframework.web.component.Label;
import org.carewebframework.web.component.MessagePane;
import org.carewebframework.web.component.Radiobutton;
import org.carewebframework.web.component.Radiogroup;
import org.carewebframework.web.component.Row;
import org.carewebframework.web.event.ClickEvent;
import org.carewebframework.web.event.Event;
import org.carewebframework.web.model.ListModel;
import org.hl7.fhir.dstu3.model.Patient;
import org.hspconsortium.cwf.api.patient.PatientContext;
import org.hspconsortium.cwf.api.patient.PatientContext.IPatientContextEvent;
import org.hspconsortium.cwf.fhir.common.FhirUtil;
import org.hspconsortium.cwfdemo.api.ucs.MessageService;
import org.hspconsortium.cwfdemo.api.ucs.MessageWrapper;
import org.hspconsortium.cwfdemo.api.ucs.Urgency;
import org.socraticgrid.hl7.services.uc.model.AlertMessage;
import org.socraticgrid.hl7.services.uc.model.Message;
import org.socraticgrid.hl7.services.uc.model.UserContactInfo;

/**
 * Controller for main message display.
 */
public class MainController extends PluginController implements IPatientContextEvent {
    
    /**
     * Response types for information-only message processing.
     */
    private enum Response {
        YES, NO, ALL, CANCEL;
        
        @Override
        public String toString() {
            return StrUtil.getLabel("cwfmessagebox.response.label." + name());
        }
    }
    
    public enum ViewMode {
        SELECTABLE, // User may set view mode
        PATIENT, // View mode set to current patient only
        ALL // View mode set to all patients
    }
    
    /**
     * Recognized message actions.
     */
    protected enum Action {
        CHECK, ADD, SCHEDULE, INFO, REFRESH, DELETE, MONITOR;
        
        public String eventName() {
            return "MESSAGE." + name();
        }
    }
    
    // This is the listener for notification action messages.
    private final IGenericEvent<Object> actionListener = new IGenericEvent<Object>() {
        
        @Override
        public void eventCallback(String eventName, Object eventData) {
            Action action = Action.valueOf(StrUtil.piece(eventName, ".", 2));
            MessageWrapper message = null;
            String messageId = null;
            
            if (eventData instanceof Message) {
                message = new MessageWrapper((Message) eventData);
                messageId = message.getId();
            } else if (eventData instanceof MessageWrapper) {
                message = (MessageWrapper) eventData;
                messageId = message.getId();
            } else {
                messageId = eventData.toString();
            }
            
            switch (action) {
                case ADD:
                    if (message == null) {
                        addMessage(service.getMessageWithId(messageId));
                    } else {
                        addMessage(message);
                    }
                    
                    break;
                
                case INFO:
                    message = findMessage(messageId);
                    
                    if (message != null) {
                        highlightMessage(message);
                        getPlugin().bringToFront();
                    }
                    
                    break;
                
                case REFRESH:
                    refresh();
                    break;
                
                case DELETE:
                    removeMessage(messageId, true);
                    break;
            }
        }
        
    };
    
    @WiredComponent
    private Grid grdMessages;
    
    @WiredComponent
    private Radiogroup rgFilter;
    
    @WiredComponent
    private Radiobutton radAll;
    
    @WiredComponent
    private Radiobutton radPatient;
    
    @WiredComponent
    private Button btnAll;
    
    @WiredComponent
    private Button btnSelected;
    
    @WiredComponent
    private Button btnInfoAll;
    
    @WiredComponent
    private Button btnForward;
    
    @WiredComponent
    private Button btnDelete;
    
    @WiredComponent
    private Button btnRefresh;
    
    @WiredComponent
    private Image imgIndicator;
    
    private Badge badge;
    
    private MessageService service;
    
    private ProcessingController processingController;
    
    private final ListModel<MessageWrapper> model = new ListModel<>();
    
    private boolean showAll = true;
    
    private ViewMode viewMode = ViewMode.SELECTABLE;
    
    private Urgency alertThreshold = Urgency.HIGH;
    
    private int alertDuration = 30;
    
    private boolean isProcessing;
    
    private Patient patient;
    
    /**
     * Expose icon urls for auto-wiring.
     */
    @Override
    public void afterInitialized(BaseComponent comp) {
        super.afterInitialized(comp);
        comp.setAttribute("iconInfoOnly", Constants.ICON_INFO);
        comp.setAttribute("iconInfoOnly", Constants.ICON_INFO);
        comp.setAttribute("iconActionable", Constants.ICON_ACTIONABLE);
        comp.setAttribute("iconUrgency", Constants.ICON_URGENCY);
        comp.setAttribute("iconType", Constants.ICON_TYPE);
        comp.setAttribute("iconIndicator", Constants.ICON_INDICATOR);
        comp.setAttribute("iconUrgencyHigh", Constants.ICON_URGENCY_HIGH);
        comp.setAttribute("iconUrgencyMedium", Constants.ICON_URGENCY_MEDIUM);
        comp.setAttribute("iconUrgencyLow", Constants.ICON_URGENCY_LOW);
        badge = new Badge(root);
        model.addEventListener((type, start, end) -> {
            updateBadge();
        });
        service.addAlertListener(new AlertListener(this));
        service.addMessageListener(new MessageListener(this));
        getPlugin().registerProperties(this, "viewMode", "alertDuration", "alertThreshold");
        (showAll ? radAll : radPatient).setChecked(true);
        processingController = ProcessingController.create(this);
        grdMessages.getRows().setRenderer(new MessageRenderer(grdMessages));
        updatePatient(true);
        subscribe(true);
        root.findByName("mnuRefresh").addEventForward(ClickEvent.TYPE, btnRefresh, null);
    }
    
    @Override
    public void cleanup() {
        subscribe(false);
    }
    
    /**
     * Refresh the display.
     */
    @Override
    public void refresh() {
        showBusy(null);
        grdMessages.getRows().setModel(null);
        loadMessages(!radAll.isChecked());
        grdMessages.getRows().setModel(model);
        updateControls(false);
    }
    
    private void updateBadge() {
        badge.setCount(model.size());
    }
    
    private void loadMessages(boolean currentPatientOnly) {
        String userId = UserContext.getActiveUser().getLogicalId();
        String patientId = currentPatientOnly && patient != null ? FhirUtil.getIdAsString(patient, true) : null;
        model.clear();
        
        try {
            List<Message> messages = service.getMessagesByRecipient(userId, patientId);
            
            for (Message message : messages) {
                model.add(new MessageWrapper(message));
            }
            showBusy(null);
        } catch (Exception e) {
            showBusy(CWFUtil.formatExceptionForDisplay(e));
        }
    }
    
    /**
     * Update controls to reflect the current selection state.
     *
     * @param processingUpdate If true, a processing status update has occurred.
     */
    private void updateControls(boolean processingUpdate) {
        btnAll.setDisabled(isProcessing || model.isEmpty());
        btnDelete.setDisabled(isProcessing || !canDeleteSelected());
        btnInfoAll.setDisabled(isProcessing || !hasInfoOnly());
        btnSelected.setDisabled(isProcessing || grdMessages.getRows().getSelectedCount() == 0);
        btnForward.setDisabled(isProcessing || btnSelected.isDisabled());
        radAll.addStyles(radAll.isChecked() ? Constants.BOLD : Constants.NO_BOLD);
        radPatient.addStyles(radPatient.isChecked() ? Constants.BOLD : Constants.NO_BOLD);
        
        if (processingUpdate) {
            grdMessages.setDisabled(isProcessing);
            CWFUtil.disableChildren(grdMessages, isProcessing);
        }
    }
    
    /**
     * Returns true if any selected message may be deleted.
     *
     * @return True if any selected message may be deleted.
     */
    private boolean canDeleteSelected() {
        for (Row row : grdMessages.getRows().getSelected()) {
            MessageWrapper message = (MessageWrapper) row.getData();
            
            if (message.canDelete()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Returns true if any selected message is information only.
     *
     * @return True if any selected message is information only.
     */
    private boolean hasInfoOnly() {
        for (MessageWrapper message : model) {
            if (!message.isActionable()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Prompts user for input, returning the response.
     *
     * @param prompt Text prompt.
     * @param responses Valid responses.
     * @param callback Callback to report response.
     */
    private void getResponse(String prompt, IPromptCallback<Response> callback, Response... responses) {
        PromptDialog.show(prompt, null, null, responses, null, null, null, (response) -> {
            if (callback != null) {
                callback.onComplete(response);
            }
        });
    }
    
    protected void addMessage(Message message) {
        if (message != null) {
            addMessage(new MessageWrapper(message));
        }
    }
    
    /**
     * Adds a message to the model unless filtered. Will generate a slide-down message alert if its
     * urgency exceeds the set threshold.
     *
     * @param message Message.
     */
    protected void addMessage(MessageWrapper message) {
        if (!(message.getMessage() instanceof AlertMessage)) {
            return;
        }
        
        int i = indexOfMessage(message.getId());
        
        if (i >= 0) {
            model.set(i, message);
            return;
        }
        
        if (radAll.isChecked() || (message.hasPatient() && patient != null
                && message.getPatientId().equals(patient.getIdElement().getIdPart()))) {
            model.add(message);
        }
        
        if (alertThreshold != null && message.getUrgency().ordinal() <= alertThreshold.ordinal()) {
            MessagePane mi = new MessagePane("New Message", "messagebox", alertDuration * 1000, true);
            mi.addChild(new Label(message.getDisplayText()));
            mi.addClass("flavor:panel-" + UrgencyRenderer.getColor(message.getUrgency()));
            mi.show();
        }
    }
    
    /**
     * Removes a message from this list, if it exists.
     *
     * @param id The message id.
     * @param modelOnly If true, remove from model only, not from server.
     */
    protected void removeMessage(String id, boolean modelOnly) {
        int i = indexOfMessage(id);
        
        if (i >= 0) {
            MessageWrapper message = model.remove(i);
            
            if (!modelOnly) {
                service.cancelMessage(message.getMessage(), false);
            }
        }
    }
    
    /**
     * Locates and returns a message based on its unique id.
     *
     * @param id Message id.
     * @return Message with a matching id, or null if not found in the current model.
     */
    private MessageWrapper findMessage(String id) {
        int i = indexOfMessage(id);
        return i < 0 ? null : model.get(i);
    }
    
    private int indexOfMessage(String id) {
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i).getId().equals(id)) {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * Places the highlight indicator next to the specified message. If the message is not found or
     * is null, the indicator is hidden.
     *
     * @param message Message to highlight.
     */
    protected void highlightMessage(MessageWrapper message) {
        int i = message == null ? -1 : model.indexOf(message);
        
        if (i >= 0) {
            Row row = (Row) grdMessages.getRows().getChildAt(i);
            imgIndicator.setParent(row.getFirstChild());
            imgIndicator.setVisible(true);
        } else {
            imgIndicator.setVisible(false);
        }
    }
    
    /**
     * Clears all selections.
     */
    private void clearSelection() {
        grdMessages.getRows().clearSelected();
        updateControls(false);
    }
    
    /**
     * Update controls when the selection changes.
     */
    @EventHandler(value = "change", target = "@grdMessages")
    private void onChange$grdMessages() {
        updateControls(false);
    }
    
    /**
     * Refresh the display.
     */
    @EventHandler(value = "click", target = "@btnRefresh")
    @EventHandler(value = "change", target = "@rgFilter")
    private void doRefresh() {
        refresh();
    }
    
    /**
     * Delete selected messages.
     */
    @EventHandler(value = "click", target = "@btnDelete")
    private void onClick$btnDelete() {
        nextMessage(grdMessages.getRows().getSelected().iterator(), false);
    }

    /**
     * Iterates through all selected messages.
     *
     * @param iter
     * @param silent
     */
    private void nextMessage(Iterator<Row> iter, boolean silent) {
        if (!iter.hasNext()) {
            return;
        }

        Row row = iter.next();
        MessageWrapper message = (MessageWrapper) row.getData();
        String s = message.getDisplayText();
        
        if (message.canDelete()) {
            if (silent) {
                removeMessage(message.getId(), false);
                nextMessage(iter, silent);
            } else {
                String msg = StrUtil.getLabel("cwfmessagebox.main.delete.confirm.prompt", s);
                getResponse(msg, (response) -> {
                    boolean newSilent = silent;
                    
                    switch (response.getResponse()) {
                        case NO:
                            break;

                        case CANCEL:
                            return;
                        
                        case ALL:
                            newSilent = true;
                            // Fall-through is intentional here.

                        default:
                            removeMessage(message.getId(), false);
                    }
                    
                    nextMessage(iter, newSilent);
                }, Response.YES, Response.NO, Response.ALL, Response.CANCEL);
                return;
            }
        } else {
            String msg = StrUtil.getLabel("cwfmessagebox.main.delete.unable.prompt", s);
            getResponse(msg, (response) -> {
                if (response.getResponse() == Response.YES) {
                    nextMessage(iter, silent);
                }
            }, Response.YES, Response.CANCEL);
        }
        
    }
    
    /**
     * Invoke the scheduled message management dialog.
     */
    @EventHandler(value = "click", target = "btnSchedule")
    private void onClick$btnSchedule() {
        SchedulingController.show();
    }
    
    /**
     * Process all messages.
     */
    @EventHandler(value = "click", target = "@btnAll")
    private void onClick$btnAll() {
        processingController.process(model);
    }
    
    /**
     * Process all information-only messages.
     */
    @EventHandler(value = "click", target = "btnInfoAll")
    private void onClick$btnInfoAll() {
        processingController.process(getMessagesToProcess(true));
    }
    
    /**
     * Process selected messages.
     */
    @EventHandler(value = "click", target = "@btnSelected")
    public void onClick$btnSelected() {
        processingController.process(getMessagesToProcess(false));
    }
    
    /**
     * Process a double-clicked message.
     *
     * @param event The process item event.
     */
    @EventHandler(value = "processItem", target = "@grdMessages")
    private void onProcessItem$grdMessages(Event event) {
        Row row = (Row) event.getTarget();
        MessageWrapper message = (MessageWrapper) row.getData();
        processingController.process(Collections.singleton(message));
    }
    
    /**
     * Forward selected messages.
     */
    @EventHandler(value = "click", target = "@btnForward")
    private void onClick$btnForward() {
        Set<UserContactInfo> recipients = new HashSet<>();
        RecipientsController.showWithComment(recipients, (comment) -> {
            if (comment != null && !recipients.isEmpty()) {
                List<MessageWrapper> messages = getMessagesToProcess(false);
                clearSelection();

                for (MessageWrapper message : messages) {
                    service.forwardMessage(message.getMessage(), recipients, comment);
                }
            }
        });
    }
    
    /**
     * Return messages to be processed.
     *
     * @param infoOnly If true, return all information-only messages. If false, return only selected
     *            messages.
     * @return List of messages to be processed.
     */
    private List<MessageWrapper> getMessagesToProcess(boolean infoOnly) {
        List<MessageWrapper> list = new ArrayList<>();
        Iterable<Row> rows = infoOnly ? grdMessages.getRows().getChildren(Row.class) : grdMessages.getRows().getSelected();

        for (Row row : rows) {
            MessageWrapper message = (MessageWrapper) row.getData();
            
            if (!infoOnly || !message.isActionable()) {
                list.add(message);
            }
        }
        
        return list;
    }
    
    /**
     * Conditionally suppress patient context changes.
     */
    @Override
    public void pending(ISurveyResponse response) {
        response.accept();
    }
    
    /**
     * Update display when patient context changes.
     */
    @Override
    public void committed() {
        updatePatient(radPatient.isChecked());
    }
    
    /**
     * Update display for currently selected patient.
     *
     * @param refresh If true, force a refresh before returning.
     */
    private void updatePatient(boolean refresh) {
        patient = PatientContext.getActivePatient();
        
        radPatient.setLabel(patient == null ? StrUtil.getLabel("cwfmessagebox.main.patient.not.selected")
                : FhirUtil.formatName(patient.getName()));
        
        if (refresh) {
            refresh();
        }
    }
    
    @Override
    public void canceled() {
    }
    
    /**
     * Subscribe to/unsubscribe from selected events.
     *
     * @param doSubscribe If true, subscribe. If false, unsubscribe.
     */
    private void subscribe(boolean doSubscribe) {
        for (Action action : Action.values()) {
            String eventName = action.eventName();
            
            if (doSubscribe) {
                getEventManager().subscribe(eventName, actionListener);
            } else {
                getEventManager().unsubscribe(eventName, actionListener);
            }
        }
    }
    
    /**
     * Invokes an action on the specified target in the event thread.
     *
     * @param action An action to perform.
     * @param target Target of the action. This may be a message id or a wrapped or unwrapped
     *            message;
     */
    protected void invokeAction(Action action, Object target) {
        getEventManager().fireLocalEvent(action.eventName(), target);
    }
    
    /**
     * Allows IOC container to inject UCS.
     *
     * @param service Message service.
     */
    public void setMessageService(MessageService service) {
        this.service = service;
    }
    
    /**
     * Returns show all setting. If true, all messages are displayed, regardless of any patient
     * association. If false, only messages associated with the selected patient are displayed.
     *
     * @return The show all setting.
     */
    public boolean getShowAll() {
        return showAll;
    }
    
    /**
     * Sets the show all setting. If true, all messages are displayed, regardless of any patient
     * association. If false, only messages associated with the selected patient are displayed.
     *
     * @param value The show all setting.
     */
    public void setShowAll(boolean value) {
        showAll = value;
        
        if (rgFilter != null) {
            (showAll ? radAll : radPatient).setChecked(true);
            refresh();
        }
    }
    
    /**
     * Returns the alert threshold. This threshold determines which newly arriving messages cause a
     * slide-down message alert to be displayed.
     *
     * @return The alert threshold.
     */
    public Urgency getAlertThreshold() {
        return alertThreshold;
    }
    
    /**
     * Sets the alert threshold. This threshold determines which newly arriving messages cause a
     * slide-down message alert to be displayed.
     *
     * @param value The alert threshold.
     */
    public void setAlertThreshold(Urgency value) {
        this.alertThreshold = value;
    }
    
    /**
     * Returns the duration, in seconds, of any slide-down message alert.
     *
     * @return Alert duration in seconds.
     */
    public int getAlertDuration() {
        return alertDuration;
    }
    
    /**
     * Sets the duration, in seconds, of any slide-down message alert.
     *
     * @param alertDuration Alert duration in seconds.
     */
    public void setAlertDuration(int alertDuration) {
        this.alertDuration = NumUtil.enforceRange(alertDuration, 1, 999999);
    }
    
    public void setProcessing(boolean isProcessing) {
        this.isProcessing = isProcessing;
        updateControls(true);
    }
    
    /**
     * Returns the view mode.
     *
     * @return The view mode.
     */
    public ViewMode getViewMode() {
        return viewMode;
    }
    
    /**
     * Sets the view mode.
     *
     * @param viewMode The view mode.
     */
    public void setViewMode(ViewMode viewMode) {
        this.viewMode = viewMode;
        Column header = rgFilter.getAncestor(Column.class);
        
        if (viewMode == ViewMode.SELECTABLE) {
            rgFilter.setVisible(true);
            header.setLabel(null);
        } else {
            rgFilter.setVisible(false);
            setShowAll(viewMode == ViewMode.ALL);
            header.setLabel(StrUtil.getLabel("cwfmessagebox.main.header.patient"));
        }
    }
}
