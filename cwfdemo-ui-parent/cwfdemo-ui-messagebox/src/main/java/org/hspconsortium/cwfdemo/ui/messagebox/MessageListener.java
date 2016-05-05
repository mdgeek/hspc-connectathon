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

import org.hspconsortium.cwfdemo.ui.messagebox.MainController.Action;
import org.socraticgrid.hl7.services.uc.exceptions.BadBodyException;
import org.socraticgrid.hl7.services.uc.exceptions.FeatureNotSupportedException;
import org.socraticgrid.hl7.services.uc.exceptions.InvalidContentException;
import org.socraticgrid.hl7.services.uc.exceptions.InvalidMessageException;
import org.socraticgrid.hl7.services.uc.exceptions.MissingBodyTypeException;
import org.socraticgrid.hl7.services.uc.exceptions.ProcessingException;
import org.socraticgrid.hl7.services.uc.exceptions.ServiceAdapterFaultException;
import org.socraticgrid.hl7.services.uc.exceptions.UndeliverableMessageException;
import org.socraticgrid.hl7.services.uc.interfaces.UCSClientIntf;
import org.socraticgrid.hl7.services.uc.model.Conversation;
import org.socraticgrid.hl7.services.uc.model.DeliveryAddress;
import org.socraticgrid.hl7.services.uc.model.Message;
import org.socraticgrid.hl7.services.uc.model.MessageModel;

public class MessageListener implements UCSClientIntf {
    
    
    private final MainController controller;
    
    public MessageListener(MainController controller) {
        this.controller = controller;
    }
    
    @Override
    public boolean callReady(Conversation conversation, String callHandle, String serverId) {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public <T extends Message> boolean handleException(MessageModel<T> messageModel, DeliveryAddress sender,
                                                       DeliveryAddress receiver, ProcessingException exp, String serverId) {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public <T extends Message> boolean handleNotification(MessageModel<T> messageModel, String serverId) {
        controller.invokeAction(Action.ADD, messageModel.getMessageType());
        return true;
    }
    
    @Override
    public <T extends Message> MessageModel<T> handleResponse(MessageModel<T> messageModel,
                                                              String serverId) throws InvalidMessageException,
                                                                               InvalidContentException,
                                                                               MissingBodyTypeException, BadBodyException,
                                                                               ServiceAdapterFaultException,
                                                                               UndeliverableMessageException,
                                                                               FeatureNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public <T extends Message> boolean receiveMessage(MessageModel<T> messageModel, String serverId) {
        controller.invokeAction(Action.ADD, messageModel.getMessageType());
        return true;
    }
    
}
