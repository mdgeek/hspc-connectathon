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

import com.cogmedicine.flowsheet.bean.DesktopSession;
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
import java.util.Map;

public class DesktopIdSocketHandler extends TextWebSocketHandler {

    private static final Log log = LogFactory.getLog(DesktopIdSocketHandler.class);

    /**
     * Stores the socket session with the desktop id
     *
     * @param socketSession
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession socketSession) {
        String desktopId = getDesktopId(socketSession);
        DesktopSession desktopSession = getDesktopSession(socketSession, desktopId);

        saveWebSocketSession(socketSession, desktopSession);
    }

    /**
     * If a session is closed, remove it from the socket sessions map
     *
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession socketSession, CloseStatus status) throws Exception {

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
    public void saveWebSocketSession(WebSocketSession socketSession, DesktopSession desktopSession) {
        WebSocketSession storedSocketSession = desktopSession.getWebsocketSession();

        if (storedSocketSession != null) {
            String message = "Web socket session already exists";
            log.info(message);
            sendMessage(socketSession, message);
            //commented out so it will not close the existing websocket connection
            //throw new IllegalArgumentException(message);
        } else {
            desktopSession.setWebsocketSession(socketSession);
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

    public static void sendMessageAndClose(WebSocketSession socketSession, String message) {
        if (socketSession != null) {
            try {
                socketSession.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                throw new RuntimeException("Unable to send the message to the client. " + e.getMessage());
            } finally {
                try {
                    socketSession.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

    public String getDesktopId(WebSocketSession socketSession) {
        Object desktopIdObject = socketSession.getAttributes().get("dtid");
        if (desktopIdObject == null) {
            String message = "Websocket url must contain the dtid parameter";
            log.info(message);

            sendMessageAndClose(socketSession, message);
            throw new IllegalArgumentException(message);
        }
        return (String) desktopIdObject;
    }

    public DesktopSession getDesktopSession(WebSocketSession socketSession, String desktopId) {
        HttpSession httpSession = getHttpSession(socketSession);
        Map<String, DesktopSession> desktopSessionMap = Utilities.getParameter(FlowsheetSessionListener.DESKTOP_SESSION_MAP, httpSession, Map.class);

        //if (desktopSessionMap.containsKey(desktopId)) {
        //    return desktopSessionMap.get(desktopId);

        DesktopSession desktopSession = desktopSessionMap.get(desktopId);
        if(desktopSession != null){
            return desktopSession;
        } else {
            String message = FlowsheetSessionListener.getNoDesktopIdMessage(httpSession);
            log.info(message);

            sendMessageAndClose(socketSession, message);
            throw new IllegalArgumentException(message);
        }
    }
}