/*-
 * #%L
 * User Profile
 * %%
 * Copyright (C) 2014 - 2017 Healthcare Services Platform Consortium
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
/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is also subject to the terms of the Health-Related Additional
 * Disclaimer of Warranty and Limitation of Liability available at
 * http://www.carewebframework.org/licensing/disclaimer.
 */
package com.cogmedsys.hsp.ui.userprofile.controller;

import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carewebframework.shell.plugins.PluginController;
import org.carewebframework.web.annotation.WiredComponent;
import org.carewebframework.web.component.BaseComponent;
import org.carewebframework.web.component.Listbox;
import org.carewebframework.web.component.Textbox;
import org.glassfish.jersey.client.ClientConfig;

/**
 * This is a sample controller that extends the PluginController class which provides some
 * convenience methods for accessing framework services and automatically registers the controller
 * with the framework so that it may receive context change events (if the controller implements a
 * supported context-related interface). This controller illustrates the use of the IPluginEvent
 * interface to receive plugin lifecycle notifications..
 */
public class UserProfileController extends PluginController {

    private static final Log log = LogFactory.getLog(UserProfileController.class);
    
    @WiredComponent
    private Listbox lbId;

    @WiredComponent
    private Textbox txEmail;

    @WiredComponent
    private Textbox txChat;

    @WiredComponent
    private Textbox txCell;
    
    private String url;

    /**
     * @see org.carewebframework.ui.FrameworkController#doAfterCompose(org.zkoss.zk.ui.Component)
     */
    @Override
    public void afterInitialized(BaseComponent comp) {
        super.afterInitialized(comp);
        log.trace("Controller composed");
        Map<String, Object> info = comp.getPage().getBrowserInfo();
        String url = (String) info.get("requestUrl");
        /*
        String port = (Executions.getCurrent().getServerPort() == 80) ? "" : (":" + Executions.getCurrent().getServerPort());
        url = Executions.getCurrent().getScheme() + "://" + Executions.getCurrent().getServerName() + port
                + Executions.getCurrent().getContextPath();
                */
    }
    
    public void onClick$btnSaveProfile() {
        String id = lbId.getSelectedItem().getLabel();
        String email = txEmail.getValue();
        String chat = txChat.getValue();
        String cell = txCell.getValue();
        Form form = new Form();
        form.param("id", id);
        form.param("contact", id + "," + email + "," + cell + "," + chat + "," + cell);
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);
        WebTarget service = client.target(url);
        Response response = service.path("service").path("registry").path("user").path(id).request()
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED), Response.class);
        System.out.println(response.getStatus());

    }
    
    public class Contact {

        String id = "1";

        String contact = "123 Roadhouse block";
    }

}
