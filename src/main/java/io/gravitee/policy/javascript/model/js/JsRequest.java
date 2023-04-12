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

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.reporter.api.http.Metrics;
import javax.net.ssl.SSLSession;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JsRequest {

    private final Request request;
    private final String content;

    public JsRequest(Request request, String content) {
        this.request = request;
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public String id() {
        return request.id();
    }

    public String getId() {
        return this.id();
    }

    public String transactionId() {
        return request.transactionId();
    }

    public String getTransactionId() {
        return this.transactionId();
    }

    public String uri() {
        return request.uri();
    }

    public String getUri() {
        return this.uri();
    }

    public String path() {
        return request.path();
    }

    public String getPath() {
        return this.path();
    }

    public String pathInfo() {
        return request.pathInfo();
    }

    public String getPathInfo() {
        return this.pathInfo();
    }

    public String contextPath() {
        return request.contextPath();
    }

    public String getContextPath() {
        return this.contextPath();
    }

    public MultiValueMap<String, String> parameters() {
        return request.parameters();
    }

    public MultiValueMap<String, String> pathParameters() {
        return request.pathParameters();
    }

    public MultiValueMap<String, String> getParameters() {
        return this.parameters();
    }

    public HttpHeaders headers() {
        return request.headers();
    }

    public HttpHeaders getHeaders() {
        return this.headers();
    }

    public HttpMethod method() {
        return request.method();
    }

    public HttpMethod getMethod() {
        return this.method();
    }

    public HttpVersion version() {
        return request.version();
    }

    public HttpVersion getVersion() {
        return this.version();
    }

    public long timestamp() {
        return request.timestamp();
    }

    public long getTimestamp() {
        return this.timestamp();
    }

    public String remoteAddress() {
        return request.remoteAddress();
    }

    public String getRemoteAddress() {
        return this.remoteAddress();
    }

    public String localAddress() {
        return request.localAddress();
    }

    public String getLocalAddress() {
        return this.localAddress();
    }

    public String scheme() {
        return request.scheme();
    }

    public String getScheme() {
        return this.scheme();
    }

    public SSLSession sslSession() {
        return request.sslSession();
    }

    public SSLSession getSslSession() {
        return this.sslSession();
    }

    public Metrics metrics() {
        return request.metrics();
    }
}
