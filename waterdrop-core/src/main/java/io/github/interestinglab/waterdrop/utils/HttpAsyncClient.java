/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package io.github.interestinglab.waterdrop.utils;
import org.apache.http.Consts;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Lookup;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.auth.*;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;

import javax.net.ssl.SSLContext;
import java.nio.charset.CodingErrorAction;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

/**
 * ?????????HTTP??????????????????????????????
 */
public class HttpAsyncClient {

	private static int socketTimeout = 5000;// ??????????????????????????????5?????? ??????????????????

	private static int connectTimeout = 2000;// ????????????

	private static int poolSize = 3000;// ????????????????????????

	private static int maxPerRoute = 1500;// ?????????????????????????????????1500?????????????????????????????????????????????3000

	// http??????????????????
	private String host = "baidu.com";
	private int port = 0;

	// ??????httpclient
	private CloseableHttpAsyncClient asyncHttpClient;


	public HttpAsyncClient() {
		try {
			this.asyncHttpClient = createAsyncClient(false);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public CloseableHttpAsyncClient createAsyncClient(boolean proxy)
			throws KeyManagementException, UnrecoverableKeyException,
			NoSuchAlgorithmException, KeyStoreException,
            MalformedChallengeException, IOReactorException {

		SSLContext sslcontext = SSLContexts.createDefault();

//		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
//				username, password);

		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//		credentialsProvider.setCredentials(AuthScope.ANY, credentials);

		// ????????????http???https???????????????socket?????????????????????
		Registry<SchemeIOSessionStrategy> sessionStrategyRegistry = RegistryBuilder
				.<SchemeIOSessionStrategy> create()
				.register("http", NoopIOSessionStrategy.INSTANCE)
				.register("https", new SSLIOSessionStrategy(sslcontext))
				.build();

		// ??????io??????
		IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
				.setIoThreadCount(Runtime.getRuntime().availableProcessors())
				.build();
		// ?????????????????????
		ConnectingIOReactor ioReactor;
		ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
		PoolingNHttpClientConnectionManager conMgr = new PoolingNHttpClientConnectionManager(
				ioReactor, null, sessionStrategyRegistry, null);

		if (poolSize > 0) {
			conMgr.setMaxTotal(poolSize);
		}

        //??????????????????????????????
		if (maxPerRoute > 0) {
			conMgr.setDefaultMaxPerRoute(maxPerRoute);
		} else {
			conMgr.setDefaultMaxPerRoute(10);
		}

        //??????????????????
		ConnectionConfig connectionConfig = ConnectionConfig.custom()
				.setMalformedInputAction(CodingErrorAction.IGNORE)
				.setUnmappableInputAction(CodingErrorAction.IGNORE)
				.setCharset(Consts.UTF_8).build();

		//??????????????????
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(connectTimeout)
				.setSocketTimeout(socketTimeout).build();


		Lookup<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder
				.<AuthSchemeProvider> create()
				.register(AuthSchemes.BASIC, new BasicSchemeFactory())
				.register(AuthSchemes.DIGEST, new DigestSchemeFactory())
				.register(AuthSchemes.NTLM, new NTLMSchemeFactory())
				.register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory())
				.register(AuthSchemes.KERBEROS, new KerberosSchemeFactory())
				.build();
		conMgr.setDefaultConnectionConfig(connectionConfig);

		if (proxy) {
			return HttpAsyncClients.custom().setConnectionManager(conMgr)
					.setDefaultCredentialsProvider(credentialsProvider)
					.setDefaultAuthSchemeRegistry(authSchemeRegistry)
					.setProxy(new HttpHost(host, port))
					.setDefaultCookieStore(new BasicCookieStore())
					.setDefaultRequestConfig(requestConfig).build();
		} else {
			return HttpAsyncClients.custom().setConnectionManager(conMgr)
					.setDefaultCredentialsProvider(credentialsProvider)
					.setDefaultAuthSchemeRegistry(authSchemeRegistry)
					.setDefaultCookieStore(new BasicCookieStore()).build();
		}

	}

	public CloseableHttpAsyncClient getAsyncHttpClient() {
		return asyncHttpClient;
	}

	//public CloseableHttpAsyncClient getProxyAsyncHttpClient() { return proxyAsyncHttpClient; }
}

