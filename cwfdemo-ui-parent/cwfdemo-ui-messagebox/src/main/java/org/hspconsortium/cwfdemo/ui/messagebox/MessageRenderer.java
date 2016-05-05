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

import org.carewebframework.ui.zk.AbstractListitemRenderer;

import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Listitem;

import org.hspconsortium.cwf.api.ucs.MessageWrapper;

public class MessageRenderer extends AbstractListitemRenderer<MessageWrapper, Object> {
    
    
    @Override
    protected void renderItem(Listitem item, MessageWrapper message) {
        createCell(item, null);
        createCell(item, null).setImage(Constants.ICON_URGENCY_ALL[message.getUrgency().ordinal()]);
        createCell(item, null).setImage(message.isActionable() ? Constants.ICON_ACTIONABLE : Constants.ICON_INFO);
        createCell(item, message.getPatientName());
        createCell(item, message.getSender());
        createCell(item, message.getSubject());
        createCell(item, message.getDeliveryDate());
        //item.setDisabled(message.isProcessing());
        item.setTooltiptext(message.getDisplayText());
        item.addForward(Events.ON_DOUBLE_CLICK, item.getListbox(), "onProcessItem");
    }
}
