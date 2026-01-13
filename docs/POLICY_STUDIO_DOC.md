## Overview
You can use the [JavaScript](http://www.javascript.com/) policy to run JavaScript scripts at any stage of request processing through the gateway.

This policy is applicable to the following API types:

- v2 APIs
- v4 HTTP proxy APIs
- v4 message APIs
- v4 LLM proxy
- v4 MCP proxy

**Note:** The JavaScript policy is not supported by v4 TCP or Native APIs.

Several variables are automatically bound to the JavaScript script. These let you read, and potentially modify, their values to define the behavior of the policy.

### High level variables

List of high-level variables available within the script:

| Variable   | Description                                                                 |
|------------|-----------------------------------------------------------------------------|
| `request`  | Inbound HTTP request                                                        |
| `response` | Outbound HTTP response                                                      |
| `message`  | Message transiting the Gateway                                              |
| `context`  | Context usable to access external components such as services and resources |
| `result`   | Object to return to alter the outcome of the request/response               |

### Content variables

List of content-specific variables available within the script:

| Variable            | Description                                                                 |
|---------------------|-----------------------------------------------------------------------------|
| `request.content`   | Inbound request content, available when "Read content" is enabled.          |
| `response.content`  | Outbound response content, available when "Read content" is enabled.        |
| `message.content`   | Message content, always available.                                          |

---

See the **Usage** and **Schema** sections for further explanation regarding available objects, their attributes, and methods.



## Usage
## Change the outcome

To change the outcome, access the `result` object in your script and use the following properties:

List of variables that allow you to control the outcome:

| Attribute | Type               | Description                |
|-----------|--------------------|----------------------------|
| `state`   | `PolicyResult.State` | To indicate a failure     |
| `code`    | integer            | An HTTP status code        |
| `error`   | string             | The error message          |
| `key`     | string             | The key of a response template |

Setting `state` to `FAILURE` will, by default, throw a `500 - internal server error`, but you can override this behavior with the above properties.

Example in the request phase:

```javascript
if (request.headers.containsKey('X-Gravitee-Break')) {
    result.key = 'RESPONSE_TEMPLATE_KEY';
    result.state = State.FAILURE;
    result.code = 500
    result.error = 'Stop request processing due to X-Gravitee-Break header'
} else {
    request.headers.set('X-JavaScript-Policy', 'ok');
}
```

To customize the error sent by the policy:

```javascript
result.key = 'RESPONSE_TEMPLATE_KEY';
result.state = State.FAILURE;
result.code = 400
result.error = '{"error":"My specific error message","code":"MY_ERROR_CODE"}'
result.contentType = 'application/json'
```

---

## Override content

To override content, you must enable **Override content** and make your script return the new content as the last instruction.

### Input body content

```json
[
    {
        "age": 32,
        "firstname": "John",
        "lastname": "Doe"
    }
]
```

### JavaScript script

The following example shows how to use the JavaScript policy to transform JSON content:

```javascript
var content = JSON.parse(response.content);
content[0].firstname = 'Hacked ' + content[0].firstname;
content[0].country = 'US';

JSON.stringify(content);
```

### Output body content

```json
[
    {
        "age": 32,
        "firstname": "Hacked John",
        "lastname": "Doe",
        "country": "US"
    }
]
```

---

## Dictionaries - Properties

Both Dictionaries (defined at the environment level) and Properties (defined at the API level) can be accessed from the JavaScript script, using:

- `context.dictionaries()` for Dictionaries
- `context.properties()` for Properties

Example of setting a request header based on a Property:

```javascript
request.headers.set('X-JavaScript-Policy', context.properties()['KEY_OF_MY_PROPERTY']);
```

---

# Schema

## Request

`request.<property>`

| Property         | Type                          | Mutable | Description |
|------------------|-------------------------------|---------|-------------|
| `content`        | `String`                      |         | Body of the request. |
| `transactionId`  | `String`                      |         | Unique identifier for the transaction. |
| `clientIdentifier` | `String`                   |         | Identifies the client that made the request. |
| `uri`            | `String`                      |         | The complete request URI. |
| `host`           | `String`                      |         | Host from the incoming request. |
| `originalHost`   | `String`                      |         | Host as originally received before any internal rewriting. |
| `contextPath`    | `String`                      |         | API context path. |
| `pathInfo`       | `String`                      |         | Path beyond the context path. |
| `path`           | `String`                      |         | The full path component of the request URI. |
| `parameters`     | `Map<String, List<String>>`   | ✅       | Query parameters as a multi-value map. See [Multimap methods](#multimap-methods). |
| `pathParameters` | `Map<String, List<String>>`   |         | Parameters extracted from path templates. Alter methods are not useful here. See [Multimap methods](#multimap-methods). |
| `headers`        | `Map<String, List<String>>`   | ✅       | HTTP headers. See [Headers methods](#headers-methods). |
| `method`         | `HttpMethod` (enum)           |         | HTTP method (GET, POST, etc.). |
| `scheme`         | `String`                      |         | HTTP or HTTPS. |
| `version`        | `HttpVersion` (enum)          |         | Protocol version: `HTTP_1_0`, `HTTP_1_1`, `HTTP_2`. |
| `timestamp`      | `long`                        |         | Epoch timestamp when request was received. |
| `remoteAddress`  | `String`                      |         | Client IP address. |
| `localAddress`   | `String`                      |         | Local server IP address. |

---

## Response

`response.<property>`

| Property   | Type                        | Mutable | Description |
|------------|-----------------------------|---------|-------------|
| `content`  | `String`                    |         | Body of the response. |
| `status`   | `int`                       |         | Response status code. |
| `reason`   | `String`                    |         | Reason for the status. |
| `headers`  | `Map<String, List<String>>` | ✅       | HTTP headers. See [Headers methods](#headers-methods). |

---

## Message

`message.<property>`

| Property            | Type                        | Mutable | Description |
|---------------------|-----------------------------|---------|-------------|
| `correlationId`     | `String`                    |         | Correlation ID to track the message. |
| `parentCorrelationId` | `String`                  |         | Parent correlation ID. |
| `timestamp`         | `long`                      |         | Epoch (ms) timestamp. |
| `error`             | `boolean`                   |         | Whether the message is an error message. |
| `metadata`          | `Map<String, Object>`       | ✅       | Message metadata, system-dependent. |
| `headers`           | `Map<String, List<String>>` | ✅       | Message headers. See [Headers methods](#headers-methods). |
| `content`           | `String`                    |         | Message body as a string. |
| `contentAsBase64`   | `String`                    |         | Message body as a base64 string. |
| `contentAsByteArray`| `byte[]`                    |         | Message body as bytes. |
| `attributes`        | `Map<String, Object>`       | ✅       | Message attributes. See below. |

### Message attributes methods

`message.attributes.<method>`

| Method         | Arguments (type) | Return type  | Description |
|----------------|------------------|--------------|-------------|
| `remove`       | key (`Object`)   |              | Remove an attribute. |
| `containsKey`  | key (`Object`)   | `boolean`    | Check if an attribute exists. |
| `containsValue`| value (`Object`) | `boolean`    | Check if attributes contain a value. |
| `empty`        |                  | `boolean`    | `true` if no attributes exist. |
| `size`         |                  | `int`        | Attribute count. |
| `keySet`       |                  | `Set<String>`| All attribute names. |

---

## Context

`context.<property>`

| Property          | Type                 | Mutable | Description |
|-------------------|----------------------|---------|-------------|
| `attributes`      | `Map<String, Object>`| ✅       | Context attributes as a map. |
| `attributeNames`  | `Set<String>`        |         | All attribute names. |
| `attributeAsList` | `List<Object>`       |         | All attribute values. |

### Context attributes methods

`context.attributes.<method>`

Refer to Gravitee documentation for available attributes.

| Method        | Arguments (type)       | Return type | Description |
|---------------|------------------------|-------------|-------------|
| `get`         | key (`Object`)         | `Object`    | Get a Gravitee attribute (e.g., `gravitee.attribute.*`). |
| `containsKey` | key (`Object`)         | `boolean`   | Check if a Gravitee attribute exists. |
| `set`         | key (`String`), value (`Object`) |             | Set a Gravitee attribute. |

Other `java.util.Map` methods are also available, such as `remove` and `size`.

---

## Common objects

### <a id="headers-methods"></a>Headers methods

Applicable to `request.headers`, `response.headers`, `message.headers`.

| Method        | Arguments (type)                       | Return type                         | Description |
|---------------|----------------------------------------|-------------------------------------|-------------|
| `get`         | key (`String`)                         | `String`                            | Get first value of a header. |
| `getAll`      | key (`String`)                         | `List<String>`                      | Get all values of a header. |
| `put`         | key (`String`), value (`String`)       | `List<String>` (previous values)    | Replace header with a single value. |
| `put`         | key (`String`), value (`List<String>`) | `List<String>` (previous values)    | Replace header with multiple values. |
| `set`         | key (`String`), value (`String`)       | Updated headers object              | Set a header. |
| `set`         | key (`String`), value (`List<String>`) | Updated headers object              | Set a header. |
| `remove`      | key (`String`)                         | Updated headers object              | Remove a header. |
| `containsKey` | key (`Object`)                         | `boolean`                           | Check if a header exists. |
| `clear`       |                                        |                                     | Remove all headers. |
| `isEmpty`     |                                        | `boolean`                           | `true` if no headers exist. |
| `size`        |                                        | `int`                               | Header count. |
| `keySet`      |                                        | `Set<String>`                       | All header names. |
| `entrySet`    |                                        | `Set<Map.Entry<String, List<String>>>` | All headers as entries. |
| `forEach`     | consumer (`Map.Entry<String, List<String>>`) |                                   | Iterate over headers. |

---

### <a id="multimap-methods"></a>Multimap methods

Multimap lets you use several values for a single map entry without pre-initializing a collection.

Applicable to `request.parameters` and `request.pathParameters`.

All methods of the Gravitee [MultiValueMap](https://github.com/gravitee-io/gravitee-common/blob/master/src/main/java/io/gravitee/common/util/MultiValueMap.java) implementation are supported:  
`getFirst`, `add`, `set`, `setAll`, `toSingleValueMap`, `containsAllKeys`.

Other `java.util.Map` methods are also available, such as `remove` and `size`.

## Coming from Apigee

You will find below the main differences between JS scripts coming from **Apigee** and the ones you can run on the **Gravitee** platform:

| Feature | Apigee | Gravitee | Comment |
|---------|--------|----------|---------|
| Access to context variables | `context.getVariable('foo');` | `context.attributes.foo;` | |
| Setting a context variable | `context.setVariable('foo', 'bar');` | `context.attributes.foo = 'bar';` | |
| Changing request or response header | `context.targetRequest.headers['TARGET-HEADER-X']='foo';` | `request.headers.set('TARGET-HEADER-X', 'foo');` | `set` is used to replace the header value. |
| Multivalued request or response header | ? | ```response.headers.add('TARGET-HEADER-X', 'foo'); response.headers.add('TARGET-HEADER-X', 'bar');``` | `add` can be used for multivalued headers. |
| Changing response code or message | `targetResponse.status.code = 500;` | `response.status(500);` | See `result` if you want to break the policy chain and return an error. |
| Changing the body response | `context.proxyResponse.content = 'foo';` | `'foo';` | Just set last instruction of the `OnRequestContent` to override the request body or `OnResponseContent` to override the response body. |
| Print messages | `print('foo');` | `print('foo');` | The `print` statement has no effect and is simply ignored for now. |
| Importing another js script | | | This is not supported for now. |
| Playing with request / response phases | ```if (context.flow=="PROXY_RESP_FLOW") { // do something; }``` | Use a script on each phase | Phases are not exactly the same and Gravitee does not allow a single script on different phases. You must define one script per phase or leave the field blank if no script is necessary. |
| Timeout | `timeLimit` configuration at JavaScript policy level | | The timeout is not supported for now. |
| Manage errors | ? | ```result.state = State.FAILURE; result.code = 400; result.error = '{"error":"My specific error message","code":"MY_ERROR_CODE"}'; result.contentType = 'application/json';``` | |
| Http call | `httpClient.get("http://example.com", callback);` | `httpClient.get("http://example.com", callback);` | ⚠️ **DEPRECATED**: This feature will be removed in a future release. Use the **HTTP Callout** policy instead for making HTTP calls. |

## Compatibility and Deprecation Notes

### Deprecated Script Properties

The following properties of the JavaScript policy are **deprecated** and will be removed in future releases:

- `onRequestScript`
- `onResponseScript`
- `onRequestContentScript`
- `onResponseContentScript`

Use the new configuration format, which allows defining a single **script** along with additional options such as *Read content* and *Override content*.

If you created a v4 HTTP API with the JavaScript policy (version <= 1.4.0), `onRequestScript` and `onResponseScript` will still execute during the request and response phases respectively.
It is strongly recommended to migrate your API to the new configuration format and use the `script` property instead.

Because `onRequestScript` and `onResponseScript` are no longer displayed due to deprecation, you can retrieve their values via the Management API (e.g., [listing plans associated with the given API](https://gravitee-io-labs.github.io/mapi-v1-docs/#tag/api-plans/get/v1/organizations/{orgId}/environments/{envId}/apis/{api}/plans)) or using the export feature in the Gravitee UI under **API Configuration → General** section.

> **Note:**
> The easiest migration path is:
> 1. Create a new policy with the copied script.
> 2. Delete the old one.
> 3. Save and deploy the API.

### HttpClient Deprecation

⚠️ **DEPRECATED**: The `httpClient` object available in JavaScript scripts is **deprecated** and will be removed in a future release.

**What's changing:**
- The `httpClient.get()`, `httpClient.post()`, and other HTTP methods will no longer be available within JavaScript policy scripts.

**Migration path:**
- Use the **HTTP Callout** policy instead for making HTTP calls from your API flows.

**Why this change:**
- The current `httpClient` implementation has security and performance limitations.
- The **HTTP Callout** policy provides better control, security, and monitoring capabilities for external HTTP calls.

If you are currently using `httpClient` in your JavaScript scripts, please plan to migrate to the recommended approach to ensure compatibility with future versions.  



## Errors
These templates are defined at the API level, in the "Entrypoint" section for v4 APIs, or in "Response Templates" for v2 APIs.
The error keys sent by this policy are as follows:

| Key |
| ---  |
| JAVASCRIPT_EXECUTION_FAILURE |


