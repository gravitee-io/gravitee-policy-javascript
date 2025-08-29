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

import io.gravitee.gateway.api.http.HttpHeaders;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.experimental.Delegate;

/**
 * This class represents wrapper over HttpHeaders to be used in JavaScript policy.
 * It delegates all methods to the underlying HttpHeaders instance, but also provides
 * additional methods to manipulate headers in a way that is more natural for JavaScript developers.
 * This wrapper also addresses the ambiguity problem when instance of HttpHeaders implements
 * both Map/Multimap and HttpHeaders interfaces as JavaScript engine is not able to properly
 * determine appropriate `forEach` method to call.
 */
public class JsHttpHeaders {

    @Delegate
    private final HttpHeaders headers;

    public JsHttpHeaders(HttpHeaders headers) {
        this.headers = headers;
    }

    @SuppressWarnings("unused")
    public List<String> put(String key, String value) {
        List<String> oldValue = getAll(key);
        headers.set(key, List.of(value));
        return oldValue;
    }

    @SuppressWarnings("unused")
    public List<String> put(String key, List<String> value) {
        List<String> oldValue = getAll(key);
        headers.set(key, new ArrayList<>(value));
        return oldValue;
    }

    @SuppressWarnings("unused")
    public Set<String> keySet() {
        return headers.names();
    }
}
