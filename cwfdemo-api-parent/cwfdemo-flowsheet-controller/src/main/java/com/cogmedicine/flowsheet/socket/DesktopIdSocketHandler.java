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
import com.cogmedicine.flowsheet.util.Utilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.servlet.http.HttpSession;
import java.io.IOException;

public class DesktopIdSocketHandler extends TextWebSocketHandler {

    private static final Log log = LogFactory.getLog(DesktopIdSocketHandler.class);

    /**
     * Stores the socket session with the desktop id
     *
     * @param session
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        saveWebSocketSession(session);
    }

    /**
     * If a session is closed, remove it from the socket sessions map
     *
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession socketSession, CloseStatus status) throws Exception {
        HttpSession httpSession = getHttpSession(socketSession);
        httpSession.setAttribute(FlowsheetSessionListener.WEB_SOCKET_SESSION, null);
    }

    /**
     * Listen for client messages.  Currently not used
     *
     * @param session
     * @param wrappedMessage
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage wrappedMessage) {
        String message = wrappedMessage.getPayload();
        String response = "hello from server";
        log.info("Received message: " + message);

        sendMessage(session, response);
    }

    /**
     * Gets for the desktop id parameter and adds the websocket session into the http session
     *
     * @param socketSession
     */
    public void saveWebSocketSession(WebSocketSession socketSession) {
        HttpSession httpSession = getHttpSession(socketSession);
        Object desktopIdObject = httpSession.getAttribute(FlowsheetSessionListener.DESKTOP_ID);

        if (desktopIdObject == null) {
            String host = Utilities.getParameter(FlowsheetSessionListener.HOST, httpSession, String.class);
            String port = Utilities.getParameter(FlowsheetSessionListener.PORT, httpSession, String.class);
            String servicePath = "/service/flowsheet/subscription/patientContext";
            String parameter = "?dtid={your-desktop-id}";
            String url = "http://" + host + ":" + port + httpSession.getServletContext().getContextPath() + servicePath + parameter;
            String message = "Register a desktop id first. Call: " + url;
            log.info(message);

            try {
                sendMessage(socketSession, message);
            } finally {
                try {
                    socketSession.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Object storedSocketSession = httpSession.getAttribute(FlowsheetSessionListener.WEB_SOCKET_SESSION);

            if (storedSocketSession != null) {
                String message = "Web socket session already exists";
                log.info(message);
                sendMessage(socketSession, message);
                throw new IllegalArgumentException(message);
            } else {
                httpSession.setAttribute(FlowsheetSessionListener.WEB_SOCKET_SESSION, socketSession);
            }
        }
    }

    /**
     * Sends a message to the client
     *
     * @param socketSession
     * @param message
     */
    public static void sendMessage(WebSocketSession socketSession, String message) {
        if (socketSession != null) {
            try {
                socketSession.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                throw new RuntimeException("Unable to send the message to the client. " + e.getMessage());
            }
        }
    }

    /**
     * Get the current http session
     *
     * @param socketSession
     * @return
     */
    public HttpSession getHttpSession(WebSocketSession socketSession) {
        Object httpSessionObject = socketSession.getAttributes().get("httpSession");
        return (HttpSession) httpSessionObject;
    }
}