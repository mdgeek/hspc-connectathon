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

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class BaseQuestionnaireHandler implements IQuestionnaireHandler {
    
    
    protected interface IResponseProcessor {
        
        
        void processResponse(String value, String target);
    }
    
    private final String questionnaireId;
    
    BaseQuestionnaireHandler(String questionnaireId) {
        this.questionnaireId = questionnaireId;
    }
    
    @Override
    public String getId() {
        return questionnaireId;
    }
    
    protected void processResponses(org.w3c.dom.Document responses, IResponseProcessor processor) {
        NodeList nodeList = responses.getElementsByTagName("response");
        
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node response = nodeList.item(i);
            NamedNodeMap attr = response.getAttributes();
            String value = attr.getNamedItem("value").getNodeValue();
            String target = attr.getNamedItem("target").getNodeValue();
            processor.processResponse(value, target);
        }
    }
    
}
