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

import static io.gravitee.common.http.HttpStatusCode.INTERNAL_SERVER_ERROR_500;
import static io.gravitee.policy.javascript.eval.ScriptContextBindings.RESULT_VARIABLE_NAME;
import static io.gravitee.policy.javascript.eval.ScriptContextFactory.createHttpMessageScriptContext;
import static io.gravitee.policy.javascript.eval.ScriptContextFactory.createHttpPlainScriptContext;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.http.HttpMessageExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.policy.javascript.PolicyResult.State;
import io.gravitee.policy.javascript.configuration.JavascriptPolicyConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Map;
import java.util.function.Consumer;
import javax.script.ScriptContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavascriptPolicy extends io.gravitee.policy.v3.javascript.JavascriptPolicy implements HttpPolicy {

    private static final Logger logger = LoggerFactory.getLogger(JavascriptPolicy.class);

    /**
     * @see JavascriptPolicyConfiguration#getScripts()
     */
    private final Flowable<String> scriptFlowable;

    public JavascriptPolicy(JavascriptPolicyConfiguration configuration) {
        super(configuration);
        scriptFlowable = Flowable.fromIterable(configuration.getScripts());
    }

    @Override
    public String id() {
        return "policy-javascript";
    }

    @Override
    public Completable onRequest(final HttpPlainExecutionContext ctx) {
        if (
            isBlank(configuration.getScript()) &&
            isBlank(configuration.getOnRequestScript()) &&
            isBlank(configuration.getOnRequestContentScript())
        ) {
            return Completable.complete();
        }

        if (
            (isNotBlank(configuration.getScript()) && configuration.isReadContent()) ||
            isNotBlank(configuration.getOnRequestContentScript())
        ) {
            Consumer<Buffer> onContentOverride = overridenContentBuffer -> ctx.request().contentLength(overridenContentBuffer.length());
            return ctx
                .request()
                .onBody(requestBodyBuffer ->
                    requestBodyBuffer
                        .defaultIfEmpty(Buffer.buffer())
                        .flatMapMaybe(buffer ->
                            onHttpContent(ctx, buffer, createHttpPlainScriptContext(ctx, buffer, null), onContentOverride)
                        )
                );
        }
        if (isNotBlank(configuration.getScript())) {
            return runScript(ctx, configuration.getScript());
        }
        return runScript(ctx, configuration.getOnRequestScript());
    }

    @Override
    public Completable onResponse(final HttpPlainExecutionContext ctx) {
        if (
            isBlank(configuration.getScript()) &&
            isBlank(configuration.getOnResponseScript()) &&
            isBlank(configuration.getOnResponseContentScript())
        ) {
            return Completable.complete();
        }

        if (
            (isNotBlank(configuration.getScript()) && configuration.isReadContent()) ||
            isNotBlank(configuration.getOnResponseContentScript())
        ) {
            Consumer<Buffer> onContentOverride = overridenContentBuffer -> ctx.response().contentLength(overridenContentBuffer.length());
            return ctx
                .response()
                .onBody(responseBodyBuffer ->
                    responseBodyBuffer
                        .defaultIfEmpty(Buffer.buffer())
                        .flatMapMaybe(buffer ->
                            onHttpContent(ctx, buffer, createHttpPlainScriptContext(ctx, null, buffer), onContentOverride)
                        )
                );
        }
        if (isNotBlank(configuration.getScript())) {
            return runScript(ctx, configuration.getScript());
        }
        return runScript(ctx, configuration.getOnResponseScript());
    }

    @Override
    public Completable onMessageRequest(final HttpMessageExecutionContext ctx) {
        return ctx.request().onMessage(message -> runScript(ctx, message));
    }

    @Override
    public Completable onMessageResponse(final HttpMessageExecutionContext ctx) {
        return ctx.response().onMessage(message -> runScript(ctx, message));
    }

    private Completable runScript(final HttpPlainExecutionContext ctx, String script) {
        var scriptContext = createHttpPlainScriptContext(ctx);

        return scriptEvaluator
            .evalRx(script, scriptContext)
            .ignoreElement()
            .onErrorResumeNext(e -> {
                logger.error("An error occurred while executing Javascript script", e);
                return ctx.interruptWith(createExecutionFailureFromThrowable(e));
            })
            .andThen(
                Completable.defer(() -> {
                    var result = (PolicyResult) scriptContext.getAttribute(RESULT_VARIABLE_NAME);
                    return handleResult(ctx, result);
                })
            );
    }

    private static Completable handleResult(HttpPlainExecutionContext ctx, PolicyResult result) {
        if (result.getState() == State.FAILURE) {
            return ctx.interruptWith(
                new ExecutionFailure(result.getCode()).key(result.getKey()).message(result.getError()).contentType(result.getContentType())
            );
        }

        return Completable.complete();
    }

    private static ExecutionFailure createExecutionFailureFromThrowable(Throwable e) {
        return new ExecutionFailure(INTERNAL_SERVER_ERROR_500)
            .key("JAVASCRIPT_EXECUTION_FAILURE")
            .parameters(Map.of("exception", e))
            .message("Internal Server Error");
    }

    private Maybe<Buffer> onHttpContent(
        HttpPlainExecutionContext ctx,
        Buffer bodyBuffer,
        ScriptContext scriptContext,
        Consumer<Buffer> onContentOverride
    ) {
        return scriptFlowable
            .concatMapMaybe(script -> runContentAwareScript(ctx, scriptContext, script))
            .lastElement()
            .filter(jsBuffer -> configuration.isOverrideContent())
            .doOnSuccess(onContentOverride::accept)
            .switchIfEmpty(Maybe.just(bodyBuffer));
    }

    private Maybe<Buffer> runContentAwareScript(HttpPlainExecutionContext ctx, ScriptContext scriptContext, String script) {
        return scriptEvaluator
            .evalRx(script, scriptContext)
            .onErrorResumeNext(e -> {
                logger.error("An error occurred while executing Javascript script", e);
                return ctx.interruptBodyWith(createExecutionFailureFromThrowable(e));
            })
            .flatMap(output -> {
                var result = (PolicyResult) scriptContext.getAttribute(RESULT_VARIABLE_NAME);
                return handleResult(ctx, output, result);
            });
    }

    private static Maybe<Buffer> handleResult(HttpPlainExecutionContext ctx, Object output, PolicyResult result) {
        if (result.getState() == State.FAILURE) {
            return ctx.interruptBodyWith(
                new ExecutionFailure(result.getCode()).key(result.getKey()).message(result.getError()).contentType(result.getContentType())
            );
        }

        return Maybe.just(Buffer.buffer(output.toString()));
    }

    private Maybe<Message> runScript(final HttpMessageExecutionContext ctx, Message message) {
        var script = configuration.getScript();
        var scriptContext = createHttpMessageScriptContext(ctx, message);

        return scriptEvaluator
            .evalRx(script, scriptContext)
            .onErrorResumeNext(e -> ctx.interruptMessageWith(createExecutionFailureFromThrowable(e)))
            .flatMap(output -> {
                var result = (PolicyResult) scriptContext.getAttribute(RESULT_VARIABLE_NAME);
                return handleResult(ctx, message, output, result);
            });
    }

    private Maybe<Message> handleResult(HttpMessageExecutionContext ctx, Message message, Object output, PolicyResult result) {
        if (result.getState() == State.FAILURE) {
            return ctx.interruptMessageWith(
                new ExecutionFailure(result.getCode()).key(result.getKey()).message(result.getError()).contentType(result.getContentType())
            );
        }

        if (configuration.isOverrideContent()) {
            message.content(Buffer.buffer(output.toString()));
        }

        return Maybe.just(message);
    }
}
