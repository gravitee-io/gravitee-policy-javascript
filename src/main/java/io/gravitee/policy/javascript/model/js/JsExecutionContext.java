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

import io.gravitee.gateway.api.ExecutionContext;
import java.util.Map;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JsExecutionContext {

    private static final String CONTEXT_DICTIONARIES_VARIABLE = "dictionaries";
    private final ExecutionContext context;

    public JsExecutionContext(final ExecutionContext context) {
        this.context = context;
    }

    public Map<String, Map<String, String>> getDictionaries() {
        return (Map<String, Map<String, String>>) this.context.getTemplateEngine()
            .getTemplateContext()
            .lookupVariable(CONTEXT_DICTIONARIES_VARIABLE);
    }

    public void attribute(String s, Object o) {
        context.setAttribute(s, o);
    }

    public void setAttribute(String s, Object o) {
        context.setAttribute(s, o);
    }

    public void removeAttribute(String s) {
        context.removeAttribute(s);
    }

    public Object attribute(String s) {
        return context.getAttribute(s);
    }

    public Object getAttribute(String s) {
        return context.getAttribute(s);
    }

    public Map<String, Object> attributes() {
        return context.getAttributes();
    }

    public Map<String, Object> getAttributes() {
        return context.getAttributes();
    }
}
