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

import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class SubscriptionIT {

    private static final String CREATE_PATIENT_CONTEXT_URL = "http://localhost:8080/connectathon/service/flowsheet/subscription/patientContext?dtid=";

    @Test
    public void case1() throws IOException{
        String desktopId = "z_h580";
        String response = RestUtilities.getResponse(CREATE_PATIENT_CONTEXT_URL + desktopId, RestUtilities.MethodRequest.GET);
        System.out.println(response);
    }

    @Test
    public void case2() throws IOException{
        String desktopId = "z_h580";
        String response = RestUtilities.getResponse("http://localhost:8080/connectathon/", RestUtilities.MethodRequest.GET);
        System.out.println(response);
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
