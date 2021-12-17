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

import io.vertx.core.MultiMap;
import java.util.Map;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JsClientRequest {

    private String url;
    private String method;
    private MultiMap headers;
    private String payload;

    public JsClientRequest() {}

    public JsClientRequest(String url) {
        this.url = url;
        this.method = "GET";
    }

    public JsClientRequest(String url, String method) {
        this.url = url;
        this.method = method;
    }

    public JsClientRequest(String url, String method, Map<String, Object> headers) {
        this.url = url;
        this.method = method;
        this.headers = toMultiMap(headers);
    }

    public JsClientRequest(String url, String method, Map<String, Object> headers, String payload) {
        this.url = url;
        this.method = method;
        this.headers = toMultiMap(headers);
        this.payload = payload;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public MultiMap getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = toMultiMap(headers);
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    private MultiMap toMultiMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        final MultiMap multiMap = MultiMap.caseInsensitiveMultiMap();
        map.forEach((key, value) -> addToMap(multiMap, key, value));

        return multiMap;
    }

    private void addToMap(MultiMap map, String key, Object value) {
        if (value instanceof String) {
            map.add(key, (String) value);
        } else if (value instanceof ScriptObjectMirror) {
            ((ScriptObjectMirror) value).forEach((s, o) -> addToMap(map, key, o));
        }
    }
}
