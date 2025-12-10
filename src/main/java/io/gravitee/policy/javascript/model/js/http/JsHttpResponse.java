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
package io.gravitee.policy.javascript.model.js.http;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseResponse;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainResponse;
import io.gravitee.policy.javascript.model.js.JsHttpHeaders;
import lombok.Getter;

public class JsHttpResponse implements HttpBaseResponse {

    @Getter
    private final HttpPlainResponse response;

    private final String content;

    public JsHttpResponse(HttpPlainResponse response, Buffer buffer) {
        this.response = response;
        this.content = buffer != null ? buffer.toString() : null;
    }

    public JsHttpResponse(HttpPlainResponse response) {
        this(response, null);
    }

    public String getContent() {
        if (content == null) {
            throw new UnsupportedOperationException("Accessing response content must be enabled in the policy configuration");
        }
        return content;
    }

    @Override
    public HttpBaseResponse status(int httpStatusCode) {
        return response.status(httpStatusCode);
    }

    @Override
    public int status() {
        return response.status();
    }

    public int getStatus() {
        return status();
    }

    @Override
    public String reason() {
        return response.reason();
    }

    public String getReason() {
        return reason();
    }

    @Override
    public HttpBaseResponse reason(String reason) {
        return response.reason(reason);
    }

    @Override
    public HttpHeaders headers() {
        return response.headers();
    }

    public JsHttpHeaders getHeaders() {
        return new JsHttpHeaders(headers());
    }

    @Override
    public HttpHeaders trailers() {
        return response.trailers();
    }

    @Override
    public boolean ended() {
        return response.ended();
    }
}
