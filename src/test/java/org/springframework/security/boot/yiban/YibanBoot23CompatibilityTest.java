package org.springframework.security.boot.yiban;

import cn.yiban.open.Authorize;
import cn.yiban.util.HTTPSimple;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.message.BasicNameValuePair;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.boot.biz.userdetails.UserDetailsServiceAdapter;
import org.springframework.security.boot.yiban.authentication.YibanAuthenticationProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class YibanBoot23CompatibilityTest {

    @Test
    void startsServletSecurityWithThePublishedSecurityBizDependency() {
        assertThat(SpringBootVersion.getVersion()).isEqualTo("2.3.12.RELEASE");
        assertThat(System.getProperty("java.specification.version")).isEqualTo("1.8");
        new WebApplicationContextRunner()
            .withUserConfiguration(ServletApplication.class)
            .withBean(UserDetailsServiceAdapter.class, () -> mock(UserDetailsServiceAdapter.class))
            .withPropertyValues("spring.security.yiban.enabled=true",
                "spring.security.yiban.authc.app-key=local-fixture",
                "spring.security.yiban.authc.app-secret=fixture-secret",
                "spring.security.yiban.authc.callback=https://example.test/callback")
            .run(context -> {
                assertThat(context).hasSingleBean(Authorize.class)
                    .hasSingleBean(YibanAuthenticationProvider.class)
                    .hasBean("springSecurityFilterChain");
                assertThat(context.getBean(Authorize.class)
                    .forwardurl("https://example.test/callback", "fixture-state", Authorize.DISPLAY_TAG_T.WEB))
                    .startsWith("https://openapi.yiban.cn/")
                    .contains("local-fixture", "fixture-state");
            });
    }

    @Test
    void transportAcceptsFormResponsesWithoutFollowingRedirects() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger redirected = new AtomicInteger();
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().set("Location", "/destination");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/destination", exchange -> {
            redirected.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.createContext("/form", exchange -> {
            byte[] body = "{\"access_token\":\"fixture\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            String endpoint = "http://127.0.0.1:" + server.getAddress().getPort();
            assertThat(HTTPSimple.POST(endpoint + "/form", Collections.singletonList(
                new BasicNameValuePair("code", "fixture")))).contains("access_token");
            assertThat(HTTPSimple.GET(endpoint + "/redirect")).isEmpty();
            assertThat(redirected.get()).isZero();
        } finally {
            server.stop(0);
        }
    }
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class ServletApplication {
    }
}
