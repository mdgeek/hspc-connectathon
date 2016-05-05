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

import org.carewebframework.ui.icons.IconUtil;
import org.carewebframework.ui.zk.ZKUtil;

import org.hspconsortium.cwfdemo.api.ucs.Urgency;

public class Constants {
    
    
    public static final String RESOURCE_PATH = ZKUtil.getResourcePath(Constants.class);
    
    public static final String BOLD = "font-weight:bold";
    
    public static final String NO_BOLD = "color:lightgray";
    
    public static final String ICON_INFO = IconUtil.getIconPath("silk:16x16:information.png");
    
    public static final String ICON_ACTIONABLE = IconUtil.getIconPath("silk:16x16:bullet_go.png");
    
    public static final String ICON_TYPE = IconUtil.getIconPath("silk:16x16:help.png");
    
    public static final String ICON_INDICATOR = IconUtil.getIconPath("silk:16x16:asterisk_orange.png");
    
    public static final String ICON_URGENCY = IconUtil.getIconPath("silk:16x16:bullet_error.png");
    
    public static final String ICON_URGENCY_HIGH = UrgencyRenderer.getIconPath(Urgency.HIGH);
    
    public static final String ICON_URGENCY_MEDIUM = UrgencyRenderer.getIconPath(Urgency.MEDIUM);
    
    public static final String ICON_URGENCY_LOW = UrgencyRenderer.getIconPath(Urgency.LOW);
    
    public static final String[] ICON_URGENCY_ALL = { ICON_URGENCY_HIGH, ICON_URGENCY_MEDIUM, ICON_URGENCY_LOW };
    
    private Constants() {
    }
}
