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
package io.gravitee.policy.javascript.eval;

public class ScriptContextBindings {

    public static final String MESSAGE_VARIABLE_NAME = "message";
    public static final String REQUEST_VARIABLE_NAME = "request";
    public static final String RESPONSE_VARIABLE_NAME = "response";
    public static final String CONTEXT_VARIABLE_NAME = "context";
    public static final String RESULT_VARIABLE_NAME = "result";
    public static final String HTTP_CLIENT_VARIABLE_NAME = "httpClient";
    public static final String REQUEST_CLASS_VARIABLE_NAME = "Request";
    public static final String STATE_CLASS_VARIABLE_NAME = "State";
}
