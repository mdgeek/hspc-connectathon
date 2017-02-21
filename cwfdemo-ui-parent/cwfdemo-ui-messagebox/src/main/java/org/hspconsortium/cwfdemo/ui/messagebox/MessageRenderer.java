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

import org.carewebframework.ui.render.AbstractRenderer;
import org.carewebframework.web.component.Grid;
import org.carewebframework.web.component.Image;
import org.carewebframework.web.component.Row;
import org.carewebframework.web.event.DblclickEvent;
import org.hspconsortium.cwfdemo.api.ucs.MessageWrapper;

public class MessageRenderer extends AbstractRenderer<Row, MessageWrapper> {

    private final Grid grid;

    public MessageRenderer(Grid grid) {
        this.grid = grid;
    }

    @Override
    public Row render(MessageWrapper message) {
        Row row = new Row();
        createLabel(row, null);
        createImage(row, Constants.ICON_URGENCY_ALL[message.getUrgency().ordinal()]);
        createImage(row, message.isActionable() ? Constants.ICON_ACTIONABLE : Constants.ICON_INFO);
        createLabel(row, message.getPatientName());
        createLabel(row, message.getSender());
        createLabel(row, message.getSubject());
        createLabel(row, message.getDeliveryDate());
        //item.setDisabled(message.isProcessing());
        row.setHint(message.getDisplayText());
        row.addEventForward(DblclickEvent.TYPE, grid, "processItem");
        return row;
    }

    private Image createImage(Row row, String src) {
        Image img = new Image(src);
        row.addChild(img);
        return img;
    }
}
