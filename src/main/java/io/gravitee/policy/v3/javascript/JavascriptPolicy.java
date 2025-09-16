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

import static io.gravitee.policy.javascript.JavascriptInitializer.JAVASCRIPT_ENGINE;
import static io.gravitee.policy.javascript.eval.ScriptContextBindings.RESULT_VARIABLE_NAME;
import static io.gravitee.policy.javascript.eval.ScriptContextFactory.createContentAwareScriptContext;

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
import io.gravitee.policy.javascript.PolicyResult;
import io.gravitee.policy.javascript.configuration.JavascriptPolicyConfiguration;
import io.gravitee.policy.javascript.eval.ScriptEvaluator;
import io.gravitee.policy.v3.javascript.model.JsContentAwareRequest;
import io.gravitee.policy.v3.javascript.model.JsContentAwareResponse;
import io.gravitee.policy.v3.javascript.model.JsExecutionContext;
import io.vertx.core.Vertx;
import javax.script.ScriptContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JavascriptPolicy {

    private static final Logger logger = LoggerFactory.getLogger(JavascriptPolicy.class);

    protected final JavascriptPolicyConfiguration configuration;
    protected final ScriptEvaluator scriptEvaluator;

    public JavascriptPolicy(JavascriptPolicyConfiguration configuration) {
        this.configuration = configuration;
        this.scriptEvaluator = new ScriptEvaluator(() -> JAVASCRIPT_ENGINE);
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
                                new JsContentAwareRequest(request, null),
                                new JsContentAwareResponse(response, buffer.toString()),
                                new JsExecutionContext(executionContext),
                                script
                            );
                            return Buffer.buffer(content);
                        } catch (PolicyFailureException ex) {
                            if (ex.getResult().getContentType() != null) {
                                policyChain.streamFailWith(
                                    io.gravitee.policy.api.PolicyResult.failure(
                                        ex.getResult().getKey(),
                                        ex.getResult().getCode(),
                                        ex.getResult().getError(),
                                        ex.getResult().getContentType()
                                    )
                                );
                            } else {
                                policyChain.streamFailWith(
                                    io.gravitee.policy.api.PolicyResult.failure(
                                        ex.getResult().getKey(),
                                        ex.getResult().getCode(),
                                        ex.getResult().getError()
                                    )
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
                                new JsContentAwareRequest(request, buffer.toString()),
                                new JsContentAwareResponse(response, null),
                                new JsExecutionContext(executionContext),
                                script
                            );

                            return Buffer.buffer(content);
                        } catch (PolicyFailureException ex) {
                            if (ex.getResult().getContentType() != null) {
                                policyChain.streamFailWith(
                                    io.gravitee.policy.api.PolicyResult.failure(
                                        ex.getResult().getKey(),
                                        ex.getResult().getCode(),
                                        ex.getResult().getError(),
                                        ex.getResult().getContentType()
                                    )
                                );
                            } else {
                                policyChain.streamFailWith(
                                    io.gravitee.policy.api.PolicyResult.failure(
                                        ex.getResult().getKey(),
                                        ex.getResult().getCode(),
                                        ex.getResult().getError()
                                    )
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
            final ScriptContext scriptContext = createContentAwareScriptContext(
                new JsContentAwareRequest(request, null),
                new JsContentAwareResponse(response, null),
                new JsExecutionContext(executionContext)
            );

            executionContext
                .getComponent(Vertx.class)
                .executeBlocking(
                    event -> {
                        try {
                            scriptEvaluator.eval(script, scriptContext);
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
                                            result.getKey(),
                                            result.getCode(),
                                            result.getError(),
                                            result.getContentType()
                                        )
                                    );
                                } else {
                                    policyChain.failWith(
                                        io.gravitee.policy.api.PolicyResult.failure(result.getKey(), result.getCode(), result.getError())
                                    );
                                }
                            }
                        }
                    }
                );
        }

        return null;
    }

    private String executeStreamScript(
        JsContentAwareRequest request,
        JsContentAwareResponse response,
        JsExecutionContext executionContext,
        String script
    ) throws PolicyFailureException {
        // Prepare binding
        final ScriptContext scriptContext = createContentAwareScriptContext(request, response, executionContext);
        final PolicyResult result = (PolicyResult) scriptContext.getAttribute(RESULT_VARIABLE_NAME);

        String content;

        // And run script
        try {
            content = scriptEvaluator.eval(script, scriptContext);
        } catch (Exception e) {
            result.setState(PolicyResult.State.FAILURE);
            result.setError(e.getMessage());
            throw new PolicyFailureException(result);
        }

        if (result.getState() == PolicyResult.State.FAILURE) {
            throw new PolicyFailureException(result);
        }

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
}
