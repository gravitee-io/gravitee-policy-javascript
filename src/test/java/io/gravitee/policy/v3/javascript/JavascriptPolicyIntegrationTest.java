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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractPolicyTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.policy.javascript.configuration.JavascriptPolicyConfiguration;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import io.vertx.rxjava3.ext.web.client.WebClient;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class JavascriptPolicyIntegrationTest extends AbstractPolicyTest<JavascriptPolicy, JavascriptPolicyConfiguration> {

    private static final String GIVEN_CONTENT = """
        {"message":"Hello World!"}
        """;

    private static final String EXPECTED_CONTENT = """
        {"message":"Hello Universe!"}
        """;

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        super.configureGateway(gatewayConfigurationBuilder);
        gatewayConfigurationBuilder.set("api.jupiterMode.enabled", "false");
    }

    /**
     * Override api plans to have a published API_KEY one.
     * @param api is the api to apply this function code
     */
    @Override
    public void configureApi(Api api) {
        api.setExecutionMode(ExecutionMode.V3);
    }

    @Test
    @DeployApi("/apis/v3/api.json")
    void should_execute_script(HttpClient client) {
        wiremock.stubFor(post("/team").willReturn(ok("").withHeader("X-To-Remove", "value")));

        WebClient.wrap(client)
            .post("/test")
            .rxSend()
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.headers().names()).doesNotContain("X-To-Remove");
                return true;
            })
            .assertComplete()
            .assertNoErrors();

        wiremock.verify(1, postRequestedFor(urlPathEqualTo("/team")).withHeader("X-Gravitee-Javascript", equalTo("Yes")));
    }

    @Test
    @DeployApi("/apis/v3/api-fail-response-template-no-key.json")
    void should_not_use_response_template_when_no_key_provided(HttpClient client) {
        wiremock.stubFor(post("/team").willReturn(ok("")));

        WebClient.wrap(client)
            .post("/test")
            .putHeader(HttpHeaderNames.ACCEPT.toString(), "*/*")
            .putHeader("X-Gravitee-Break", "break")
            .rxSend()
            .map(response -> {
                assertThat(response.statusCode()).isEqualTo(409);
                assertThat(response.headers().get(HttpHeaderNames.CONTENT_TYPE)).isNotNull().isEqualTo("application/json");
                return response.body();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertValue(body -> {
                assertThat(body).hasToString("{\"message\":\"Error message no response template\",\"http_status_code\":409}");
                return true;
            })
            .assertComplete()
            .assertNoErrors();

        wiremock.verify(0, postRequestedFor(urlPathEqualTo("/team")));
    }

    @Test
    @DeployApi("/apis/v3/api-fail-response-template.json")
    void should_use_response_template_when_key_provided(HttpClient client) {
        wiremock.stubFor(post("/team").willReturn(ok("")));

        WebClient.wrap(client)
            .post("/test")
            .putHeader(HttpHeaderNames.ACCEPT.toString(), "*/*")
            .putHeader("X-Gravitee-Break", "break")
            .rxSend()
            .map(response -> {
                assertThat(response.statusCode()).isEqualTo(450);
                assertThat(response.headers().get(HttpHeaderNames.CONTENT_TYPE)).isNotNull().isEqualTo("application/xml");
                return response.body();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertValue(body -> {
                assertThat(body).hasToString(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<auth>\n    <resp>\n        <hdr>E</hdr>\n        <errDesc>internal technical error </errDesc>\n    </resp>\n</auth>"
                );
                return true;
            })
            .assertComplete()
            .assertNoErrors();

        wiremock.verify(0, postRequestedFor(urlPathEqualTo("/team")));
    }

    @Test
    @DeployApi("/apis/v3/api-post.json")
    void should_execute_on_response_and_on_response_content_scripts(HttpClient client) {
        wiremock.stubFor(post("/team").willReturn(okJson(GIVEN_CONTENT)));

        client
            .rxRequest(POST, "/api-post")
            .map(request -> request.putHeader(HttpHeaderNames.ACCEPT.toString(), "*/*"))
            .flatMap(HttpClientRequest::rxSend)
            .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
            .doOnSuccess(response -> assertThat(response.headers().names()).contains("X-Phase"))
            .flatMap(HttpClientResponse::body)
            .doOnSuccess(body -> assertThat(body.toString().trim()).hasToString(EXPECTED_CONTENT.trim()))
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete()
            .assertNoErrors();

        wiremock.verify(1, postRequestedFor(urlPathEqualTo("/team")));
    }

    @Test
    @DeployApi("/apis/v3/api-pre-nocontent.json")
    public void should_execute_on_request_script_only(HttpClient client) {
        wiremock.stubFor(get("/team").willReturn(ok("")));

        client
            .rxRequest(GET, "/api-pre")
            .flatMap(request -> request.rxSend())
            .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete()
            .assertNoErrors();

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/team")).withHeader("X-Phase", equalTo("on-request")));
    }

    @Test
    @DeployApi("/apis/v3/api-pre.json")
    void should_execute_on_request_and_on_request_content_script(HttpClient client) {
        wiremock.stubFor(post("/team").willReturn(ok("")));

        client
            .rxRequest(POST, "/api-pre")
            .flatMap(request -> request.rxSend(GIVEN_CONTENT))
            .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete()
            .assertNoErrors();

        wiremock.verify(
            1,
            postRequestedFor(urlPathEqualTo("/team"))
                .withHeader("X-Phase", equalTo("on-request"))
                .withRequestBody(equalToJson(EXPECTED_CONTENT.trim()))
        );
    }

    @Test
    @DeployApi("/apis/v3/api-with-script-error.json")
    void should_fail_with_package_declaration(HttpClient client) {
        client
            .rxRequest(POST, "/fail")
            .flatMap(HttpClientRequest::rxSend)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(500);
                return true;
            })
            .assertComplete()
            .assertNoErrors();
    }
}
