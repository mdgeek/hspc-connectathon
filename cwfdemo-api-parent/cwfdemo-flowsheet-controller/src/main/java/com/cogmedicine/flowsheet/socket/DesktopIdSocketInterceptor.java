/*
 * Copyright 2017 Cognitive Medical Systems, Inc (http://www.cognitivemedicine.com).
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Jeff Chung
 */

package com.cogmedicine.flowsheet.socket;

import com.cogmedicine.flowsheet.listener.FlowsheetSessionListener;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * Stores the desktop id request parameter into the websocket session attributes
 */
public class DesktopIdSocketInterceptor extends HttpSessionHandshakeInterceptor {

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        ServletServerHttpRequest ssreq = (ServletServerHttpRequest) request;
        HttpServletRequest req = ssreq.getServletRequest();

        HttpSession httpSession = req.getSession();
        attributes.put("httpSession", httpSession);

        String dtid = req.getParameter("dtid");
        if (dtid != null) {
            attributes.put("dtid", dtid);
        }

        httpSession.setAttribute(FlowsheetSessionListener.HOST, req.getServerName());
        httpSession.setAttribute(FlowsheetSessionListener.PORT, req.getServerPort() + "");

        return super.beforeHandshake(request, response, wsHandler, attributes);
    }
}
