/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.javascript;

import static io.gravitee.policy.javascript.JavascriptInitializer.HTTP_CLIENT;
import static io.gravitee.policy.javascript.JavascriptInitializer.JAVASCRIPT_ENGINE;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.stream.TransformableRequestStreamBuilder;
import io.gravitee.gateway.api.http.stream.TransformableResponseStreamBuilder;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.api.stream.exception.TransformationException;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponse;
import io.gravitee.policy.api.annotations.OnResponseContent;
import io.gravitee.policy.javascript.configuration.JavascriptPolicyConfiguration;
import io.gravitee.policy.javascript.model.js.JsClientRequest;
import io.gravitee.policy.javascript.model.js.JsExecutionContext;
import io.gravitee.policy.javascript.model.js.JsHttpClient;
import io.gravitee.policy.javascript.model.js.JsRequest;
import io.gravitee.policy.javascript.model.js.JsResponse;
import io.vertx.core.Vertx;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;
import javax.script.*;
import jdk.dynalink.beans.StaticClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JavascriptPolicy {

    private static final Logger logger = LoggerFactory.getLogger(JavascriptPolicy.class);

    private static final String REQUEST_VARIABLE_NAME = "request";
    private static final String RESPONSE_VARIABLE_NAME = "response";
    private static final String CONTEXT_VARIABLE_NAME = "context";
    private static final String RESULT_VARIABLE_NAME = "result";
    private static final String HTTP_CLIENT_VARIABLE_NAME = "httpClient";
    private static final String REQUEST_CLASS_VARIABLE_NAME = "Request";
    private static final String STATE_CLASS_VARIABLE_NAME = "State";

    private final JavascriptPolicyConfiguration configuration;

    public JavascriptPolicy(JavascriptPolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    @OnRequest
    public void onRequest(Request request, Response response, ExecutionContext executionContext, PolicyChain policyChain) {
        executeScript(request, response, executionContext, policyChain, configuration.getOnRequestScript());
    }

    @OnResponse
    public void onResponse(Request request, Response response, ExecutionContext executionContext, PolicyChain policyChain) {
        executeScript(request, response, executionContext, policyChain, configuration.getOnResponseScript());
    }

    @OnResponseContent
    public ReadWriteStream onResponseContent(
        Request request,
        Response response,
        ExecutionContext executionContext,
        PolicyChain policyChain
    ) {
        String script = configuration.getOnResponseContentScript();

        if (script != null && !script.trim().isEmpty()) {
            return TransformableResponseStreamBuilder
                .on(response)
                .chain(policyChain)
                .transform(
                    buffer -> {
                        try {
                            final String content = executeStreamScript(
                                new JsRequest(request, null),
                                new JsResponse(response, buffer.toString()),
                                new JsExecutionContext(executionContext),
                                script
                            );
                            return Buffer.buffer(content);
                        } catch (PolicyFailureException ex) {
                            if (ex.getResult().getContentType() != null) {
                                policyChain.streamFailWith(
                                    io.gravitee.policy.api.PolicyResult.failure(
                                        ex.getResult().getCode(),
                                        ex.getResult().getError(),
                                        ex.getResult().getContentType()
                                    )
                                );
                            } else {
                                policyChain.streamFailWith(
                                    io.gravitee.policy.api.PolicyResult.failure(ex.getResult().getCode(), ex.getResult().getError())
                                );
                            }
                        } catch (Throwable t) {
                            logger.error("Unable to run Javascript script", t);
                            throw new TransformationException("Unable to run Javascript script: " + t.getMessage(), t);
                        }
                        return null;
                    }
                )
                .build();
        }

        return null;
    }

    @OnRequestContent
    public ReadWriteStream onRequestContent(
        Request request,
        Response response,
        ExecutionContext executionContext,
        PolicyChain policyChain
    ) {
        String script = configuration.getOnRequestContentScript();

        if (script != null && !script.trim().isEmpty()) {
            return TransformableRequestStreamBuilder
                .on(request)
                .chain(policyChain)
                .transform(
                    buffer -> {
                        try {
                            final String content = executeStreamScript(
                                new JsRequest(request, buffer.toString()),
                                new JsResponse(response, null),
                                new JsExecutionContext(executionContext),
                                script
                            );

                            return Buffer.buffer(content);
                        } catch (PolicyFailureException ex) {
                            if (ex.getResult().getContentType() != null) {
                                policyChain.streamFailWith(
                                    io.gravitee.policy.api.PolicyResult.failure(
                                        ex.getResult().getCode(),
                                        ex.getResult().getError(),
                                        ex.getResult().getContentType()
                                    )
                                );
                            } else {
                                policyChain.streamFailWith(
                                    io.gravitee.policy.api.PolicyResult.failure(ex.getResult().getCode(), ex.getResult().getError())
                                );
                            }
                        } catch (Throwable t) {
                            logger.error("Unable to run Javascript script", t);
                            throw new TransformationException("Unable to run Javascript script: " + t.getMessage(), t);
                        }
                        return null;
                    }
                )
                .build();
        }

        return null;
    }

    private String executeScript(
        Request request,
        Response response,
        ExecutionContext executionContext,
        PolicyChain policyChain,
        String script
    ) {
        if (script == null || script.trim().isEmpty()) {
            policyChain.doNext(request, response);
        } else {
            // Prepare binding
            final ScriptContext scriptContext = createScriptContext(
                new JsRequest(request, null),
                new JsResponse(response, null),
                new JsExecutionContext(executionContext)
            );

            executionContext
                .getComponent(Vertx.class)
                .executeBlocking(
                    event -> {
                        try {
                            eval(script, scriptContext);
                            event.complete();
                        } catch (Exception e) {
                            event.fail(e);
                        }
                    },
                    event -> {
                        if (event.failed()) {
                            logger.error("Unable to run Javascript script", event.cause());
                            policyChain.failWith(io.gravitee.policy.api.PolicyResult.failure(event.cause().getMessage()));
                        } else {
                            PolicyResult result = (PolicyResult) scriptContext.getAttribute(RESULT_VARIABLE_NAME);

                            if (result.getState() == PolicyResult.State.SUCCESS) {
                                policyChain.doNext(request, response);
                            } else {
                                if (result.getContentType() != null) {
                                    policyChain.failWith(
                                        io.gravitee.policy.api.PolicyResult.failure(
                                            result.getCode(),
                                            result.getError(),
                                            result.getContentType()
                                        )
                                    );
                                } else {
                                    policyChain.failWith(io.gravitee.policy.api.PolicyResult.failure(result.getCode(), result.getError()));
                                }
                            }
                        }
                    }
                );
        }

        return null;
    }

    private String executeStreamScript(JsRequest request, JsResponse response, JsExecutionContext executionContext, String script)
        throws PolicyFailureException {
        // Prepare binding
        final ScriptContext scriptContext = createScriptContext(request, response, executionContext);
        final PolicyResult result = (PolicyResult) scriptContext.getAttribute(RESULT_VARIABLE_NAME);

        String content;

        // And run script
        try {
            content = eval(script, scriptContext);

            if (result.getState() == PolicyResult.State.FAILURE) {
                throw new PolicyFailureException(result);
            }
        } catch (Exception e) {
            result.setState(PolicyResult.State.FAILURE);
            result.setError(e.getMessage());
            throw new PolicyFailureException(result);
        }

        return content;
    }

    private String eval(String script, ScriptContext scriptContext) throws ScriptException, ExecutionException, InterruptedException {
        String content = (String) JAVASCRIPT_ENGINE.eval(script, scriptContext);
        final JsHttpClient httpClient = (JsHttpClient) scriptContext.getAttribute("httpClient");
        httpClient.shutDown();

        // Note: here we can do scriptContext.getWriter().toString() if we want to retrieve the printed logs but we won't display them in the gateway logs for now.
        return content;
    }

    private static class PolicyFailureException extends Exception {

        private final PolicyResult result;

        PolicyFailureException(PolicyResult result) {
            this.result = result;
        }

        public PolicyResult getResult() {
            return result;
        }
    }

    private ScriptContext createScriptContext(JsRequest request, JsResponse response, JsExecutionContext executionContext) {
        // Prepare binding
        Bindings bindings = new SimpleBindings();
        bindings.put(REQUEST_VARIABLE_NAME, request);
        bindings.put(RESPONSE_VARIABLE_NAME, response);
        bindings.put(CONTEXT_VARIABLE_NAME, executionContext);
        bindings.put(RESULT_VARIABLE_NAME, new PolicyResult());

        bindings.put(STATE_CLASS_VARIABLE_NAME, StaticClass.forClass(PolicyResult.State.class));
        bindings.put(REQUEST_CLASS_VARIABLE_NAME, StaticClass.forClass(JsClientRequest.class));
        bindings.put(HTTP_CLIENT_VARIABLE_NAME, new JsHttpClient(HTTP_CLIENT));

        final SimpleScriptContext scriptContext = new SimpleScriptContext();
        final StringWriter printWriter = new StringWriter();
        final StringWriter errorWriter = new StringWriter();

        scriptContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        scriptContext.setWriter(printWriter);
        scriptContext.setErrorWriter(errorWriter);

        return scriptContext;
    }
}
