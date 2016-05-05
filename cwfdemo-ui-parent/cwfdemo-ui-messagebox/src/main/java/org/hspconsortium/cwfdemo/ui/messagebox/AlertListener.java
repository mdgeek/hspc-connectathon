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

import java.util.List;

import org.hspconsortium.cwfdemo.ui.messagebox.MainController.Action;
import org.socraticgrid.hl7.services.uc.interfaces.UCSAlertingIntf;
import org.socraticgrid.hl7.services.uc.model.Message;
import org.socraticgrid.hl7.services.uc.model.MessageModel;

public class AlertListener implements UCSAlertingIntf {
    
    
    private final MainController controller;
    
    public AlertListener(MainController controller) {
        this.controller = controller;
    }
    
    @Override
    public <T extends Message> boolean receiveAlertMessage(MessageModel<T> messageModel, List<String> localReceivers,
                                                           String serverId) {
        controller.invokeAction(Action.ADD, messageModel.getMessageType());
        return false;
    }
    
    @Override
    public <T extends Message> boolean updateAlertMessage(MessageModel<T> newMessageModel, MessageModel<T> oldMessageModel,
                                                          List<String> localReceivers, String serverId) {
        controller.invokeAction(Action.DELETE, oldMessageModel.getMessageType());
        controller.invokeAction(Action.ADD, newMessageModel.getMessageType());
        return false;
    }
    
    @Override
    public <T extends Message> boolean cancelAlertMessage(MessageModel<T> messageModel, List<String> localReceivers,
                                                          String serverId) {
        controller.removeMessage(messageModel.getMessageType().getHeader().getMessageId(), true);
        return false;
    }
    
}
