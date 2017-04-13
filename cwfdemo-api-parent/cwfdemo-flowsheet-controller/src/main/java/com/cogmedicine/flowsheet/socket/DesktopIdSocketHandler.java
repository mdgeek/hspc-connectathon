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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class DesktopIdSocketHandler extends TextWebSocketHandler {

    private static final Log log = LogFactory.getLog(DesktopIdSocketHandler.class);
    private static final Map<String, WebSocketSession> socketSessions = new HashMap<>();

    /**
     * Stores the socket session with the desktop id
     * @param session
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        //requires the DesktopIdSocketInterceptor to add the dtid request parameter into the websocket session attributes
        Object desktopIdObject = session.getAttributes().get("dtid");
        if(desktopIdObject == null) {
            String message = "Websocket url must contain the dtid parameter";
            try {
                session.sendMessage(new TextMessage(message));
            }catch(IOException e){
                e.printStackTrace();
            } finally {
                try {
                    session.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            log.info(message);
        } else {
            String desktopId = (String) desktopIdObject;
            //check to see if the socket already exists for that desktop id
            if (socketSessions.containsKey(desktopId)) {
                WebSocketSession mappedSession = socketSessions.get(desktopId);
                if (mappedSession.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage("Session already exists"));
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to send message to the client with desktopId: " + desktopId + ". " + e.getMessage());
                    }
                } else {
                    log.info("Recreating websocket session for desktop id " + desktopId);
                    socketSessions.remove(desktopId);
                    socketSessions.put(desktopId, session);
                }
            } else {
                log.info("Creating websocket session for desktop id " + desktopId);
                socketSessions.put(desktopId, session);
            }
        }
    }

    /**
     * If a session is closed, remove it from the socket sessions map
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        log.info("Connection closed");
        for (String desktopId : socketSessions.keySet()) {
            if (socketSessions.get(desktopId).getId().equals(session.getId())) {
                log.info("Removed session for desktop id " + desktopId);
                socketSessions.remove(desktopId);
            }
        }
    }

    /**
     * Listen for client messages.  Currently not used
     * @param session
     * @param wrappedMessage
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage wrappedMessage) {
        String message = wrappedMessage.getPayload();
        log.info("Received message: " + message);

        try {
            session.sendMessage(new TextMessage("message received"));
        } catch (IOException e) {
            throw new RuntimeException("Unable to send message to the client. " + e.getMessage());
        }
    }

    /**
     * Closes the socket session
     * @param desktopId
     */
    public static void clearSession(String desktopId){
        if(socketSessions.containsKey(desktopId)) {
            WebSocketSession socketSession = socketSessions.get(desktopId);
            if (socketSession.isOpen()) {
                try {
                    socketSession.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to close session with desktop id: " + desktopId);
                }
            }

            socketSessions.remove(desktopId);
        }else{
            throw new RuntimeException("Cannot find desktop id: " + desktopId);
        }
    }

    /**
     * Gets the socket session by desktop id
     * @param desktopId
     * @return
     */
    public static WebSocketSession getSocketSession(String desktopId) {
        return socketSessions.get(desktopId);
    }

    /**
     * Sends a message to the client
     * @param desktopId
     * @param message
     */
    public static void sendMessage(String desktopId, String message) {
        WebSocketSession socketSession = DesktopIdSocketHandler.getSocketSession(desktopId);
        if (socketSession != null) {
            try {
                socketSession.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                throw new RuntimeException("Unable to send the message to the client. " + e.getMessage());
            }
        }
    }
}