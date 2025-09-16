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

import io.gravitee.policy.javascript.model.js.JsHttpClient;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ScriptEvaluator {

    private final Supplier<ScriptEngine> engineProvider;

    public Maybe<@NonNull Object> evalRx(String script, ScriptContext scriptContext) {
        return Maybe.fromCallable(
            () -> {
                try {
                    String evalResult = eval(script, scriptContext);
                    return Optional.ofNullable(evalResult).orElse("");
                } catch (ScriptException | ExecutionException | InterruptedException e) {
                    throw new RuntimeException("Failed to execute JavaScript script", e);
                }
            }
        );
    }

    public @Nullable String eval(String script, ScriptContext scriptContext)
        throws ScriptException, ExecutionException, InterruptedException {
        // see https://github.com/javadelight/delight-nashorn-sandbox/issues/73
        final String blockAccessToEngine =
            "Object.defineProperty(this, 'engine', {});" + "Object.defineProperty(this, 'context', {});delete this.__noSuchProperty__;";

        Object ret = engineProvider.get().eval(blockAccessToEngine + script, scriptContext);

        final JsHttpClient httpClient = (JsHttpClient) scriptContext.getAttribute("httpClient");
        httpClient.shutDown();

        // Note: here we can do scriptContext.getWriter().toString() if we want to retrieve the printed logs but we won't display them in the gateway logs for now.
        return (ret instanceof String) ? (String) ret : null;
    }
}
