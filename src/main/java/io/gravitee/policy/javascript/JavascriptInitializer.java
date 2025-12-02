/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.javascript;

import io.gravitee.policy.api.PolicyContext;
import io.gravitee.policy.api.PolicyContextProvider;
import io.gravitee.policy.api.PolicyContextProviderAware;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JavascriptInitializer implements PolicyContext, PolicyContextProviderAware {

    public static HttpClient HTTP_CLIENT;
    public static ScriptEngine JAVASCRIPT_ENGINE;
    private static Boolean initialized = false;
    private static Vertx vertx;

    @Override
    public void onActivation() throws Exception {
        initJavascriptEngine();
    }

    @Override
    public void onDeactivation() throws Exception {}

    @Override
    public void setPolicyContextProvider(PolicyContextProvider policyContextProvider) {
        setContext(policyContextProvider);
    }

    private static synchronized void setContext(PolicyContextProvider policyContextProvider) {
        if (!initialized) {
            vertx = policyContextProvider.getComponent(Vertx.class);
        }
    }

    private static synchronized void initJavascriptEngine() {
        if (!initialized) {
            System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
            System.setProperty("polyglot.js.nashorn-compat", "true");
            System.setProperty("polyglot.js.strict", "true");
            System.setProperty("polyglot.js.ecmascript-version", "latest");
            JAVASCRIPT_ENGINE = new ScriptEngineManager().getEngineByName("js");
            final Bindings bindings = JAVASCRIPT_ENGINE.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.put("polyglot.js.allowAllAccess", false);
            bindings.remove("load");
            bindings.remove("loadWithNewGlobal");
            bindings.remove("exit");
            bindings.remove("eval");
            bindings.remove("quit");

            initHttpClient();
            initialized = true;
        }
    }

    /**
     * @deprecated we should remove the use of httpclient inside javascript and exclusively rely on EndpointCalloutPolicy or dynamically add an EndpointInvoker in the script context.
     */
    private static void initHttpClient() {
        if (vertx != null) {
            final HttpClientOptions options = new HttpClientOptions()
                .setTrustAll(true)
                .setVerifyHost(false)
                .setMaxPoolSize(30)
                .setKeepAlive(false)
                .setTcpKeepAlive(false)
                .setConnectTimeout(3000);

            // TODO: check how to manage the proxy options.
            //        if ((useSystemProxy != null && useSystemProxy == Boolean.TRUE) || (useSystemProxy == null && this.isProxyConfigured)) {
            //            ProxyOptions proxyOptions = new ProxyOptions();
            //            proxyOptions.setType(ProxyType.valueOf(httpClientProxyType));
            //            if (HTTPS_SCHEME.equals(uriScheme)) {
            //                proxyOptions.setHost(httpClientProxyHttpsHost);
            //                proxyOptions.setPort(httpClientProxyHttpsPort);
            //                proxyOptions.setUsername(httpClientProxyHttpsUsername);
            //                proxyOptions.setPassword(httpClientProxyHttpsPassword);
            //            } else {
            //                proxyOptions.setHost(httpClientProxyHttpHost);
            //                proxyOptions.setPort(httpClientProxyHttpPort);
            //                proxyOptions.setUsername(httpClientProxyHttpUsername);
            //                proxyOptions.setPassword(httpClientProxyHttpPassword);
            //            }
            //            options.setProxyOptions(proxyOptions);
            //        }

            HTTP_CLIENT = vertx.createHttpClient(options);
        }
    }
}
