package org.springframework.security.boot.yiban.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.BasicHttpEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.boot.biz.userdetails.UserDetailsServiceAdapter;
import org.springframework.security.boot.yiban.authentication.YibanAuthenticationProvider;
import org.springframework.security.boot.yiban.authentication.YibanAuthenticationToken;
import org.springframework.security.boot.yiban.authentication.YibanAuthorizationProcessingFilter;
import org.springframework.security.boot.yiban.authentication.YibanPreAuthenticatedProcessingFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class YibanOAuthFlowTest {

    private static final String LOGIN = "/login/yiban";
    private static final String CALLBACK = "https://app.example.test/login/yiban?source=fixture";
    private static final String TOKEN = "fixture token+";
    private static final String CODE = "中文 +/=";

    private final AtomicInteger exchanges = new AtomicInteger();
    private final AtomicReference<Map<String, String>> form = new AtomicReference<>();
    private final AtomicReference<Map<String, String>> query = new AtomicReference<>();
    private final AtomicReference<String> tokenResponse = new AtomicReference<>("{\"access_token\":\"fixture token+\"}");
    private final AtomicReference<String> profileResponse =
            new AtomicReference<>("{\"status\":\"success\",\"info\":{\"yb_userid\":\"user-1\"}}");

    private HttpServer server;
    private YibanOAuthClient client;

    @BeforeEach
    void startGateway() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token/info", exchange -> {
            exchanges.incrementAndGet();
            BasicHttpEntity body = new BasicHttpEntity();
            body.setContent(exchange.getRequestBody());
            form.set(URLEncodedUtils.parse(EntityUtils.toString(body, StandardCharsets.UTF_8), StandardCharsets.UTF_8)
                    .stream().collect(Collectors.toMap(pair -> pair.getName(), pair -> pair.getValue())));
            respond(exchange, tokenResponse.get());
        });
        server.createContext("/user/me", exchange -> {
            query.set(parameters(exchange.getRequestURI()));
            respond(exchange, profileResponse.get());
        });
        server.start();
        URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        client = new YibanOAuthClient("fixture-app", "fixture-secret", CALLBACK,
                base.resolve("/code/authorize"), base.resolve("/token/info"), base.resolve("/user/me"));
    }

    @AfterEach
    void stopGateway() throws IOException {
        try {
            client.close();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void exchangesOnlyTheMatchingCallbackAndAuthenticatesTheResolvedAccount() throws Exception {
        UserDetailsServiceAdapter users = mock(UserDetailsServiceAdapter.class);
        when(users.loadUserDetails(any(Authentication.class)))
                .thenReturn(User.withUsername("local-user").password("unused").authorities("ROLE_USER").build());

        YibanAuthenticationProvider provider = new YibanAuthenticationProvider(users, client);
        YibanAuthorizationProcessingFilter callback = new YibanAuthorizationProcessingFilter(client);
        callback.setAuthenticationManager(provider::authenticate);
        YibanPreAuthenticatedProcessingFilter initiation = new YibanPreAuthenticatedProcessingFilter(client, LOGIN, "code");

        MockHttpServletRequest start = request();
        MockHttpServletResponse outward = new MockHttpServletResponse();
        initiation.doFilter(start, outward, new MockFilterChain());
        URI redirect = URI.create(outward.getRedirectedUrl());
        assertThat(redirect.getPath()).isEqualTo("/code/authorize");
        Map<String, String> auth = parameters(redirect);
        assertThat(auth).containsEntry("client_id", "fixture-app").containsEntry("redirect_uri", CALLBACK)
                .doesNotContainKey("client_secret");
        String state = auth.get("state");
        assertThat(state).isNotBlank();

        MockHttpServletRequest denied = callback((MockHttpSession) start.getSession(), "invalid-state");
        assertThatThrownBy(() -> callback.doAttemptAuthentication(denied, new MockHttpServletResponse()))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(exchanges.get()).isZero();

        MockHttpServletRequest valid = callback((MockHttpSession) start.getSession(), state);
        MockFilterChain forwarded = new MockFilterChain();
        initiation.doFilter(valid, new MockHttpServletResponse(), forwarded);
        assertThat(forwarded.getRequest()).isSameAs(valid);
        Authentication authenticated = callback.doAttemptAuthentication(valid, new MockHttpServletResponse());
        assertThat(authenticated.isAuthenticated()).isTrue();
        assertThat(authenticated.getName()).isEqualTo("local-user");
        assertThat(form.get()).containsEntry("code", CODE).containsEntry("client_id", "fixture-app")
                .containsEntry("client_secret", "fixture-secret").containsEntry("redirect_uri", CALLBACK);
        assertThat(query.get()).containsEntry("access_token", TOKEN);

        assertThatThrownBy(() -> callback.doAttemptAuthentication(valid, new MockHttpServletResponse()))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(exchanges.get()).isEqualTo(1);
    }

    @Test
    void rejectsTheResponseWhenTheVendorDoesNotReturnAnAccessToken() {
        tokenResponse.set("{\"error\":\"fixture-secret\"}");
        assertThatThrownBy(() -> client.exchangeCode(CODE))
                .isInstanceOf(AuthenticationServiceException.class)
                .hasMessageNotContaining("fixture-secret");
    }

    @Test
    void rejectsTheResponseWhenTheVendorDoesNotReturnAUserId() {
        UserDetailsServiceAdapter users = mock(UserDetailsServiceAdapter.class);
        YibanAuthenticationProvider provider = new YibanAuthenticationProvider(users, client);
        profileResponse.set("{\"status\":\"success\",\"info\":{}}");
        assertThatThrownBy(() -> provider.authenticate(
                new YibanAuthenticationToken(TOKEN)))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void handlesLoginAndCallbackThroughTheAutoConfiguredSecurityChain() {
        UserDetailsServiceAdapter users = mock(UserDetailsServiceAdapter.class);
        when(users.loadUserDetails(any(Authentication.class)))
                .thenReturn(User.withUsername("local-user").password("unused").authorities("ROLE_USER").build());
        new WebApplicationContextRunner()
                .withUserConfiguration(Application.class)
                .withBean(YibanOAuthClient.class, () -> client, definition -> definition.setDestroyMethodName(""))
                .withBean(UserDetailsServiceAdapter.class, () -> users)
                .withPropertyValues("spring.security.yiban.enabled=true",
                        "spring.security.yiban.authc.app-key=fixture-app",
                        "spring.security.yiban.authc.app-secret=fixture-secret",
                        "spring.security.yiban.authc.callback=" + CALLBACK)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(YibanOAuthClient.class);
                    assertThat(context).hasSingleBean(YibanAuthenticationProvider.class);
                    FilterChainProxy filters = context.getBean("springSecurityFilterChain", FilterChainProxy.class);
                    SecurityFilterChain yiban = filters.getFilterChains().stream()
                            .filter(chain -> chain.matches(request())).findFirst().get();
                    assertThat(yiban.getFilters()).anyMatch(YibanPreAuthenticatedProcessingFilter.class::isInstance)
                            .anyMatch(YibanAuthorizationProcessingFilter.class::isInstance);
                    MockHttpServletRequest protectedRequest = new MockHttpServletRequest("GET", "/login");
                    protectedRequest.setServletPath("/login");
                    assertThat(filters.getFilterChains())
                            .anyMatch(chain -> chain.matches(protectedRequest) && !chain.getFilters().isEmpty()
                                    && chain.getFilters().stream().noneMatch(YibanAuthorizationProcessingFilter.class::isInstance));

                    MockHttpServletRequest start = request();
                    MockHttpServletResponse redirect = new MockHttpServletResponse();
                    filters.doFilter(start, redirect, new MockFilterChain());
                    String state = parameters(URI.create(redirect.getRedirectedUrl())).get("state");
                    assertThat(state).isNotBlank();

                    MockHttpSession session = (MockHttpSession) start.getSession();
                    MockHttpServletResponse response = new MockHttpServletResponse();
                    filters.doFilter(callback(session, state), response, new MockFilterChain());
                    assertThat(exchanges.get()).isEqualTo(1);
                    assertThat(response.getStatus()).isLessThan(400);
                    assertThat(session.getAttribute("SPRING_SECURITY_CONTEXT")).isNotNull();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Application {
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream response = exchange.getResponseBody()) {
            response.write(bytes);
        } finally {
            exchange.close();
        }
    }

    private static Map<String, String> parameters(URI uri) {
        return URLEncodedUtils.parse(uri, StandardCharsets.UTF_8).stream()
                .collect(Collectors.toMap(pair -> pair.getName(), pair -> pair.getValue()));
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", LOGIN);
        request.setServletPath(LOGIN);
        return request;
    }

    private static MockHttpServletRequest callback(MockHttpSession session, String state) {
        MockHttpServletRequest request = request();
        request.setSession(session);
        request.addParameter("code", CODE);
        request.addParameter("state", state);
        return request;
    }
}
