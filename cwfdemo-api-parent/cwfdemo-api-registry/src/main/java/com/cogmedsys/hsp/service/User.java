/*-
 * #%L
 * Registry API
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
package com.cogmedsys.hsp.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/")
public class User {

    private final static Map<String, String> registry = new HashMap<String, String>();
    
    static {
        registry.put("eafry", "eafry,eafry@cognitivemedicine.com,5035507932,eafry@cognitivemedicine.com,5035507932");
        registry.put("4", "Practitioner/4,user-1@some.email.com,5035507932,user-1,5035507932");
        registry.put("PractitionerId-4", "Practitioner/PractitionerId-4,user-1@some.email.com,5035507932,user-1,5035507932");
        registry.put("PatientId-1234", "Patient/PatientId-1234,user-1@some.email.com,5035507932,user-1,5035507932");
        registry.put("PatientId-1235", "Patient/PatientId-1235,user-1@some.email.com,5035507932,user-1,5035507932");
    }

    public User() {
    }

    /**
     * Method processing HTTP GET requests, producing "text/plain" MIME media
     * type.
     *
     * @return String that will be send back as a response of type "text/plain".
     */
    @GET
    @Path("/user/{id}")
    @Produces("text/plain")
    public String getContact(@PathParam("id") String id) {
        return registry.get(id);
    }
    
    @POST
    @Path("/user/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String addContact(@FormParam("id") String id,
        @FormParam("contact") String contact,
        @Context HttpServletResponse servletResponse) throws IOException {
        registry.put(id, contact);
        return contact;
    }
    
    @GET
    @Path("/user/Practitioner/{id}")
    @Produces("text/plain")
    public String getPractitionerContact(@PathParam("id") String id) {
        return registry.get(id);
    }
    
    @POST
    @Path("/user/Practitioner/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String addPractitionerContact(@FormParam("id") String id,
        @FormParam("contact") String contact,
        @Context HttpServletResponse servletResponse) throws IOException {
        registry.put(id, contact);
        return contact;
    }
    
    
    @GET
    @Path("/user/Patient/{id}")
    @Produces("text/plain")
    public String getPatientContact(@PathParam("id") String id) {
        return registry.get(id);
    }
    
    @POST
    @Path("/user/Patient/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String addPatientContact(@FormParam("id") String id,
        @FormParam("contact") String contact,
        @Context HttpServletResponse servletResponse) throws IOException {
        registry.put(id, contact);
        return contact;
    }
}
