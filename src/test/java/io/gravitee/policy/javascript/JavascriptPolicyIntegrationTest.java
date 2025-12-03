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

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractPolicyTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.policy.javascript.configuration.JavascriptPolicyConfiguration;
import io.vertx.ext.web.client.WebClient;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    @DeployApi("/apis/api.json")
    void should_execute_script(WebClient client) throws ExecutionException, InterruptedException, TimeoutException {
        wiremock.stubFor(post("/team").willReturn(ok("").withHeader("X-To-Remove", "value")));

        var response = client.post("/test").send().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().names()).doesNotContain("X-To-Remove");

        wiremock.verify(1, postRequestedFor(urlPathEqualTo("/team")).withHeader("X-Gravitee-Javascript", equalTo("Yes")));
    }

    @Test
    @DeployApi("/apis/api-fail-response-template-no-key.json")
    void should_not_use_response_template_when_no_key_provided(WebClient client)
        throws ExecutionException, InterruptedException, TimeoutException {
        wiremock.stubFor(post("/team").willReturn(ok("")));

        var response = client
            .post("/test")
            .putHeader(HttpHeaderNames.ACCEPT.toString(), "*/*")
            .putHeader("X-Gravitee-Break", "break")
            .send()
            .toCompletionStage()
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.headers().get(HttpHeaderNames.CONTENT_TYPE)).doesNotContain("application/json\"");
        assertThat(response.bodyAsString()).isEqualTo("{\"message\":\"Error message no response template\",\"http_status_code\":409}");
        wiremock.verify(0, postRequestedFor(urlPathEqualTo("/team")));
    }

    @Test
    @DeployApi("/apis/api-fail-response-template.json")
    void should_use_response_template_when_key_provided(WebClient client)
        throws ExecutionException, InterruptedException, TimeoutException {
        wiremock.stubFor(post("/team").willReturn(ok("")));

        var response = client
            .post("/test")
            .putHeader(HttpHeaderNames.ACCEPT.toString(), "*/*")
            .putHeader("X-Gravitee-Break", "break")
            .send()
            .toCompletionStage()
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);

        assertThat(response.statusCode()).isEqualTo(450);
        assertThat(response.headers().get(HttpHeaderNames.CONTENT_TYPE)).doesNotContain("application/xml\"");
        assertThat(response.bodyAsString())
            .isEqualTo(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<auth>\n    <resp>\n        <hdr>E</hdr>\n        <errDesc>internal technical error </errDesc>\n    </resp>\n</auth>"
            );

        wiremock.verify(0, postRequestedFor(urlPathEqualTo("/team")));
    }
}
