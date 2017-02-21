/*
 * #%L
 * cwf-ui-mockuments
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
package org.hspconsortium.cwfdemo.ui.mockuments;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carewebframework.ui.render.AbstractRenderer;
import org.carewebframework.web.component.Row;
import org.hspconsortium.cwf.fhir.document.Document;

/**
 * Renderer for the document list.
 */
public class DocumentListRenderer extends AbstractRenderer<Row, Document> {

    private static final Log log = LogFactory.getLog(DocumentListRenderer.class);
    
    public DocumentListRenderer() {
        super("background-color: white", null);
    }
    
    @Override
    public Row render(Document doc) {
        Row row = new Row();
        createLabel(row, doc.getDateTime());
        createLabel(row, doc.getTitle());
        createLabel(row, doc.getLocationName());
        createLabel(row, doc.getAuthorName());
        createLabel(row, doc.getStatus());
        return row;
    }
    
}
