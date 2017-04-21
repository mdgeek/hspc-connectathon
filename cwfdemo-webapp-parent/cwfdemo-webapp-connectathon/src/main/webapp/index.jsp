<%@ page import="com.cogmedicine.flowsheet.controller.FlowsheetSubscriptionControllerDstu3" %>
<%--
  ~ Copyright 2017 Cognitive Medical Systems, Inc (http://www.cognitivemedicine.com).
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~
  ~ @author Jeff Chung
  --%>

<html>
<head>
    <script type="text/javascript">
        function createWebSocket() {
            var wsEndpoint = document.getElementById('wsEndpoint').value;
            var desktopId = document.getElementById('desktopId').value;
            wsEndpoint = wsEndpoint + '?dtid=' + desktopId;

            var ws = new WebSocket(wsEndpoint);

            ws.onopen = function(){
                ws.send("hello from client");
                console.log("Sent hello message to the server");
            };

            ws.onmessage = function (evt){
                var received_msg = evt.data;
                console.log("Received message: " + received_msg);
            };

            ws.onclose = function(){
                console.log("Connection is closed.");
            };
        }
    </script>

</head>
<body>

<div id="sse">
    <table>
        <tr>
            <td>WebSocket endpoint:</td>
            <td>
                <input id="wsEndpoint" type="text" value="<%= FlowsheetSubscriptionControllerDstu3.getWebsocketUrl(request)%>"/>
            </td>
        </tr>
        <tr>
            <td>Desktop id:</td>
            <td>
                <input id="desktopId" type="text"/>
            </td>
        </tr>
        <tr>
            <td>
                <input type="button" value="Submit" onclick="createWebSocket()"/>
            </td>
        </tr>
    </table>
</div>

</body>
</html>