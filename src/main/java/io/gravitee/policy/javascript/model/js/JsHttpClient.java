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
package io.gravitee.policy.javascript.model.js;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.impl.ConcurrentHashSet;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JsHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(JsHttpClient.class);

    private final HttpClient httpClient;
    private final Set<CompletableFuture<JsClientResponse>> futures;

    public JsHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.futures = new ConcurrentHashSet<>();
    }

    public JsHttpExchange get(String url) {
        return get(url, null);
    }

    public JsHttpExchange get(String url, BiConsumer<Object, Object> callback) {
        return send(url, callback);
    }

    public JsHttpExchange get(JsClientRequest request) {
        return send(request, null);
    }

    public JsHttpExchange get(JsClientRequest request, BiConsumer<Object, Object> callback) {
        return send(request, callback);
    }

    public JsHttpExchange send(String url) {
        return send(new JsClientRequest(url), null);
    }

    public JsHttpExchange send(String url, BiConsumer<Object, Object> callback) {
        return send(new JsClientRequest(url), callback);
    }

    public JsHttpExchange send(JsClientRequest request) {
        return send(request, null);
    }

    public JsHttpExchange send(JsClientRequest request, BiConsumer<Object, Object> callback) {
        final CompletableFuture<JsClientResponse> future = new CompletableFuture<>();
        futures.add(future);
        final JsHttpExchange exchange = new JsHttpExchange(future);

        final URI uri;
        try {
            uri = new URI(request.getUrl());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        RequestOptions requestOptions = new RequestOptions()
            .setAbsoluteURI(request.getUrl())
            .setHeaders(request.getHeaders())
            .setMethod(HttpMethod.valueOf(request.getMethod()));

        Future<HttpClientRequest> futureRequest = httpClient.request(requestOptions);
        futureRequest.onFailure(throwable -> handleError(callback, future, throwable));
        futureRequest.onSuccess(httpClientRequest -> {
            // Connection is made, lets continue.
            final Future<HttpClientResponse> futureResponse;

            if (request.getPayload() != null) {
                futureResponse = httpClientRequest.send(Buffer.buffer(request.getPayload()));
            } else {
                futureResponse = httpClientRequest.send();
            }

            futureResponse
                .onSuccess(httpResponse -> handleSuccess(callback, future, httpResponse))
                .onFailure(throwable -> handleError(callback, future, throwable));
        });

        return exchange;
    }

    public void shutDown() throws ExecutionException, InterruptedException {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
    }

    private HttpClientResponse handleSuccess(
        BiConsumer<Object, Object> callback,
        CompletableFuture<JsClientResponse> future,
        HttpClientResponse httpResponse
    ) {
        return httpResponse.bodyHandler(buffer -> {
            final JsClientResponse javascriptResponse = new JsClientResponse();
            javascriptResponse.setStatus(httpResponse.statusCode());
            javascriptResponse.setBody(buffer.toString());

            if (callback != null) {
                callback.accept(javascriptResponse, null);
            }

            future.complete(javascriptResponse);
        });
    }

    private void handleError(BiConsumer<Object, Object> callback, CompletableFuture<JsClientResponse> future, Throwable throwable) {
        callback.accept(null, throwable.getCause());
        future.completeExceptionally(throwable.getCause());
    }
}
