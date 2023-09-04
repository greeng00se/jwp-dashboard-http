package org.apache.coyote.http11;

import static org.apache.coyote.http11.common.Constants.CRLF;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.UUID;
import nextstep.jwp.db.InMemoryUserRepository;
import nextstep.jwp.exception.UncheckedServletException;
import nextstep.jwp.model.User;
import org.apache.coyote.Processor;
import org.apache.coyote.http11.common.HttpCookie;
import org.apache.coyote.http11.common.HttpMethod;
import org.apache.coyote.http11.common.HttpStatus;
import org.apache.coyote.http11.common.Session;
import org.apache.coyote.http11.common.SessionManager;
import org.apache.coyote.http11.request.RequestBody;
import org.apache.coyote.http11.request.RequestHeader;
import org.apache.coyote.http11.request.RequestLine;
import org.apache.coyote.http11.response.HttpResponseGenerator;
import org.apache.coyote.http11.response.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http11Processor implements Runnable, Processor {

    private static final Logger LOG = LoggerFactory.getLogger(Http11Processor.class);
    private static final String INDEX_PAGE = "/index.html";
    private static final String REGISTER_PAGE = "/register.html";
    private static final String LOGIN_PAGE = "/login.html";
    private static final String ACCOUNT = "account";
    private static final String PASSWORD = "password";
    private static final String EMAIL = "email";

    private final Socket connection;
    private final HttpResponseGenerator httpResponseGenerator = new HttpResponseGenerator();
    private final SessionManager sessionManager = new SessionManager();

    public Http11Processor(final Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        LOG.info("connect host: {}, port: {}", connection.getInetAddress(), connection.getPort());
        process(connection);
    }

    @Override
    public void process(final Socket connection) {
        try (final InputStream inputStream = connection.getInputStream();
             final OutputStream outputStream = connection.getOutputStream();
             final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            final String firstLine = bufferedReader.readLine();
            if (firstLine == null) {
                return;
            }
            final RequestLine requestLine = RequestLine.from(firstLine);
            final RequestHeader requestHeader = readHeader(bufferedReader);
            final RequestBody requestBody = readBody(bufferedReader, requestHeader);

            final ResponseEntity responseEntity = handleRequest(requestLine, requestHeader, requestBody);

            final String response = httpResponseGenerator.generate(responseEntity);
            outputStream.write(response.getBytes());
            outputStream.flush();
        } catch (IOException | UncheckedServletException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private RequestHeader readHeader(final BufferedReader bufferedReader) throws IOException {
        final StringBuilder stringBuilder = new StringBuilder();
        for (String line = bufferedReader.readLine(); !"".equals(line); line = bufferedReader.readLine()) {
            stringBuilder.append(line).append(CRLF);
        }
        return RequestHeader.from(stringBuilder.toString());
    }

    private RequestBody readBody(final BufferedReader bufferedReader, final RequestHeader requestHeader)
            throws IOException {
        final String contentLength = requestHeader.get("Content-Length");
        if (contentLength == null) {
            return RequestBody.empty();
        }
        final int length = Integer.parseInt(contentLength);
        char[] buffer = new char[length];
        bufferedReader.read(buffer, 0, length);
        return RequestBody.from(new String(buffer));
    }

    private ResponseEntity handleRequest(
            final RequestLine requestLine,
            final RequestHeader requestHeader,
            final RequestBody requestBody
    ) {
        final String path = requestLine.parseUriWithOutQueryString();
        if (path.equals("/login")) {
            return login(requestLine, requestHeader, requestBody);
        }
        if (path.equals("/register")) {
            return register(requestLine, requestBody);
        }
        return new ResponseEntity(HttpStatus.OK, path);
    }

    private ResponseEntity login(
            final RequestLine requestLine,
            final RequestHeader requestHeader,
            final RequestBody requestBody
    ) {
        if (requestLine.getHttpMethod() == HttpMethod.GET) {
            final HttpCookie httpCookie = requestHeader.parseCookie();
            final Session session = sessionManager.findSession(httpCookie.getJSessionId());
            if (session != null) {
                return new ResponseEntity(HttpStatus.FOUND, INDEX_PAGE);
            }
            return new ResponseEntity(HttpStatus.OK, LOGIN_PAGE);
        }
        final String account = requestBody.get(ACCOUNT);
        final String password = requestBody.get(PASSWORD);
        return InMemoryUserRepository.findByAccount(account)
                .filter(user -> user.checkPassword(password))
                .map(this::loginSuccess)
                .orElseGet(() -> new ResponseEntity(HttpStatus.UNAUTHORIZED, "/401.html"));
    }

    private ResponseEntity loginSuccess(final User user) {
        final String uuid = UUID.randomUUID().toString();
        final ResponseEntity responseEntity = new ResponseEntity(HttpStatus.FOUND, INDEX_PAGE);
        responseEntity.setJSessionId(uuid);
        final Session session = new Session(uuid);
        session.setAttribute("user", user);
        sessionManager.add(session);
        return responseEntity;
    }

    private ResponseEntity register(final RequestLine requestLine, final RequestBody requestBody) {
        if (requestLine.getHttpMethod() == HttpMethod.GET) {
            return new ResponseEntity(HttpStatus.OK, REGISTER_PAGE);
        }
        final String account = requestBody.get(ACCOUNT);

        if (InMemoryUserRepository.findByAccount(account).isPresent()) {
            return new ResponseEntity(HttpStatus.CONFLICT, "/409.html");
        }

        final String password = requestBody.get(PASSWORD);
        final String email = requestBody.get(EMAIL);
        InMemoryUserRepository.save(new User(account, password, email));
        return new ResponseEntity(HttpStatus.FOUND, INDEX_PAGE);
    }
}
