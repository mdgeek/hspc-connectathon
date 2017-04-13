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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import javax.ws.rs.core.MediaType;
import javax.xml.ws.http.HTTPException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Rest service utilities. Generally used in the tests
 */
public class RestUtilities {
	public static final String CONTEXT_PATH = "/cohort-identification";

	public enum MethodRequest {
		POST,
		GET,
		PUT,
		DELETE
	}

	/**
	 * Get the response for a CXF REST service without an object parameter
	 * 
	 * @param url
	 * @param typeRequest
	 * @return
	 * @throws IOException
	 */
	public static String getResponse(String url, MethodRequest typeRequest) throws IOException {
		return getResponse(url, null, typeRequest);
	}

	/**
	 * Get the response for a CXF REST service with an object parameter
	 * 
	 * @param url
	 * @param parameter
	 * @param typeRequest
	 * @return
	 * @throws IOException
	 */
	public static String getResponse(String url, Object parameter, MethodRequest typeRequest) throws IOException {
		StringEntity parameterEntity = new StringEntity(new ObjectMapper().writeValueAsString(parameter));

		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response;

		switch (typeRequest) {
		case POST:
			HttpPost httppost = new HttpPost(url);
			httppost.setHeader("Content-type", MediaType.APPLICATION_JSON);
			if (parameter != null) {
				httppost.setEntity(parameterEntity);
			}

			response = httpclient.execute(httppost);
			break;
		case PUT:
			HttpPut httpPut = new HttpPut(url);
			httpPut.setHeader("Content-type", MediaType.APPLICATION_JSON);
			if (parameter != null) {
				httpPut.setEntity(parameterEntity);
			}
			response = httpclient.execute(httpPut);
			break;
		case DELETE:
			HttpDelete httpDelete = new HttpDelete(url);
			httpDelete.setHeader("Content-type", MediaType.APPLICATION_JSON);
			response = httpclient.execute(httpDelete);
			break;
		case GET:
			HttpGet httpGet = new HttpGet(url);
			httpGet.setHeader("Content-type", MediaType.APPLICATION_JSON);

			/*
			HttpClientContext httpClientContext = new HttpClientContext();
			CookieStore cookieStore = new BasicCookieStore();
			httpClientContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
			*/

			//response = httpclient.execute(httpGet, httpClientContext);
			response = httpclient.execute(httpGet);

			/*
			Object aaa = httpClientContext.getCookieStore();
			List<Cookie> cookies = httpClientContext.getCookieStore().getCookies();
			for(Cookie cookie : cookies){
				System.out.println(cookie.getName() + " === " + cookie.getValue());
			}
			*/

			System.out.println("...");
			break;
		default:
			throw new IllegalArgumentException("Cannot handle type request " + typeRequest);
		}

		if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300) {
			throw new HTTPException(response.getStatusLine().getStatusCode());
		}

		if (response.getStatusLine().getStatusCode() == 204) {
			return "";
		}

		return EntityUtils.toString(response.getEntity());
	}
}