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
package io.gravitee.policy.javascript.model.js;

import java.util.concurrent.CompletableFuture;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JsHttpExchange {

    private CompletableFuture<JsClientResponse> future;

    private JsClientResponse response;

    private Throwable error;

    public JsHttpExchange(CompletableFuture<JsClientResponse> future) {
        this.future = future;
    }

    public void waitForComplete() throws Exception {
        try {
            response = future.get();
        } catch (Exception e) {
            error = e;
        }
    }

    public JsClientResponse getResponse() {
        return response;
    }

    public JsClientResponse response() {
        return response;
    }

    public boolean isComplete() {
        return future.isDone() && !future.isCancelled();
    }

    public boolean isError() {
        return future.isCompletedExceptionally();
    }

    public boolean isSuccess() {
        return isComplete() && !isError();
    }
}
