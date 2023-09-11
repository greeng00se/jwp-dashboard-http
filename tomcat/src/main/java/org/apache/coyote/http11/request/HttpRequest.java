package org.apache.coyote.http11.request;

import org.apache.coyote.http11.common.Headers;
import org.apache.coyote.http11.common.HttpCookie;
import org.apache.coyote.http11.common.HttpMethod;
import org.apache.coyote.http11.common.HttpVersion;
import org.apache.coyote.http11.common.Session;

public class HttpRequest {

    private final RequestLine requestLine;
    private final Headers headers;
    private final RequestBody requestBody;
    private Session session;

    public HttpRequest(
            final RequestLine requestLine,
            final Headers requestHeader,
            final RequestBody requestBody
    ) {
        this.requestLine = requestLine;
        this.headers = requestHeader;
        this.requestBody = requestBody;
    }

    public boolean isGet() {
        return requestLine.getHttpMethod() == HttpMethod.GET;
    }

    public boolean isPost() {
        return requestLine.getHttpMethod() == HttpMethod.POST;
    }

    public HttpCookie parseCookie() {
        return headers.parseCookie();
    }

    public void setSession(final Session session) {
        this.session = session;
    }

    public String parseSessionId() {
        final HttpCookie httpCookie = headers.parseCookie();
        return httpCookie.getJSessionId();
    }

    public HttpVersion getHttpVersion() {
        return requestLine.getHttpVersion();
    }

    public RequestLine getRequestLine() {
        return requestLine;
    }

    public Headers getHeaders() {
        return headers;
    }

    public RequestBody getRequestBody() {
        return requestBody;
    }

    public Session getSession() {
        return session;
    }
}
