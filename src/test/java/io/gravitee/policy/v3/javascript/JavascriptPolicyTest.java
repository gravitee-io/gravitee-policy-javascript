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
package io.gravitee.policy.v3.javascript;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.gravitee.policy.javascript.JavascriptInitializer.JAVASCRIPT_ENGINE;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;
import io.gravitee.policy.javascript.JavascriptInitializer;
import io.gravitee.policy.javascript.configuration.JavascriptPolicyConfiguration;
import io.gravitee.reporter.api.http.Metrics;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.future.PromiseImpl;
import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class JavascriptPolicyTest {

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private PolicyChain policyChain;

    @Mock
    private JavascriptPolicyConfiguration configuration;

    @Mock
    private Vertx vertx;

    @RegisterExtension
    public static WireMockExtension wiremock = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    @BeforeAll
    public static void globalInit() throws Exception {
        final JavascriptInitializer javascriptInitializer = new JavascriptInitializer();
        javascriptInitializer.onActivation();
    }

    @BeforeEach
    public void setUp() throws Exception {
        lenient().when(request.metrics()).thenReturn(Metrics.on(System.currentTimeMillis()).build());

        final JavascriptInitializer javascriptInitializer = new JavascriptInitializer();
        javascriptInitializer.onActivation();

        lenient().when(executionContext.getComponent(Vertx.class)).thenReturn(vertx);

        // Make sure we execute the javascript on the current thread.
        Promise<Object> promise = new PromiseImpl<>();
        lenient()
            .doAnswer(
                i -> {
                    ((Handler<Promise<Object>>) i.getArgument(0)).handle(promise);
                    ((Handler<Promise<Object>>) i.getArgument(1)).handle(promise);
                    return null;
                }
            )
            .when(vertx)
            .executeBlocking(any(Handler.class), any(Handler.class));
    }

    @Test
    public void shouldFail_invalidScript() throws Exception {
        when(configuration.getOnRequestScript()).thenReturn(loadResource("invalid_script.js"));
        new JavascriptPolicy(configuration).onRequest(request, response, executionContext, policyChain);

        verify(policyChain, times(1)).failWith(any(io.gravitee.policy.api.PolicyResult.class));
    }

    @Test
    public void shouldIterateOverHeaders() {
        HttpHeaders requestHeaders = HttpHeaders.create();
        requestHeaders.set("A", "AValue");
        requestHeaders.set("B", "BValue");
        requestHeaders.set("C", "CValue");

        HttpHeaders responseHeaders = HttpHeaders.create();
        String script = "request.headers.forEach(function(v) { response.headers.set(v.key, v.value); });";

        when(request.headers()).thenReturn(requestHeaders);
        when(response.headers()).thenReturn(responseHeaders);
        when(configuration.getOnRequestScript()).thenReturn(script);
        new JavascriptPolicy(configuration).onRequest(request, response, executionContext, policyChain);

        requestHeaders.forEach(entry -> assertEquals(requestHeaders.get(entry.getKey()), responseHeaders.get(entry.getKey())));
        verify(policyChain, times(1)).doNext(request, response);
    }

    @Test
    public void shouldIterateOverParameters() {
        MultiValueMap<String, String> multiMap = new LinkedMultiValueMap<>();
        multiMap.add("A", "AValue");
        multiMap.add("B", "BValue");
        multiMap.add("C", "CValue");

        HttpHeaders responseHeaders = HttpHeaders.create();

        String script = "request.parameters.keySet().forEach(function(v) { response.headers.set(v, request.parameters.getFirst(v)); });";

        when(request.parameters()).thenReturn(multiMap);
        when(response.headers()).thenReturn(responseHeaders);
        when(configuration.getOnRequestScript()).thenReturn(script);
        new JavascriptPolicy(configuration).onRequest(request, response, executionContext, policyChain);

        verify(policyChain, times(1)).doNext(request, response);
    }

    /**
     * Doc example: https://docs.gravitee.io/apim_policies_javascript.html#description
     */
    @Test
    public void shouldModifyResponseHeaders() throws Exception {
        HttpHeaders headers = spy(HttpHeaders.class);
        when(response.headers()).thenReturn(headers);
        when(configuration.getOnRequestScript()).thenReturn(loadResource("modify_response_headers.js"));
        new JavascriptPolicy(configuration).onRequest(request, response, executionContext, policyChain);

        verify(headers, times(1)).remove(eq("X-Powered-By"));
        verify(headers, times(1)).set(eq("X-Gravitee-Gateway-Version"), eq("0.14.0"));
        verify(policyChain, times(1)).doNext(request, response);
    }

    /**
     * Issue: https://github.com/gravitee-io/issues/issues/2455
     */
    @Test
    public void shouldSetContextAttribute() throws Exception {
        final Map<String, Object> attributes = new HashMap<>();
        when(executionContext.getAttributes()).thenReturn(attributes);

        when(configuration.getOnRequestScript()).thenReturn(loadResource("set_context_attribute.js"));
        new JavascriptPolicy(configuration).onRequest(request, response, executionContext, policyChain);

        assertEquals(0, attributes.get("anyKey"));
        verify(policyChain, times(1)).doNext(request, response);
    }

    /**
     * Doc example: https://docs.gravitee.io/apim_policies_javascript.html#onrequest_onresponse
     * First run does not break the request.
     */
    @Test
    public void shouldNotBreakRequest() throws Exception {
        HttpHeaders headers = spy(HttpHeaders.class);
        when(request.headers()).thenReturn(headers);

        when(configuration.getOnRequestScript()).thenReturn(loadResource("break_request.js"));

        new JavascriptPolicy(configuration).onRequest(request, response, executionContext, policyChain);
        verify(headers, times(1)).set(eq("X-Javascript-Policy"), eq("ok"));
        verify(policyChain, times(1)).doNext(request, response);
    }

    /**
     * Doc example: https://docs.gravitee.io/apim_policies_javascript.html#onrequest_onresponse
     * Second run must break because of HTTP headers
     */
    @Test
    public void shouldBreakRequest() throws Exception {
        HttpHeaders headers = spy(HttpHeaders.create());
        when(request.headers()).thenReturn(headers);
        when(configuration.getOnRequestScript()).thenReturn(loadResource("break_request.js"));

        headers.set("X-Gravitee-Break", "value");

        new JavascriptPolicy(configuration).onRequest(request, response, executionContext, policyChain);
        verify(policyChain, times(1))
            .failWith(
                argThat(
                    result ->
                        result.statusCode() == HttpStatusCode.INTERNAL_SERVER_ERROR_500 &&
                        result.message().equals("Stop request processing due to X-Gravitee-Break header")
                )
            );
    }

    @Test
    public void shouldBreakRequestContent() throws Exception {
        HttpHeaders headers = spy(HttpHeaders.create());
        when(request.headers()).thenReturn(headers);
        when(configuration.getOnRequestContentScript()).thenReturn(loadResource("break_request_content.js"));

        headers.set("X-Gravitee-Break", "value");

        new JavascriptPolicy(configuration).onRequestContent(request, response, executionContext, policyChain);

        ReadWriteStream stream = new JavascriptPolicy(configuration).onRequestContent(request, response, executionContext, policyChain);
        stream.end(Buffer.buffer());

        verify(policyChain, never()).failWith(any(io.gravitee.policy.api.PolicyResult.class));
        verify(policyChain, times(1))
            .streamFailWith(
                argThat(
                    result ->
                        result.statusCode() == HttpStatusCode.INTERNAL_SERVER_ERROR_500 &&
                        result.message().equals("Stop request content processing due to X-Gravitee-Break header")
                )
            );
        verify(policyChain, never()).doNext(any(), any());
    }

    @Test
    public void shouldReadJson() throws Exception {
        HttpHeaders headers = spy(HttpHeaders.create());
        when(request.headers()).thenReturn(headers);

        when(configuration.getOnRequestContentScript()).thenReturn(loadResource("read_json.js"));
        String content = loadResource("read_json.json");

        ReadWriteStream stream = new JavascriptPolicy(configuration).onRequestContent(request, response, executionContext, policyChain);
        stream.end(Buffer.buffer(content));

        verify(policyChain, never()).failWith(any(io.gravitee.policy.api.PolicyResult.class));
        verify(policyChain, never()).streamFailWith(any(PolicyResult.class));
        verify(policyChain, never()).doNext(any(), any());
    }

    @Test
    public void javaClassNotAllowed() {
        assertThrows(
            ScriptException.class,
            () -> {
                String script = "var system = Java.type('java.lang.System')";
                JAVASCRIPT_ENGINE.eval(script);
            }
        );
    }

    @Test
    public void systemExitNotAllowed() {
        assertThrows(
            ScriptException.class,
            () -> {
                String script = "exit(1);";
                JAVASCRIPT_ENGINE.eval(script);
            }
        );
    }

    @Test
    public void evalNotAllowed() {
        assertThrows(
            ScriptException.class,
            () -> {
                String script = "eval('1 + 2')";
                JAVASCRIPT_ENGINE.eval(script);
            }
        );
    }

    @Test
    public void loadNotAllowed() {
        assertThrows(
            ScriptException.class,
            () -> {
                String script = "load('lib.js');";
                JAVASCRIPT_ENGINE.eval(script);
            }
        );
    }

    @Test
    public void loadWithNewGlobalNotAllowed() {
        assertThrows(
            ScriptException.class,
            () -> {
                String script = new StringBuilder("var script = 'var i = 0;';")
                    .append("function addition() {")
                    .append("return loadWithNewGlobal({ name: \"addition\", script: script });")
                    .append("}")
                    .append("addition();")
                    .toString();
                JAVASCRIPT_ENGINE.eval(script);
            }
        );
    }

    @Test
    public void quitNotAllowed() {
        assertThrows(
            ScriptException.class,
            () -> {
                String script = "quit();";
                JAVASCRIPT_ENGINE.eval(script);
            }
        );
    }

    @Test
    public void independentExecutions() {
        assertThrows(
            ScriptException.class,
            () -> {
                String script1 = "var a = 1";
                String script2 = "a";
                JAVASCRIPT_ENGINE.eval(script1, new SimpleScriptContext());
                JAVASCRIPT_ENGINE.eval(script2, new SimpleScriptContext());

                fail("a should be undefined and it should raise an ScriptException");
            }
        );
    }

    @Test
    public void canCreateEngine() {
        final String baseUrl = wiremock.getRuntimeInfo().getHttpBaseUrl();

        final String path = "/unreachable";
        wiremock.stubFor(get(urlEqualTo(path)).willReturn(aResponse().withStatus(200)));

        when(configuration.getOnRequestScript())
            .thenReturn(
                "this.engine.factory.scriptEngine.getFactory().getScriptEngine().eval(\"var Runtime=Java.type(\\\"java.lang.Runtime\\\"); Runtime.getRuntime().exec(\\\"curl " +
                baseUrl +
                " \\\");\");"
            );
        new JavascriptPolicy(configuration).onRequest(request, response, executionContext, policyChain);

        wiremock.verify(0, getRequestedFor(urlEqualTo(path)));
    }

    private String loadResource(String resource) throws IOException {
        InputStream stream = JavascriptPolicy.class.getResourceAsStream(resource);
        return readInputStreamToString(stream, Charset.defaultCharset());
    }

    private String readInputStreamToString(InputStream stream, Charset defaultCharset) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader(stream, defaultCharset))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                builder.append((char) c);
            }
        }

        return builder.toString();
    }
}
