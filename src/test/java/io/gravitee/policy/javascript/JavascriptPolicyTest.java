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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.gravitee.policy.javascript.JavascriptInitializer.JAVASCRIPT_ENGINE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
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
import io.gravitee.policy.javascript.configuration.JavascriptPolicyConfiguration;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.future.PromiseImpl;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

    public static WireMockServer wiremock;

    @BeforeAll
    public static void globalInit() throws Exception {
        final JavascriptInitializer javascriptInitializer = new JavascriptInitializer();
        javascriptInitializer.onActivation();
        wiremock = new WireMockServer(wireMockConfig().dynamicPort());
        wiremock.start();
    }

    @BeforeEach
    public void init() throws Exception {
        final JavascriptInitializer javascriptInitializer = new JavascriptInitializer();
        javascriptInitializer.onActivation();
        //        when(executionContext.getComponent(Vertx.class)).thenReturn(vertx);

    }

    @Nested
    class EvaluationFailures {

        @Test
        public void javaClassNotAllowed() {
            String script = "var system = Java.type('java.lang.System')";
            assertThatThrownBy(() -> JAVASCRIPT_ENGINE.eval(script)).isInstanceOf(ScriptException.class);
        }

        @Test
        public void systemExitNotAllowed() {
            String script = "exit(1);";
            assertThatThrownBy(() -> JAVASCRIPT_ENGINE.eval(script)).isInstanceOf(ScriptException.class);
        }

        @Test
        public void evalNotAllowed() {
            String script = "eval('1 + 2')";
            assertThatThrownBy(() -> JAVASCRIPT_ENGINE.eval(script)).isInstanceOf(ScriptException.class);
        }

        @Test
        public void loadNotAllowed() {
            String script = "load('lib.js');";
            assertThatThrownBy(() -> JAVASCRIPT_ENGINE.eval(script)).isInstanceOf(ScriptException.class);
        }

        @Test
        public void loadWithNewGlobalNotAllowed() {
            String script = new StringBuilder("var script = 'var i = 0;';")
                .append("function addition() {")
                .append("return loadWithNewGlobal({ name: \"addition\", script: script });")
                .append("}")
                .append("addition();")
                .toString();
            assertThatThrownBy(() -> JAVASCRIPT_ENGINE.eval(script)).isInstanceOf(ScriptException.class);
        }

        @Test
        public void quitNotAllowed() {
            String script = "quit();";
            assertThatThrownBy(() -> JAVASCRIPT_ENGINE.eval(script)).isInstanceOf(ScriptException.class);
        }

        @Test
        public void independentExecutions() throws javax.script.ScriptException {
            String script1 = "var a = 1";
            String script2 = "a";
            JAVASCRIPT_ENGINE.eval(script1, new SimpleScriptContext());
            assertThatThrownBy(() -> JAVASCRIPT_ENGINE.eval(script2)).isInstanceOf(ScriptException.class);
        }
    }

    @Nested
    class OnRequest {

        @BeforeEach
        void beforeEach() {
            //when(request.metrics()).thenReturn(Metrics.on(System.currentTimeMillis()).build());
            when(executionContext.getComponent(Vertx.class)).thenReturn(vertx);
            // Make sure we execute the javascript on the current thread.
            Promise<Object> promise = new PromiseImpl<>();
            doAnswer(
                    i -> {
                        ((Handler<Promise<Object>>) i.getArgument(0)).handle(promise);
                        ((Handler<Promise<Object>>) i.getArgument(1)).handle(promise);
                        return null;
                    }
                )
                .when(vertx)
                .executeBlocking(any(Handler.class), any(Handler.class));
        }

        @Nested
        class Failures {

            @Test
            public void shouldFail_invalidScript() throws Exception {
                when(configuration.getOnRequestScript()).thenReturn(loadResource("invalid_script.js"));
                new JavascriptPolicy(configuration).onRequest(request, response, executionContext, policyChain);

                verify(policyChain, times(1)).failWith(any(io.gravitee.policy.api.PolicyResult.class));
            }
        }

        @Nested
        class Success {

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

                requestHeaders.forEach(entry -> assertThat(entry.getValue()).isEqualTo(responseHeaders.get(entry.getKey())));
                verify(policyChain, times(1)).doNext(request, response);
            }

            @Test
            public void shouldIterateOverParameters() {
                MultiValueMap<String, String> multiMap = new LinkedMultiValueMap<>();
                multiMap.add("A", "AValue");
                multiMap.add("B", "BValue");
                multiMap.add("C", "CValue");

                HttpHeaders responseHeaders = HttpHeaders.create();

                String script =
                    "request.parameters.keySet().forEach(function(v) { response.headers.set(v, request.parameters.getFirst(v)); });";

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
                HttpHeaders headers = spy(HttpHeaders.create());
                when(response.headers()).thenReturn(headers);
                when(configuration.getOnRequestScript()).thenReturn(loadResource("modify_response_headers.js"));
                new JavascriptPolicy(configuration).onRequest(request, response, executionContext, policyChain);

                verify(headers, times(1)).remove("X-Powered-By");
                verify(headers, times(1)).set("X-Gravitee-Gateway-Version", "0.14.0");
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

                assertThat(attributes.get("anyKey")).isEqualTo(0);
                verify(policyChain, times(1)).doNext(request, response);
            }

            /**
             * Doc example: https://docs.gravitee.io/apim_policies_javascript.html#onrequest_onresponse
             * First run does not break the request.
             */
            @Test
            public void shouldNotBreakRequest() throws Exception {
                HttpHeaders headers = HttpHeaders.create();
                when(request.headers()).thenReturn(headers);

                when(configuration.getOnRequestScript()).thenReturn(loadResource("break_request.js"));

                new JavascriptPolicy(configuration).onRequest(request, response, executionContext, policyChain);
                verify(policyChain, times(1)).doNext(request, response);
                assertThat(headers.containsKey("X-Groovy-Policy")).isTrue();
                assertThat(headers.get("X-Groovy-Policy")).isEqualTo("ok");
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
        }
    }

    @Nested
    class OnRequestContent {

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
