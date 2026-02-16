You can use the [JavaScript](http://www.javascript.com/) policy to run JavaScript scripts at any stage of request processing through the gateway.

This policy is applicable to the following API types:

- v2 APIs
- v4 HTTP proxy APIs
- v4 message APIs
- v4 LLM proxy
- v4 MCP proxy
- v4 A2A proxy

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
