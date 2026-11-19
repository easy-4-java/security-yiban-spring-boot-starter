package org.springframework.security.boot.yiban.authentication;

import java.io.IOException;
import java.util.Objects;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.boot.yiban.client.YibanAuthorizationState;
import org.springframework.security.boot.yiban.client.YibanOAuthClient;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** 仅在授权入口发起易班授权。回调由登录处理过滤器处理。 */
public class YibanPreAuthenticatedProcessingFilter extends AbstractPreAuthenticatedProcessingFilter {

    private final YibanOAuthClient authorize;
    private final RequestMatcher initiationRequest;
    private final String codeParameter;

    public YibanPreAuthenticatedProcessingFilter(YibanOAuthClient authorize, String loginUrl, String codeParameter) {
        this.authorize = authorize;
        this.initiationRequest = new AntPathRequestMatcher(loginUrl, "GET");
        this.codeParameter = codeParameter;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        if (!initiationRequest.matches(http) || Objects.nonNull(http.getParameter(codeParameter))
                || Objects.nonNull(http.getParameter("state")) || Objects.nonNull(http.getParameter("error"))) {
            chain.doFilter(request, response);
            return;
        }
        String state = YibanAuthorizationState.issue(http);
        ((HttpServletResponse) response).sendRedirect(authorize.authorizationUri(state).toASCIIString());
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        return "N/A";
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        return "N/A";
    }
}
