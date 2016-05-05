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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import org.carewebframework.common.StrUtil;
import org.carewebframework.ui.FrameworkController;
import org.carewebframework.ui.zk.DateTimebox;
import org.carewebframework.ui.zk.ListUtil;
import org.carewebframework.ui.zk.PopupDialog;
import org.carewebframework.ui.zk.PromptDialog;
import org.carewebframework.ui.zk.ZKUtil;

import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hspconsortium.cwf.api.patient.PatientContext;
import org.hspconsortium.cwf.api.ucs.MessageService;
import org.hspconsortium.cwf.api.ucs.ScheduledMessage;
import org.hspconsortium.cwf.api.ucs.Urgency;
import org.hspconsortium.cwf.fhir.common.FhirUtil;
import org.socraticgrid.hl7.services.uc.model.UserContactInfo;

/**
 * Controller for creating or editing a scheduled message.
 */
public class ScheduleController extends FrameworkController {
    
    
    private static final long serialVersionUID = 1L;
    
    private static final String DIALOG = ZKUtil.getResourcePath(ScheduleController.class) + "schedule.zul";
    
    private MessageService service;
    
    private ScheduledMessage message;
    
    private List<UserContactInfo> recipients = new ArrayList<>();
    
    private DateTimebox dtbDelivery;
    
    private Combobox cboUrgency;
    
    private Textbox txtSubject;
    
    private Textbox txtMessage;
    
    private Textbox txtRecipients;
    
    private Checkbox chkAssociate;
    
    private Label lblPatient;
    
    private Component pnlAssociate;
    
    /**
     * Display the scheduled message dialog modally.
     *
     * @param message The scheduled message to be modified. Specify null to create a new scheduled
     *            message.
     * @return The modified or new scheduled message, or null if the dialog was cancelled.
     */
    public static ScheduledMessage show(ScheduledMessage message) {
        Map<Object, Object> args = new HashMap<>();
        args.put("message", message);
        Window dlg = PopupDialog.popup(DIALOG, args, true, false, true);
        return (ScheduledMessage) dlg.getAttribute("message");
    }
    
    /**
     * Initialize the dialog.
     */
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        message = (ScheduledMessage) arg.get("message");
        
        for (Urgency urgency : Urgency.values()) {
            Comboitem item = new Comboitem(UrgencyRenderer.getDisplayName(urgency), UrgencyRenderer.getIconPath(urgency));
            item.setValue(urgency);
            cboUrgency.appendChild(item);
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
        dtbDelivery.setConstraint("no past");
        ListUtil.selectComboboxData(cboUrgency, message.getUrgency());
        
        txtSubject.setValue(message.getSubject());
        txtMessage.setValue(message.getBody());
        
        if (message.hasPatient()) {
            lblPatient.setValue(message.getPatientName());
            chkAssociate.setChecked(true);
            chkAssociate.setValue(message.getPatientId());
        } else {
            Patient patient = PatientContext.getActivePatient();
            
            if (patient == null) {
                pnlAssociate.setVisible(false);
            } else {
                String name = FhirUtil.formatName(patient.getName());
                Identifier mrn = FhirUtil.getMRN(patient);
                lblPatient.setValue(name + " (" + (mrn == null ? "" : mrn.getValue()) + ")");
                chkAssociate.setValue(patient.getIdElement().getIdPart());
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
        
        txtRecipients.setText(sb.toString());
    }
    
    /**
     * Validate entries.
     *
     * @return True if all entries successfully validated. False otherwise.
     */
    private boolean validate() {
        if (StringUtils.trimToEmpty(txtSubject.getText()).isEmpty()) {
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
    private void wrongValue(Component comp, String key) {
        Clients.wrongValue(comp, Labels.getLabel(key));
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
    public void onClick$btnOK() {
        if (validate()) {
            message.setDeliveryDate(new Date(dtbDelivery.getDate().getTime()));
            message.setPatientId(chkAssociate.isChecked() ? (String) chkAssociate.getValue() : null);
            message.setPatientName(chkAssociate.isChecked() ? lblPatient.getValue() : null);
            message.setSubject(txtSubject.getValue());
            message.setUrgency((Urgency) cboUrgency.getSelectedItem().getValue());
            List<String> body = StrUtil.toList(txtMessage.getText());
            
            if (service.scheduleNotification(message, body, recipients)) {
                root.setAttribute("message", message);
                root.detach();
            } else {
                PromptDialog.showError("@cwfmessagebox.schedule.save.failure");
            }
        }
    }
    
    /**
     * Show the recipients dialog.
     */
    public void onClick$btnRecipients() {
        if (RecipientsController.show(recipients)) {
            updateRecipients();
        }
    }
}
