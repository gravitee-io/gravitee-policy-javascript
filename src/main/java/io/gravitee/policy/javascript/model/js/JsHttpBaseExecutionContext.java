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

import io.gravitee.gateway.reactive.api.context.http.HttpBaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseRequest;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseResponse;
import io.gravitee.reporter.api.v4.metric.Metrics;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsHttpBaseExecutionContext {

    private static final String CONTEXT_DICTIONARIES_VARIABLE = "dictionaries";
    private static final String CONTEXT_PROPERTIES_VARIABLE = "properties";

    private final HttpBaseExecutionContext executionContext;

    public JsHttpBaseExecutionContext(HttpBaseExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    @SuppressWarnings({ "unchecked" })
    public Map<String, String> getDictionaries() {
        return lookupVariable(CONTEXT_DICTIONARIES_VARIABLE);
    }

    @SuppressWarnings({ "unchecked" })
    public Map<String, String> getProperties() {
        return lookupVariable(CONTEXT_PROPERTIES_VARIABLE);
    }

    public HttpBaseRequest request() {
        throw new UnsupportedOperationException("Javascript scripts do not support accessing this method");
    }

    public HttpBaseResponse response() {
        throw new UnsupportedOperationException("Javascript scripts do not support accessing this method");
    }

    public Metrics metrics() {
        return executionContext.metrics();
    }

    public Metrics getMetrics() {
        return executionContext.metrics();
    }

    public void setAttribute(String s, Object o) {
        executionContext.setAttribute(s, o);
    }

    public void putAttribute(String s, Object o) {
        executionContext.putAttribute(s, o);
    }

    public void removeAttribute(String s) {
        executionContext.removeAttribute(s);
    }

    public <T> T getAttribute(String s) {
        return executionContext.getAttribute(s);
    }

    public <T> List<T> getAttributeAsList(String s) {
        return executionContext.getAttributeAsList(s);
    }

    public Set<String> getAttributeNames() {
        return executionContext.getAttributeNames();
    }

    public <T> Map<String, T> getAttributes() {
        return executionContext.getAttributes();
    }

    private Map<String, String> lookupVariable(String variableName) {
        return (Map<String, String>) executionContext.getTemplateEngine().getTemplateContext().lookupVariable(variableName);
    }
}
