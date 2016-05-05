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

import org.carewebframework.common.StrUtil;

import org.hspconsortium.cwfdemo.api.ucs.Urgency;

/**
 * Helper methods for rendering urgency.
 */
public class UrgencyRenderer {
    
    
    /**
     * Returns the path of the image resource representing the graphical representation of the
     * urgency.
     * 
     * @param urgency The urgency.
     * @return The image resource name.
     */
    public static String getIconPath(Urgency urgency) {
        return getLabel(urgency, "icon");
    }
    
    /**
     * Returns the color to be used when displaying alerts.
     * 
     * @param urgency The urgency.
     * @return A color.
     */
    public static String getColor(Urgency urgency) {
        return getLabel(urgency, "color");
    }
    
    /**
     * Returns the display name for the urgency.
     * 
     * @param urgency The urgency.
     * @return Display name
     */
    public static String getDisplayName(Urgency urgency) {
        return getLabel(urgency, "label");
    }
    
    /**
     * Returns the label property for the specified attribute name and urgency.
     * 
     * @param urgency The urgency.
     * @param name The attribute name.
     * @return The label value.
     */
    private static String getLabel(Urgency urgency, String name) {
        return StrUtil.getLabel("cwfmessagebox.urgency." + name + "." + urgency.name());
    }
    
    private UrgencyRenderer() {
    }
}
