package nextstep.jwp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.UUID;
import nextstep.jwp.db.InMemoryUserRepository;
import nextstep.jwp.model.User;
import org.apache.catalina.SessionManager;
import org.apache.catalina.controller.Controller;
import org.apache.coyote.http11.common.Headers;
import org.apache.coyote.http11.common.HttpStatus;
import org.apache.coyote.http11.common.HttpVersion;
import org.apache.coyote.http11.common.Session;
import org.apache.coyote.http11.request.HttpRequest;
import org.apache.coyote.http11.request.RequestBody;
import org.apache.coyote.http11.request.RequestLine;
import org.apache.coyote.http11.response.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LoginControllerTest {

    private final Controller controller = new LoginController();
    private final SessionManager sessionManager = new SessionManager();

    @BeforeEach
    void setUp() {
        sessionManager.clear();
        InMemoryUserRepository.clear();
    }

    @Test
    void GET_요청을_받았을_때_세션에_사용자_정보가_없는_경우_login_페이지를_반환하도록_설정한다() {
        // given
        final RequestLine requestLine = RequestLine.from("GET /login HTTP/1.1");
        final HttpRequest httpRequest = new HttpRequest(requestLine, new Headers(), new RequestBody());
        httpRequest.setSession(new Session());
        final HttpResponse httpResponse = new HttpResponse(HttpVersion.HTTP_1_1);

        // when
        controller.service(httpRequest, httpResponse);

        // then
        assertAll(
                () -> assertThat(httpResponse.getHttpStatus()).isEqualTo(HttpStatus.OK),
                () -> assertThat(httpResponse.getRedirect()).isEqualTo("/login.html")
        );
    }

    @Test
    void GET_요청을_받았을_때_세션에_사용자_정보가_있는_경우_index_페이지를_반환하도록_설정한다() {
        // given
        final RequestLine requestLine = RequestLine.from("GET /login HTTP/1.1");
        final String uuid = UUID.randomUUID().toString();
        final Session session = new Session(uuid);
        final HttpRequest httpRequest = new HttpRequest(requestLine, new Headers(), new RequestBody());
        session.setAttribute("user", new User("gugu", "password", "gugu@naver.com"));
        httpRequest.setSession(session);
        final HttpResponse httpResponse = new HttpResponse(HttpVersion.HTTP_1_1);
        sessionManager.add(session);

        // when
        controller.service(httpRequest, httpResponse);

        // then
        assertAll(
                () -> assertThat(httpResponse.getHttpStatus()).isEqualTo(HttpStatus.FOUND),
                () -> assertThat(httpResponse.getRedirect()).isEqualTo("/index.html")
        );
    }

    @Test
    void POST_요청을_받았을_때_인증이_실패하는_경우_401_UNAUTHORIZED를_반환한다() {
        // given
        final RequestLine requestLine = RequestLine.from("POST /login HTTP/1.1");
        final RequestBody requestBody = RequestBody.from("account=hello&password=pw");
        final HttpRequest httpRequest = new HttpRequest(requestLine, new Headers(), requestBody);
        final HttpResponse httpResponse = new HttpResponse(HttpVersion.HTTP_1_1);
        InMemoryUserRepository.save(new User("hello", "world", "email@email.com"));

        // when
        controller.service(httpRequest, httpResponse);

        // then
        assertAll(
                () -> assertThat(httpResponse.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED),
                () -> assertThat(httpResponse.getRedirect()).isEqualTo("/401.html")
        );
    }

    @Test
    void POST_요청을_받았을_때_인증에_성공하는_경우_index_페이지를_반환하도록_설정한다() {
        // given
        final RequestLine requestLine = RequestLine.from("POST /login HTTP/1.1");
        final RequestBody requestBody = RequestBody.from("account=hello&password=world");
        final HttpRequest httpRequest = new HttpRequest(requestLine, new Headers(), requestBody);
        final String uuid = UUID.randomUUID().toString();
        final Session session = new Session(uuid);
        httpRequest.setSession(session);
        final HttpResponse httpResponse = new HttpResponse(HttpVersion.HTTP_1_1);
        InMemoryUserRepository.save(new User("hello", "world", "email@email.com"));

        // when
        controller.service(httpRequest, httpResponse);

        // then
        assertAll(
                () -> assertThat(httpResponse.getHttpStatus()).isEqualTo(HttpStatus.FOUND),
                () -> assertThat(httpResponse.getRedirect()).isEqualTo("/index.html")
        );
    }
}
