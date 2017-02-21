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
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.carewebframework.common.StrUtil;
import org.carewebframework.ui.FrameworkController;
import org.carewebframework.ui.dialog.DateTimebox;
import org.carewebframework.ui.dialog.DialogUtil;
import org.carewebframework.ui.dialog.PopupDialog;
import org.carewebframework.ui.util.CWFUtil;
import org.carewebframework.web.ancillary.IResponseCallback;
import org.carewebframework.web.annotation.EventHandler;
import org.carewebframework.web.annotation.WiredComponent;
import org.carewebframework.web.component.BaseComponent;
import org.carewebframework.web.component.BaseUIComponent;
import org.carewebframework.web.component.Checkbox;
import org.carewebframework.web.component.Combobox;
import org.carewebframework.web.component.Comboitem;
import org.carewebframework.web.component.Label;
import org.carewebframework.web.component.Textbox;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hspconsortium.cwf.api.patient.PatientContext;
import org.hspconsortium.cwf.fhir.common.FhirUtil;
import org.hspconsortium.cwfdemo.api.ucs.MessageService;
import org.hspconsortium.cwfdemo.api.ucs.ScheduledMessage;
import org.hspconsortium.cwfdemo.api.ucs.Urgency;
import org.socraticgrid.hl7.services.uc.model.UserContactInfo;

/**
 * Controller for creating or editing a scheduled message.
 */
public class ScheduleController extends FrameworkController {

    private static final String DIALOG = CWFUtil.getResourcePath(ScheduleController.class) + "schedule.cwf";
    
    private MessageService service;
    
    private ScheduledMessage message;
    
    private List<UserContactInfo> recipients = new ArrayList<>();
    
    private DateTimebox dtbDelivery;
    
    @WiredComponent
    private Combobox cboUrgency;
    
    @WiredComponent
    private Textbox txtSubject;
    
    @WiredComponent
    private Textbox txtMessage;
    
    @WiredComponent
    private Textbox txtRecipients;
    
    @WiredComponent
    private Checkbox chkAssociate;
    
    @WiredComponent
    private Label lblPatient;
    
    @WiredComponent
    private BaseUIComponent pnlAssociate;
    
    /**
     * Display the scheduled message dialog modally.
     *
     * @param message The scheduled message to be modified. Specify null to create a new scheduled
     *            message.
     * @param callback Callback to return the modified or new scheduled message, or null if the
     *            dialog was cancelled.
     */
    public static void show(ScheduledMessage message, IResponseCallback<ScheduledMessage> callback) {
        Map<String, Object> args = Collections.singletonMap("message", message);
        PopupDialog.show(DIALOG, args, true, false, true, (event) -> {
            if (callback != null) {
                callback.onComplete((ScheduledMessage) event.getTarget().getAttribute("message"));
            }
        });
    }
    
    /**
     * Initialize the dialog.
     */
    @Override
    public void afterInitialized(BaseComponent comp) {
        super.afterInitialized(comp);
        message = (ScheduledMessage) comp.getAttribute("message");
        
        for (Urgency urgency : Urgency.values()) {
            Comboitem item = new Comboitem(UrgencyRenderer.getDisplayName(urgency));
            //, UrgencyRenderer.getIconPath(urgency));
            item.setData(urgency);
            cboUrgency.addChild(item);
        }
        
        if (message == null) {
            message = new ScheduledMessage();
            message.setUrgency(Urgency.LOW);
            message.setDeliveryDate(new Date());
            //TODO: recipients.add(new UserContactInfo(UserContext.getActiveUser()));
        } else {
            recipients = service.getScheduledNotificationRecipients(message);
        }
        
        populateForm();
    }
    
    /**
     * Populate the dialog based on values from the scheduled message.
     */
    private void populateForm() {
        dtbDelivery.setDate(message.getDeliveryDate());
        //dtbDelivery.setConstraint("no past");
        Comboitem item = (Comboitem) cboUrgency.findChildByData(message.getUrgency());
        
        if (item != null) {
            item.setSelected(true);
        }
        
        txtSubject.setValue(message.getSubject());
        txtMessage.setValue(message.getBody());
        
        if (message.hasPatient()) {
            lblPatient.setLabel(message.getPatientName());
            chkAssociate.setChecked(true);
            chkAssociate.setData(message.getPatientId());
        } else {
            Patient patient = PatientContext.getActivePatient();
            
            if (patient == null) {
                pnlAssociate.setVisible(false);
            } else {
                String name = FhirUtil.formatName(patient.getName());
                Identifier mrn = FhirUtil.getMRN(patient);
                lblPatient.setLabel(name + " (" + (mrn == null ? "" : mrn.getValue()) + ")");
                chkAssociate.setData(patient.getIdElement().getIdPart());
            }
        }
        
        updateRecipients();
    }
    
    /**
     * Update the recipient text box based on the current recipient list.
     */
    private void updateRecipients() {
        StringBuilder sb = new StringBuilder();
        
        for (UserContactInfo recipient : recipients) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            
            sb.append(recipient.getName());
        }
        
        txtRecipients.setValue(sb.toString());
    }
    
    /**
     * Validate entries.
     *
     * @return True if all entries successfully validated. False otherwise.
     */
    private boolean validate() {
        if (StringUtils.trimToEmpty(txtSubject.getValue()).isEmpty()) {
            wrongValue(txtSubject, "cwfmessagebox.schedule.validate.nosubject");
        } else if (dtbDelivery.getDate() == null) {
            wrongValue(dtbDelivery, "cwfmessagebox.schedule.validate.nodate");
        } else if (recipients.isEmpty()) {
            wrongValue(txtRecipients, "cwfmessagebox.schedule.validate.norecipients");
        } else {
            return true;
        }
        
        return false;
    }
    
    /**
     * Displays a validation error next to the specified component.
     *
     * @param comp The component that failed validation.
     * @param key The key of the label to display.
     */
    private void wrongValue(BaseUIComponent comp, String key) {
        comp.setBalloon(StrUtil.getLabel(key));
    }
    
    /**
     * Allows IOC container to inject message service.
     *
     * @param service Message service.
     */
    public void setMessageService(MessageService service) {
        this.service = service;
    }
    
    /**
     * Update the scheduled message with new input values and send to the server, then close the
     * dialog if successful.
     */
    @EventHandler(value = "click", target = "btnOk")
    private void onClick$btnOK() {
        if (validate()) {
            message.setDeliveryDate(new Date(dtbDelivery.getDate().getTime()));
            message.setPatientId(chkAssociate.isChecked() ? (String) chkAssociate.getData() : null);
            message.setPatientName(chkAssociate.isChecked() ? lblPatient.getLabel() : null);
            message.setSubject(txtSubject.getValue());
            message.setUrgency((Urgency) cboUrgency.getSelectedItem().getData());
            List<String> body = StrUtil.toList(txtMessage.getValue());
            
            if (service.scheduleNotification(message, body, recipients)) {
                root.setAttribute("message", message);
                root.detach();
            } else {
                DialogUtil.showError("@cwfmessagebox.schedule.save.failure");
            }
        }
    }
    
    /**
     * Show the recipients dialog.
     */
    @EventHandler(value = "click", target = "btnRecipients")
    private void onClick$btnRecipients() {
        RecipientsController.show(recipients, (updated) -> {
            if (updated) {
                updateRecipients();
            }
        });
    }
}
