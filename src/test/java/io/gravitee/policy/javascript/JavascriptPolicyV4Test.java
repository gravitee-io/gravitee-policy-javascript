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

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.INTERNAL_SERVER_ERROR_500;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpRequest;
import io.gravitee.gateway.reactive.api.context.http.HttpResponse;
import io.gravitee.gateway.reactive.api.message.DefaultMessage;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.policy.javascript.configuration.JavascriptPolicyConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.MaybeTransformer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JavascriptPolicyV4Test {

    @Mock
    private HttpExecutionContext ctx;

    @Mock
    private HttpRequest request;

    @Mock
    private HttpResponse response;

    @Captor
    private ArgumentCaptor<MaybeTransformer<Buffer, Buffer>> onBodyCaptor;

    @Captor
    private ArgumentCaptor<Function<Message, Maybe<Message>>> onMessageCaptor;

    @BeforeAll
    public static void globalInit() throws Exception {
        final JavascriptInitializer javascriptInitializer = new JavascriptInitializer();
        javascriptInitializer.onActivation();
    }

    @BeforeEach
    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    public void setUp() {
        Map<String, Object> attributes = new HashMap<>();
        lenient().when(ctx.getAttributes()).thenReturn(attributes);
        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(ctx.response()).thenReturn(response);
        lenient().when(request.headers()).thenReturn(HttpHeaders.create());
        lenient().when(response.headers()).thenReturn(HttpHeaders.create());
        lenient()
            .when(ctx.interruptBodyWith(any(ExecutionFailure.class)))
            .thenAnswer(invocation -> Maybe.error(new InterruptionFailureException(invocation.getArgument(0))));
        lenient()
            .when(ctx.interruptMessageWith(any(ExecutionFailure.class)))
            .thenAnswer(invocation -> Maybe.error(new InterruptionFailureException(invocation.getArgument(0))));
    }

    @Test
    void should_fail_with_invalid_script() {
        when(request.onBody(onBodyCaptor.capture())).thenReturn(Completable.complete());

        var policy = new JavascriptPolicy(buildConfig("invalid_script.js"));
        policy.onRequest(ctx).test().assertNoValues();

        ((Maybe<Buffer>) onBodyCaptor.getValue().apply(Maybe.just(Buffer.buffer()))).test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertError(error -> {
                assertThat(error).isInstanceOf(InterruptionFailureException.class);
                InterruptionFailureException failureException = (InterruptionFailureException) error;
                ExecutionFailure executionFailure = failureException.getExecutionFailure();
                assertThat(executionFailure).isNotNull();
                assertThat(executionFailure.key()).isEqualTo("JAVASCRIPT_EXECUTION_FAILURE");
                assertThat(executionFailure.statusCode()).isEqualTo(INTERNAL_SERVER_ERROR_500);
                assertThat(executionFailure.message()).isNotNull();
                return true;
            });
    }

    @Test
    void should_fail_with_result_failure() {
        var policy = new JavascriptPolicy(buildConfig("break_request.js"));

        when(request.onBody(onBodyCaptor.capture())).thenReturn(Completable.complete());
        policy.onRequest(ctx).test().assertNoValues();

        ((Maybe<Buffer>) onBodyCaptor.getValue().apply(Maybe.just(Buffer.buffer()))).test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertError(error -> {
                assertThat(error).isInstanceOf(InterruptionFailureException.class);
                InterruptionFailureException failureException = (InterruptionFailureException) error;
                ExecutionFailure executionFailure = failureException.getExecutionFailure();
                assertThat(executionFailure).isNotNull();
                assertThat(executionFailure.key()).isEqualTo("JAVASCRIPT_FAILED_ON_PURPOSE");
                assertThat(executionFailure.statusCode()).isEqualTo(BAD_REQUEST_400);
                assertThat(executionFailure.message()).isEqualTo("Rejected Request");
                return true;
            });
    }

    @Test
    void should_set_context_attribute_on_http_request() {
        var policy = new JavascriptPolicy(buildConfig("set_context_attribute.js"));

        when(request.onBody(onBodyCaptor.capture())).thenReturn(Completable.complete());
        policy.onRequest(ctx).test().assertNoValues();

        ((Maybe<Buffer>) onBodyCaptor.getValue().apply(Maybe.just(Buffer.buffer()))).test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertNoErrors();

        verify(ctx, times(1)).getAttributes();
        assertThat(ctx.getAttributes().get("count")).isEqualTo(100);
    }

    @Test
    void should_set_context_attribute_on_http_response() {
        var policy = new JavascriptPolicy(buildConfig("set_context_attribute.js"));

        when(response.onBody(onBodyCaptor.capture())).thenReturn(Completable.complete());
        policy.onResponse(ctx).test().assertNoValues();

        ((Maybe<Buffer>) onBodyCaptor.getValue().apply(Maybe.just(Buffer.buffer()))).test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertNoErrors();

        verify(ctx, times(1)).getAttributes();
        assertThat(ctx.getAttributes().get("count")).isEqualTo(100);
    }

    @Test
    void should_set_header_on_http_request() {
        var policy = new JavascriptPolicy(buildConfig("set_request_header.js"));

        when(request.onBody(onBodyCaptor.capture())).thenReturn(Completable.complete());
        policy.onRequest(ctx).test().assertNoValues();

        var headers = HttpHeaders.create();
        when(request.headers()).thenReturn(headers);

        ((Maybe<Buffer>) onBodyCaptor.getValue().apply(Maybe.just(Buffer.buffer()))).test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertNoErrors();

        assertThat(headers.get("x-context")).isEqualTo("test");
    }

    @Test
    void should_set_header_on_http_response() {
        var policy = new JavascriptPolicy(buildConfig("set_response_header.js"));

        when(response.onBody(onBodyCaptor.capture())).thenReturn(Completable.complete());
        policy.onResponse(ctx).test().assertNoValues();

        var headers = HttpHeaders.create();
        when(response.headers()).thenReturn(headers);

        ((Maybe<Buffer>) onBodyCaptor.getValue().apply(Maybe.just(Buffer.buffer()))).test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertNoErrors();

        assertThat(headers.get("x-context")).isEqualTo("test");
    }

    @Test
    void should_remove_header_on_http_request() {
        var policy = new JavascriptPolicy(buildConfig("remove_request_header.js"));

        when(request.onBody(onBodyCaptor.capture())).thenReturn(Completable.complete());
        policy.onRequest(ctx).test().assertNoValues();

        var headers = HttpHeaders.create().set("x-context", "test");
        when(request.headers()).thenReturn(headers);

        ((Maybe<Buffer>) onBodyCaptor.getValue().apply(Maybe.just(Buffer.buffer()))).test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertNoErrors();

        assertThat(headers.size()).isZero();
    }

    @Test
    void should_remove_header_on_http_response() {
        var policy = new JavascriptPolicy(buildConfig("remove_response_header.js"));

        when(response.onBody(onBodyCaptor.capture())).thenReturn(Completable.complete());
        policy.onResponse(ctx).test().assertNoValues();

        var headers = HttpHeaders.create().set("x-context", "test");
        when(response.headers()).thenReturn(headers);

        ((Maybe<Buffer>) onBodyCaptor.getValue().apply(Maybe.just(Buffer.buffer()))).test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertNoErrors();

        assertThat(headers.size()).isZero();
    }

    @Test
    void should_set_context_attribute_on_message_request() {
        var policy = new JavascriptPolicy(buildConfig("set_context_attribute.js"));

        when(request.onMessage(onMessageCaptor.capture())).thenReturn(Completable.complete());
        policy.onMessageRequest(ctx).test().assertNoValues();

        var message = mock(Message.class);

        onMessageCaptor
            .getValue()
            .apply(message)
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertValueCount(1)
            .assertNoErrors();

        verify(ctx, times(1)).getAttributes();
        assertThat(ctx.getAttributes().get("count")).isEqualTo(100);
    }

    @Test
    void should_set_context_attribute_on_message_response() {
        var policy = new JavascriptPolicy(buildConfig("set_context_attribute.js"));

        when(response.onMessage(onMessageCaptor.capture())).thenReturn(Completable.complete());
        policy.onMessageResponse(ctx).test().assertNoValues();

        var message = mock(Message.class);

        onMessageCaptor
            .getValue()
            .apply(message)
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertValueCount(1)
            .assertNoErrors();

        verify(ctx, times(1)).getAttributes();
        assertThat(ctx.getAttributes().get("count")).isEqualTo(100);
    }

    @Test
    void should_set_message_request_header() {
        var policy = new JavascriptPolicy(buildConfig("set_message_header.js"));
        var message = DefaultMessage.builder().headers(HttpHeaders.create()).build();

        when(request.onMessage(onMessageCaptor.capture())).thenReturn(Completable.complete());
        policy.onMessageRequest(ctx).test().assertNoValues();

        onMessageCaptor
            .getValue()
            .apply(message)
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertValueCount(1)
            .assertNoErrors();

        assertThat(message.headers().get("x-context")).isEqualTo("test");
    }

    @Test
    void should_set_message_response_header() {
        var policy = new JavascriptPolicy(buildConfig("set_message_header.js"));
        var message = DefaultMessage.builder().headers(HttpHeaders.create()).build();

        when(response.onMessage(onMessageCaptor.capture())).thenReturn(Completable.complete());
        policy.onMessageResponse(ctx).test().assertNoValues();

        onMessageCaptor
            .getValue()
            .apply(message)
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertValueCount(1)
            .assertNoErrors();

        assertThat(message.headers().get("x-context")).isEqualTo("test");
    }

    @Test
    void should_remove_message_request_header() {
        var policy = new JavascriptPolicy(buildConfig("remove_message_header.js"));
        var message = DefaultMessage.builder().headers(HttpHeaders.create().set("x-context", "test")).build();

        when(request.onMessage(onMessageCaptor.capture())).thenReturn(Completable.complete());
        policy.onMessageRequest(ctx).test().assertNoValues();

        onMessageCaptor
            .getValue()
            .apply(message)
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertValueCount(1)
            .assertNoErrors();

        assertThat(message.headers().size()).isZero();
    }

    @Test
    void should_remove_message_response_header() {
        var policy = new JavascriptPolicy(buildConfig("remove_message_header.js"));
        var message = DefaultMessage.builder().headers(HttpHeaders.create().set("x-context", "test")).build();

        when(response.onMessage(onMessageCaptor.capture())).thenReturn(Completable.complete());
        policy.onMessageResponse(ctx).test().assertNoValues();

        onMessageCaptor
            .getValue()
            .apply(message)
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertValueCount(1)
            .assertNoErrors();

        assertThat(message.headers().size()).isZero();
    }

    @Test
    void should_set_message_attribute() {
        var policy = new JavascriptPolicy(buildConfig("set_message_attribute.js"));
        var message = DefaultMessage.builder().build();

        when(request.onMessage(onMessageCaptor.capture())).thenReturn(Completable.complete());
        policy.onMessageRequest(ctx).test().assertNoValues();

        onMessageCaptor
            .getValue()
            .apply(message)
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertValueCount(1)
            .assertNoErrors();

        assertThat(message.attributes()).containsEntry("count", 100);
    }

    private static JavascriptPolicyConfiguration buildConfig(String script) {
        var config = new JavascriptPolicyConfiguration();
        config.setScript(loadScript(script));
        config.setReadContent(true);
        return config;
    }

    @Test
    void should_get_message_binary_content_as_base64() {
        var policy = new JavascriptPolicy(buildConfig("get_message_binary_content.js"));
        var message = DefaultMessage.builder().build();
        byte[] isoEncodedCharacter = "é".getBytes(StandardCharsets.ISO_8859_1);
        message.content(Buffer.buffer(isoEncodedCharacter));

        when(request.onMessage(onMessageCaptor.capture())).thenReturn(Completable.complete());
        policy.onMessageRequest(ctx).test().assertNoValues();

        onMessageCaptor
            .getValue()
            .apply(message)
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertValueCount(1)
            .assertNoErrors();

        assertThat(message.<String>attribute("wronglyBase64EncodedContent")).isNotEqualTo(
            Base64.getEncoder().encodeToString(isoEncodedCharacter)
        );
        assertThat(message.<String>attribute("goodBase64Content")).isEqualTo(Base64.getEncoder().encodeToString(isoEncodedCharacter));
        assertThat(message.<byte[]>attribute("byteArray")).isEqualTo(isoEncodedCharacter);
    }

    @Test
    void should_rewrite_request_headers_to_response() {
        var policy = new JavascriptPolicy(buildConfig("rewrite_request_headers_to_response.js"));

        var requestHeaders = HttpHeaders.create().set("A", "AValue").set("B", "BValue").set("C", "CValue");
        when(request.headers()).thenReturn(requestHeaders);

        var responseHeaders = HttpHeaders.create();
        when(response.headers()).thenReturn(responseHeaders);

        when(request.onBody(onBodyCaptor.capture())).thenReturn(Completable.complete());
        policy.onRequest(ctx).test().assertNoValues();

        ((Maybe<Buffer>) onBodyCaptor.getValue().apply(Maybe.just(Buffer.buffer()))).test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertNoErrors();

        assertThat(responseHeaders.toListValuesMap()).isEqualTo(requestHeaders.toListValuesMap());
    }

    private static String loadScript(String file) {
        try {
            return new String(getResourceAsStream(file).readAllBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read script bytes from file " + file, e);
        }
    }

    private static InputStream getResourceAsStream(String file) {
        var resourceAsStream = JavascriptPolicy.class.getResourceAsStream(file);

        if (resourceAsStream == null) {
            throw new IllegalArgumentException("Unable to find script file " + file);
        }

        return resourceAsStream;
    }
}
