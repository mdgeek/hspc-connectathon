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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.carewebframework.api.context.ISurveyResponse;
import org.carewebframework.api.domain.DomainFactoryRegistry;
import org.carewebframework.api.spring.SpringUtil;
import org.carewebframework.common.StrUtil;
import org.carewebframework.ui.FrameworkController;
import org.carewebframework.ui.dialog.DialogUtil;
import org.carewebframework.ui.util.CWFUtil;
import org.carewebframework.web.annotation.EventHandler;
import org.carewebframework.web.component.BaseComponent;
import org.carewebframework.web.component.Window;
import org.carewebframework.web.event.IEventListener;
import org.hl7.fhir.dstu3.model.Patient;
import org.hspconsortium.cwf.api.patient.PatientContext;
import org.hspconsortium.cwf.api.patient.PatientContext.IPatientContextEvent;
import org.hspconsortium.cwfdemo.api.ucs.MessageWrapper;
import org.hspconsortium.cwfdemo.ui.messagebox.ViewerController.Action;
import org.hspconsortium.cwfdemo.ui.messagebox.ViewerController.ActionEvent;

/**
 * Controller for processing messages.
 */
public class ProcessingController extends FrameworkController implements IPatientContextEvent {

    private static final String DIALOG = CWFUtil.getResourcePath(ProcessingController.class) + "processing.cwf";
    
    private Iterator<MessageWrapper> iterator;
    
    private boolean requestingContextChange;
    
    private int currentIndex;
    
    private int total;
    
    private Action viewerAction;
    
    private ViewerController viewer;
    
    private MainController mainController;

    private Window window;
    
    /**
     * Listens and responds to action events originating from the viewer dialog.
     */
    private final IEventListener actionListener = (evt) -> {
        ActionEvent event = (ActionEvent) evt;
        viewerAction = event.getAction();
        processAction(viewerAction, event.getMessage());
    };
    
    /**
     * Creates an amodal instance of the processing dialog.
     *
     * @param mainController The requesting controller.
     * @return The controller associated with the newly created dialog.
     */
    protected static ProcessingController create(MainController mainController) {
        Window dlg = DialogUtil.popup(DIALOG, false, false, false);
        ProcessingController controller = (ProcessingController) FrameworkController.getController(dlg);
        controller.mainController = mainController;
        return controller;
    }
    
    /**
     * Creates a message viewer instance for use by this controller.
     */
    @Override
    public void afterInitialized(BaseComponent comp) {
        super.afterInitialized(comp);
        window = (Window) comp;
        viewer = ViewerController.create(actionListener);
    }
    
    /**
     * Process the next message.
     */
    @EventHandler(value = "click", target = "btnNext")
    private void onClick$btnNext() {
        doNext();
    }
    
    /**
     * Cancel all processing.
     */
    @EventHandler(value = "click", target = "btnStop")
    private void onClick$btnStop() {
        cancelProcessing();
    }
    
    /**
     * Closes (hides) the processing dialog.
     */
    private void close() {
        root.setVisible(false);
        iterator = null;
        mainController.setProcessing(false);
    }
    
    /**
     * Returns true if message processing is underway.
     *
     * @return True if message processing is underway.
     */
    private boolean isProcessing() {
        return iterator != null;
    }
    
    /**
     * Cancel all processing.
     */
    public void cancelProcessing() {
        close();
        viewer.onAction(null);
        mainController.highlightMessage(null);
    }
    
    /**
     * Process the specified messages.
     *
     * @param messages The messages to process.
     */
    public void process(Collection<MessageWrapper> messages) {
        List<MessageWrapper> lst = new ArrayList<>(messages);
        currentIndex = 0;
        total = lst.size();
        iterator = lst.iterator();
        viewerAction = null;
        mainController.setProcessing(true);
        doNext();
    }
    
    /**
     * Handle a message action.
     *
     * @param action The action specified by the user.
     * @param message The message to be acted upon.
     */
    private void processAction(Action action, MessageWrapper message) {
        switch (action) {
            case SKIP:
            case SKIP_ALL:
                break;
            
            case DELETE:
            case DELETE_ALL:
                mainController.removeMessage(message.getId(), false);
                break;
            
            case CANCEL:
                close();
                break;
            
            case VIEW:
                changePatient(message);
                return;
        }
        
        doNext();
    }
    
    /**
     * Process the next message. If there are no more messages, cancel processing. If this is the
     * last message, hide this dialog before processing the message.
     */
    private void doNext() {
        if (iterator == null || !iterator.hasNext()) {
            cancelProcessing();
            return;
        }
        
        MessageWrapper message = iterator.next();
        window.setTitle(StrUtil.getLabel("cwfmessagebox.processing.caption", ++currentIndex, total));
        window.setHint(message.getDisplayText());
        
        if (!iterator.hasNext()) {
            close();
        } else {
            root.setVisible(true);
        }
        
        process(message);
    }
    
    /**
     * Process a single message. If the message is actionable, an event of the appropriate type is
     * fired. If the event is information-only, it is displayed in the message viewer.
     *
     * @param message The message to process.
     */
    private void process(MessageWrapper message) {
        mainController.highlightMessage(message);
        
        if (!message.isActionable()) {
            if (viewerAction == Action.SKIP_ALL || viewerAction == Action.DELETE_ALL) {
                processAction(viewerAction, message);
            } else {
                viewer.process(message, null);
            }
        } else {
            String eventName = "NOTIFY." + message.getType();
            
            if (getEventManager().hasSubscribers(eventName)) {
                viewer.onAction(null);
                String service = message.getParam("SRV");
                
                if (service != null) {
                    SpringUtil.getBean(service);
                }
                
                if (changePatient(message)) {
                    getEventManager().fireLocalEvent(eventName, message);
                }
            } else {
                viewer.process(message, StrUtil.getLabel("cwfmessagebox.processing.nohandler", message.getType()));
            }
        }
    }
    
    /**
     * Changes the patient context to the patient associated with the message, if any.
     *
     * @param message A message.
     * @return False if a context change was requested and rejected. Otherwise, true.
     */
    private boolean changePatient(MessageWrapper message) {
        if (message.hasPatient()) {
            Patient patient = DomainFactoryRegistry.fetchObject(Patient.class, message.getPatientId());
            
            try {
                requestingContextChange = true;
                PatientContext.changePatient(patient);
            } finally {
                requestingContextChange = false;
            }
            return PatientContext.getActivePatient() == patient;
        }
        return true;
    }
    
    /**
     * Disallow a patient context change while actively processing messages, unless the context
     * change request originated from this controller.
     */
    @Override
    public void pending(ISurveyResponse response) {
        if (!requestingContextChange && !response.isSilent() && isProcessing()) {
            DialogUtil.confirm("@cwfmessagebox.processing.cancel.confirm.prompt", (confirm) -> {
                if (confirm) {
                    response.reject(StrUtil.formatMessage("@cwfmessagebox.processing.cancel.rejected.message"));
                } else {
                    response.accept();
                }
            });
            response.defer();
        } else {
            response.accept();
        }
    }
    
    /**
     * Cancel processing when the patient context changes, unless the context change request
     * originated from this controller.
     */
    @Override
    public void committed() {
        if (!requestingContextChange) {
            cancelProcessing();
        }
    }
    
    @Override
    public void canceled() {
    }
}
