if (request.headers.containsKey('X-Gravitee-Break')) {
    result.state = State.FAILURE
    result.code = 500
    result.error = 'Stop request processing due to X-Gravitee-Break header'
} else {
    request.headers.set('X-Groovy-Policy', 'ok');
}