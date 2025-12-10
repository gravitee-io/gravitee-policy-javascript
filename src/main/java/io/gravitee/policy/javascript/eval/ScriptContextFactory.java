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
package io.gravitee.policy.javascript.eval;

import static io.gravitee.policy.javascript.JavascriptInitializer.HTTP_CLIENT;
import static io.gravitee.policy.javascript.JavascriptInitializer.JAVASCRIPT_ENGINE;
import static io.gravitee.policy.javascript.eval.ScriptContextBindings.*;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.reactive.api.context.http.HttpMessageExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.policy.javascript.PolicyResult;
import io.gravitee.policy.javascript.model.js.JsClientRequest;
import io.gravitee.policy.javascript.model.js.JsHttpBaseExecutionContext;
import io.gravitee.policy.javascript.model.js.JsHttpClient;
import io.gravitee.policy.javascript.model.js.http.JsHttpRequest;
import io.gravitee.policy.javascript.model.js.http.JsHttpResponse;
import io.gravitee.policy.javascript.model.js.message.JsMessage;
import io.gravitee.policy.v3.javascript.model.JsContentAwareRequest;
import io.gravitee.policy.v3.javascript.model.JsContentAwareResponse;
import io.gravitee.policy.v3.javascript.model.JsExecutionContext;
import java.io.StringWriter;
import java.util.function.Consumer;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;
import jdk.dynalink.beans.StaticClass;

public class ScriptContextFactory {

    public static ScriptContext createHttpPlainScriptContext(final HttpPlainExecutionContext ctx) {
        return createCommonScriptContext(new JsHttpBaseExecutionContext(ctx), bindings -> {
            bindings.put(REQUEST_VARIABLE_NAME, new JsHttpRequest(ctx.request()));
            bindings.put(RESPONSE_VARIABLE_NAME, new JsHttpResponse(ctx.response()));
        });
    }

    public static ScriptContext createHttpPlainScriptContext(
        final HttpPlainExecutionContext ctx,
        final Buffer requestContent,
        final Buffer responseContent
    ) {
        return createCommonScriptContext(new JsHttpBaseExecutionContext(ctx), bindings -> {
            bindings.put(REQUEST_VARIABLE_NAME, new JsHttpRequest(ctx.request(), requestContent));
            bindings.put(RESPONSE_VARIABLE_NAME, new JsHttpResponse(ctx.response(), responseContent));
        });
    }

    public static ScriptContext createHttpMessageScriptContext(final HttpMessageExecutionContext ctx, final Message message) {
        return createCommonScriptContext(new JsHttpBaseExecutionContext(ctx), bindings -> {
            bindings.put(MESSAGE_VARIABLE_NAME, new JsMessage(message));
        });
    }

    public static ScriptContext createContentAwareScriptContext(
        JsContentAwareRequest request,
        JsContentAwareResponse response,
        JsExecutionContext executionContext
    ) {
        return createCommonScriptContext(executionContext, bindings -> {
            bindings.put(REQUEST_VARIABLE_NAME, request);
            bindings.put(RESPONSE_VARIABLE_NAME, response);
        });
    }

    private static <T> ScriptContext createCommonScriptContext(final T ctx, Consumer<Bindings> extraBindingsConfigurer) {
        Bindings bindings = JAVASCRIPT_ENGINE.createBindings();
        sanitizeBindings(bindings);
        bindings.put(CONTEXT_VARIABLE_NAME, ctx);
        bindings.put(RESULT_VARIABLE_NAME, new PolicyResult());

        bindings.put(STATE_CLASS_VARIABLE_NAME, StaticClass.forClass(PolicyResult.State.class));
        bindings.put(REQUEST_CLASS_VARIABLE_NAME, StaticClass.forClass(JsClientRequest.class));
        bindings.put(HTTP_CLIENT_VARIABLE_NAME, new JsHttpClient(HTTP_CLIENT));

        extraBindingsConfigurer.accept(bindings);

        final SimpleScriptContext scriptContext = new SimpleScriptContext();
        final StringWriter printWriter = new StringWriter();
        final StringWriter errorWriter = new StringWriter();

        scriptContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        scriptContext.setWriter(printWriter);
        scriptContext.setErrorWriter(errorWriter);

        return scriptContext;
    }

    protected static void sanitizeBindings(Bindings bindings) {
        bindings.remove("quit");
        bindings.remove("exit");
        bindings.remove("print");
        bindings.remove("echo");
        bindings.remove("readFully");
        bindings.remove("readLine");
        bindings.remove("load");
        bindings.remove("loadWithNewGlobal");
    }
}
