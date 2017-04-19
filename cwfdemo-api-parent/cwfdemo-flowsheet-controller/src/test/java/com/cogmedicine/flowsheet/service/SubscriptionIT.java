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

package com.cogmedicine.flowsheet.service;

import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class SubscriptionIT {

    private static final String CREATE_PATIENT_CONTEXT_URL = "http://localhost:8080/connectathon/service/flowsheet/subscription/patientContext?dtid=";
    private static final String LOGIN = "http://localhost:8080/connectathon/zkau/web/org/carewebframework/security/spring/loginWindow.zul";

    class Credentials{
        String loginPaneUrl;
        String passwordPaneUrl;

        public String getLoginPaneUrl() {
            return loginPaneUrl;
        }

        public void setLoginPaneUrl(String loginPaneUrl) {
            this.loginPaneUrl = loginPaneUrl;
        }

        public String getPasswordPaneUrl() {
            return passwordPaneUrl;
        }

        public void setPasswordPaneUrl(String passwordPaneUrl) {
            this.passwordPaneUrl = passwordPaneUrl;
        }
    }

    @Test
    public void loginTest() throws IOException{
        Credentials credentials = new Credentials();
        credentials.setLoginPaneUrl("demo");
        credentials.setPasswordPaneUrl("demo");
        String response = RestUtilities.getResponse(LOGIN, credentials, RestUtilities.MethodRequest.POST);
        System.out.println(response);
    }

    @Test
    public void cannotFindDesktopIdDueToDifferentSessionTest() throws IOException{
        String desktopId = "z_a4l";
        String response = RestUtilities.getResponse(CREATE_PATIENT_CONTEXT_URL + desktopId, RestUtilities.MethodRequest.GET);
        System.out.println(response);
    }

    @Test
    public void createCookieTest() throws IOException{
        //need to log in to the browser and get the jsessionid from the cookies and the desktop id
        String myJSessionId = "B7394D35186490DE1C62E7D44BED23D1";
        String desktopId = "z_dpx";

        BasicClientCookie myCookie = new BasicClientCookie("JSESSIONID", myJSessionId);
        myCookie.setDomain("localhost");
        myCookie.setPath("/connectathon");
        myCookie.setSecure(false);
        myCookie.setAttribute("path", "/connectathon");
        myCookie.setAttribute("httponly", null);

        CookieStore cookieStore = new BasicCookieStore();
        cookieStore.addCookie(myCookie);

        HttpClientContext httpClientContext = new HttpClientContext();
        httpClientContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
        httpClientContext.setCookieStore(cookieStore);

        String response2 = RestUtilities.getResponse(CREATE_PATIENT_CONTEXT_URL + desktopId, null, RestUtilities.MethodRequest.GET, httpClientContext);
        System.out.println(response2);
    }

    @Test
    public void overwriteCookieTest() throws IOException{
        //need to log in to the browser and get the jsessionid from the cookies and the desktop id
        String myJSessionId = "B7394D35186490DE1C62E7D44BED23D1";
        String desktopId = "z_jox";

        HttpClientContext httpClientContext = new HttpClientContext();
        CookieStore cookieStore = new BasicCookieStore();
        httpClientContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

        String response = RestUtilities.getResponse("http://localhost:8080/connectathon/", null, RestUtilities.MethodRequest.GET, httpClientContext);

        List<Cookie> cookies = httpClientContext.getCookieStore().getCookies();
        for(Cookie cookie : cookies){
            if(cookie.getName().equals("JSESSIONID")) {
                BasicClientCookie cookieImpl = (BasicClientCookie)cookie;
                cookieImpl.setValue(myJSessionId);
                System.out.println(cookie.getName() + " === " + cookie.getValue());
            }
        }

        //System.out.println(response);
        String response2 = RestUtilities.getResponse(CREATE_PATIENT_CONTEXT_URL + desktopId, null, RestUtilities.MethodRequest.GET, httpClientContext);
        System.out.println(response2);
    }

    public void initWSocketConnection(final String wsocketUrl){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                WSocketClientEndPoint clientEndPoint = null;
                try {
                    URI uri = new URI(wsocketUrl);
                    clientEndPoint = new WSocketClientEndPoint(uri);
                }catch(URISyntaxException e){
                    e.printStackTrace();
                }

                while(true){
                    myWait(1);
                }
            }
        });
        thread.start();
        myWait(1);
    }

    public static void myWait(int seconds){
        try{
            Thread.sleep(seconds * 1000l);
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }
}
